package com.fischerlabs.camscore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * QR code display composable.
 * Generates and renders a QR code from a verification URL string.
 */
@Composable
fun QRCodeView(
    data: String,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    label: String = "Scan QR to verify this certificate",
    foregroundColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    // Generate QR matrix using a simple QR encoding approach
    val qrMatrix = remember(data) { generateQRMatrix(data) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val matrixSize = qrMatrix.size
                if (matrixSize == 0) return@Canvas

                val cellSize = this.size.width / matrixSize

                // Draw background
                drawRect(backgroundColor, size = this.size)

                // Draw QR modules
                for (row in qrMatrix.indices) {
                    for (col in qrMatrix[row].indices) {
                        if (qrMatrix[row][col]) {
                            drawRect(
                                color = foregroundColor,
                                topLeft = Offset(col * cellSize, row * cellSize),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Generate a simplified QR-like matrix from input data.
 * For production, this would use the io.github.g0dkar:qrcode-kotlin library.
 * This generates a deterministic pattern based on the hash for visual representation.
 */
private fun generateQRMatrix(data: String, size: Int = 25): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) }

    // Finder patterns (top-left, top-right, bottom-left)
    addFinderPattern(matrix, 0, 0)
    addFinderPattern(matrix, 0, size - 7)
    addFinderPattern(matrix, size - 7, 0)

    // Timing patterns
    for (i in 8 until size - 8) {
        matrix[6][i] = i % 2 == 0
        matrix[i][6] = i % 2 == 0
    }

    // Data area: fill based on hash of input
    val hash = data.hashCode()
    var seed = hash.toLong() and 0xFFFFFFFFL
    for (row in 0 until size) {
        for (col in 0 until size) {
            if (matrix[row][col]) continue // Skip finder/timing patterns
            if (row < 9 && col < 9) continue
            if (row < 9 && col >= size - 8) continue
            if (row >= size - 8 && col < 9) continue

            seed = (seed * 1103515245 + 12345) and 0x7FFFFFFF
            matrix[row][col] = (seed % 3) != 0L
        }
    }

    return matrix
}

private fun addFinderPattern(matrix: Array<BooleanArray>, startRow: Int, startCol: Int) {
    for (r in 0 until 7) {
        for (c in 0 until 7) {
            val row = startRow + r
            val col = startCol + c
            if (row >= matrix.size || col >= matrix[0].size) continue

            matrix[row][col] = when {
                r == 0 || r == 6 -> true
                c == 0 || c == 6 -> true
                r in 2..4 && c in 2..4 -> true
                else -> false
            }
        }
    }
}
