import java.net.URL
import java.util.zip.ZipInputStream
import java.io.FileOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ps1.netplay"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ps1.netplay"
        minSdk = 26
        targetSdk = 34
        versionCode = 11
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17 -Oz -fvisibility=hidden -fdata-sections -ffunction-sections -fexceptions -frtti"
                arguments += "-DANDROID_STL=c++_shared"
                abiFilters += listOf("arm64-v8a")
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    ndkVersion = "26.1.10909125"

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material.ExperimentalMaterialApi"
        )
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/*.md",
                "/META-INF/NOTICE*",
                "/META-INF/LICENSE*"
            )
        }
        jniLibs {
            pickFirsts += listOf("**/libc++_shared.so")
            useLegacyPackaging = false
        }
    }

    buildFeatures { 
        viewBinding = true 
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8" // Make sure this matches the Kotlin version
    }

    bundle {
        abi {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        language {
            enableSplit = true
        }
    }
}

tasks.register("downloadPs1Core") {
    val targetDir = file("src/main/assets/cores/arm64-v8a")
    val targetFile = file("src/main/assets/cores/arm64-v8a/pcsx_rearmed_libretro_android.so")
    outputs.file(targetFile)
    doLast {
        if (!targetFile.exists()) {
            targetDir.mkdirs()
            println("Downloading pcsx_rearmed_libretro_android.so...")
            val url = URL("https://buildbot.libretro.com/nightly/android/latest/arm64-v8a/pcsx_rearmed_libretro_android.so.zip")
            val connection = url.openConnection()
            ZipInputStream(connection.getInputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name.endsWith(".so")) {
                        FileOutputStream(targetFile).use { out -> zis.copyTo(out) }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        }
    }
}
// Cores and BIOS are downloaded on-demand by CoreManager inside the app


dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.livekit:livekit-android:2.18.2")
    
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("io.coil-kt:coil-compose:2.6.0")
}
