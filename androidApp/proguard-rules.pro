# CamScore ProGuard / R8 optimization rules

# OpenCV
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# Camera2 and CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# SQLDelight
-keep class com.fischerlabs.camscore.database.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Firebase GitLive
-keep class dev.gitlive.firebase.** { *; }
