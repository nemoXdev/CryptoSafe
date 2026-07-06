import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

android {
    namespace = "com.cryptosafe.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cryptosafe.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "1.2.2"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                keyAlias = "release"

                val storePass = System.getenv("CRYPTOSAFE_STORE_PASSWORD")
                val keyPass = System.getenv("CRYPTOSAFE_KEY_PASSWORD")

                if (storePass != null && keyPass != null) {
                    storePassword = storePass
                    keyPassword = keyPass
                } else {
                    val props = Properties()
                    val propsFile = rootProject.file("local.properties")
                    if (propsFile.exists()) {
                        props.load(propsFile.inputStream())
                    }
                    storePassword = props.getProperty("keystore.password")
                    keyPassword = props.getProperty("key.password")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = if (file("release.keystore").exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    dependenciesInfo {
        includeInApk = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")

    implementation(platform("androidx.compose:compose-bom:2025.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.lambdapioneer.argon2kt:argon2kt:1.6.0")

    configurations.all {
        exclude(group = "io.opencensus")
    }
}
