# Al-Mahalla Optimized Proguard & R8 Rules
# Keep application activities, services, receivers, and application class
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep Serializable and Parcelable models
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep data models used in JSON serialization
-keep class com.ps1.netplay.model.** { *; }
-keepclassmembers class com.ps1.netplay.model.** { *; }

# JNI & Native
-keepclasseswithmembernames class * {
    native <methods>;
}

# Zego Express Video & Audio Calling Engine
-keep class im.zego.zegoexpress.** { *; }
-dontwarn im.zego.zegoexpress.**

# OkHttp & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn kotlinx.coroutines.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

