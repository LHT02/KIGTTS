plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "com.kgtts.kgtts_app"
    compileSdk = 36
    ndkVersion = "27.0.12077973"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        applicationId = "com.kgtts.kgtts_app"
        minSdk = 26
        targetSdk = 36
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                abiFilters("arm64-v8a")
            }
        }
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.31.6"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources {
            excludes += listOf("/META-INF/{AL2.0,LGPL2.1}")
        }
        jniLibs {
            pickFirsts += listOf("lib/**/libonnxruntime.so")
        }
    }

    aaptOptions {
        noCompress += listOf("zip")
    }
}

flutter {
    source = "../.."
}

dependencies {
    implementation(files("libs/sherpa-onnx-1.12.20.aar"))
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
