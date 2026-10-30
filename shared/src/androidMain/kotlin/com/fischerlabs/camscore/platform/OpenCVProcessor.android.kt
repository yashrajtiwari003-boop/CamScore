package com.fischerlabs.camscore.platform

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.video.Video
import kotlin.math.sqrt

/**
 * Android OpenCVProcessor implementation using OpenCV Android SDK.
 *
 * Implements all three scoring phases:
 *   Phase 1: Autofocus — Laplacian variance (σ²)
 *   Phase 2: OIS — Lucas-Kanade Pyramid Optical Flow
 *   Phase 3: Lens Switch — CIE L*a*b* ΔE* color difference
 *   Pre-check: Texture/edge density via Canny
 */
class AndroidOpenCVProcessor : OpenCVProcessor {

    // Retained reference frame and feature points for optical flow tracking
    private var previousGrayFrame: Mat? = null
    private var previousPoints: MatOfPoint2f? = null
    private var isInitialized = false

    override fun initialize() {
        // OpenCV is initialized via OpenCVLoader.initDebug() in MainActivity.
        // This method exists for lifecycle management.
        isInitialized = true
    }

    override fun release() {
        releaseFrame()
        isInitialized = false
    }

    override fun calculateLaplacianVariance(
        frameData: ByteArray,
        width: Int,
        height: Int
    ): Double {
        // Create a Mat from NV21/YUV420 data
        val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
        yuvMat.put(0, 0, frameData)

        // Convert YUV to grayscale (just take Y channel = first height rows)
        val grayMat = Mat(height, width, CvType.CV_8UC1)
        yuvMat.submat(0, height, 0, width).copyTo(grayMat)

        // Apply Laplacian operator
        val laplacianMat = Mat()
        Imgproc.Laplacian(grayMat, laplacianMat, CvType.CV_64F)

        // Compute mean and standard deviation
        val mean = MatOfDouble()
        val stddev = MatOfDouble()
        Core.meanStdDev(laplacianMat, mean, stddev)

        // Variance = stddev²
        val stddevValue = stddev.toArray()[0]
        val variance = stddevValue * stddevValue

        // Clean up
        yuvMat.release()
        grayMat.release()
        laplacianMat.release()
        mean.release()
        stddev.release()

        return variance
    }

    override fun initOpticalFlowTracking(
        frameData: ByteArray,
        width: Int,
        height: Int,
        numCorners: Int
    ) {
        // Release previous tracking state
        previousGrayFrame?.release()
        previousPoints?.release()

        // Create grayscale Mat from YUV data
        val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
        yuvMat.put(0, 0, frameData)

        val grayMat = Mat(height, width, CvType.CV_8UC1)
        yuvMat.submat(0, height, 0, width).copyTo(grayMat)
        yuvMat.release()

        // Detect good features to track (Shi-Tomasi corners)
        val corners = MatOfPoint()
        Imgproc.goodFeaturesToTrack(
            grayMat,
            corners,
            numCorners,
            0.01,  // quality level
            10.0,  // minimum distance between corners
            Mat(), // mask (none)
            3,     // block size
            false, // useHarrisDetector
            0.04   // Harris k parameter
        )

        // Convert MatOfPoint to MatOfPoint2f for optical flow
        val cornersArray = corners.toArray()
        val points2f = MatOfPoint2f()
        if (cornersArray.isNotEmpty()) {
            val pointsArray = cornersArray.map { org.opencv.core.Point(it.x, it.y) }.toTypedArray()
            points2f.fromArray(*pointsArray)
        }

        previousGrayFrame = grayMat
        previousPoints = points2f
        corners.release()
    }

    override fun calculateOpticalFlow(
        newFrameData: ByteArray,
        width: Int,
        height: Int
    ): Double {
        val prevGray = previousGrayFrame ?: return 0.0
        val prevPts = previousPoints ?: return 0.0

        if (prevPts.empty()) return 0.0

        // Create new grayscale frame
        val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
        yuvMat.put(0, 0, newFrameData)

        val newGray = Mat(height, width, CvType.CV_8UC1)
        yuvMat.submat(0, height, 0, width).copyTo(newGray)
        yuvMat.release()

        // Calculate Lucas-Kanade Pyramid optical flow
        val nextPts = MatOfPoint2f()
        val status = MatOfByte()
        val err = MatOfFloat()

        Video.calcOpticalFlowPyrLK(
            prevGray,
            newGray,
            prevPts,
            nextPts,
            status,
            err,
            Size(21.0, 21.0), // window size
            3,                 // max pyramid level
            org.opencv.core.TermCriteria(
                org.opencv.core.TermCriteria.COUNT + org.opencv.core.TermCriteria.EPS,
                30,   // max iterations
                0.01  // epsilon
            )
        )

        // Calculate mean displacement of successfully tracked points
        val statusArray = status.toArray()
        val prevPtsArray = prevPts.toArray()
        val nextPtsArray = nextPts.toArray()

        var totalDisplacement = 0.0
        var trackedCount = 0

        for (i in statusArray.indices) {
            if (statusArray[i].toInt() == 1 && i < prevPtsArray.size && i < nextPtsArray.size) {
                val dx = nextPtsArray[i].x - prevPtsArray[i].x
                val dy = nextPtsArray[i].y - prevPtsArray[i].y
                val displacement = sqrt(dx * dx + dy * dy)
                totalDisplacement += displacement
                trackedCount++
            }
        }

        val meanDisplacement = if (trackedCount > 0) totalDisplacement / trackedCount else 0.0

        // Update reference for next call: use the new frame and successfully tracked points
        prevGray.release()
        previousGrayFrame = newGray

        // Keep successfully tracked points as new reference
        if (trackedCount > 0) {
            val goodPoints = mutableListOf<org.opencv.core.Point>()
            for (i in statusArray.indices) {
                if (statusArray[i].toInt() == 1 && i < nextPtsArray.size) {
                    goodPoints.add(nextPtsArray[i])
                }
            }
            previousPoints?.release()
            val newPrevPts = MatOfPoint2f()
            newPrevPts.fromArray(*goodPoints.toTypedArray())
            previousPoints = newPrevPts
        }

        nextPts.release()
        status.release()
        err.release()

        return meanDisplacement
    }

    override fun extractCenterPatchLab(
        frameData: ByteArray,
        width: Int,
        height: Int,
        patchSize: Int
    ): LabColor {
        // Create Mat from YUV data
        val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
        yuvMat.put(0, 0, frameData)

        // Convert YUV (NV21) to BGR
        val bgrMat = Mat()
        Imgproc.cvtColor(yuvMat, bgrMat, Imgproc.COLOR_YUV2BGR_NV21)
        yuvMat.release()

        // Extract center patch
        val centerX = width / 2
        val centerY = height / 2
        val halfPatch = patchSize / 2

        val roiX = (centerX - halfPatch).coerceAtLeast(0)
        val roiY = (centerY - halfPatch).coerceAtLeast(0)
        val roiW = patchSize.coerceAtMost(width - roiX)
        val roiH = patchSize.coerceAtMost(height - roiY)

        val roi = Rect(roiX, roiY, roiW, roiH)
        val patchBgr = Mat(bgrMat, roi)

        // Convert BGR to CIE L*a*b*
        val patchLab = Mat()
        Imgproc.cvtColor(patchBgr, patchLab, Imgproc.COLOR_BGR2Lab)

        // Calculate mean L*a*b* values across the patch
        val meanScalar: Scalar = Core.mean(patchLab)

        // OpenCV L*a*b* encoding: L is [0, 255] mapped from [0, 100],
        // a and b are [0, 255] mapped from [-128, 127]
        val l = meanScalar.`val`[0] * 100.0 / 255.0
        val a = meanScalar.`val`[1] - 128.0
        val b = meanScalar.`val`[2] - 128.0

        // Cleanup
        bgrMat.release()
        patchBgr.release()
        patchLab.release()

        return LabColor(l = l, a = a, b = b)
    }

    override fun calculateDeltaE(lab1: LabColor, lab2: LabColor): Double {
        // CIE76 ΔE* formula
        val dL = lab1.l - lab2.l
        val da = lab1.a - lab2.a
        val db = lab1.b - lab2.b
        return sqrt(dL * dL + da * da + db * db)
    }

    override fun calculateTextureScore(
        frameData: ByteArray,
        width: Int,
        height: Int
    ): Float {
        // Create grayscale Mat from YUV Y-channel
        val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
        yuvMat.put(0, 0, frameData)

        val grayMat = Mat(height, width, CvType.CV_8UC1)
        yuvMat.submat(0, height, 0, width).copyTo(grayMat)
        yuvMat.release()

        // Apply Gaussian blur to reduce noise before edge detection
        val blurredMat = Mat()
        Imgproc.GaussianBlur(grayMat, blurredMat, Size(3.0, 3.0), 0.0)
        grayMat.release()

        // Apply Canny edge detection
        val edgeMat = Mat()
        Imgproc.Canny(blurredMat, edgeMat, 50.0, 150.0)
        blurredMat.release()

        // Count edge pixels as a fraction of total pixels
        val edgePixels = Core.countNonZero(edgeMat).toFloat()
        val totalPixels = (width * height).toFloat()
        val edgeDensity = edgePixels / totalPixels

        edgeMat.release()

        return edgeDensity
    }

    override fun releaseFrame() {
        previousGrayFrame?.release()
        previousGrayFrame = null
        previousPoints?.release()
        previousPoints = null
    }
}

/**
 * Helper typealias for OpenCV's MatOfDouble which is used
 * by Core.meanStdDev but not always imported.
 */
private typealias MatOfDouble = org.opencv.core.MatOfDouble

actual fun createOpenCVProcessor(): OpenCVProcessor {
    return AndroidOpenCVProcessor()
}
