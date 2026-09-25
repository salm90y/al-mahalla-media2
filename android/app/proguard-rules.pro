# Al-Mahalla Proguard Rules
-keep class com.ps1.netplay.** { *; }
-keepclassmembers class com.ps1.netplay.** { *; }

# JNI & Native
-keepclasseswithmembernames class * {
    native <methods>;
}

# Zego Express Video & Audio Calling Engine
-keep class im.zego.zegoexpress.** { *; }

# OkHttp & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn kotlinx.coroutines.**
