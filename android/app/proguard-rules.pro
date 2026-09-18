# Al-Mahalla Proguard Rules
-keep class com.ps1.netplay.** { *; }
-keepclassmembers class com.ps1.netplay.** { *; }

# JNI & Native
-keepclasseswithmembernames class * {
    native <methods>;
}

# LiveKit & WebRTC
-keep class io.livekit.android.** { *; }
-keep interface io.livekit.android.** { *; }
-keep class livekit.org.webrtc.** { *; }
-keepclassmembers class livekit.org.webrtc.** { *; }

# OkHttp & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn kotlinx.coroutines.**
