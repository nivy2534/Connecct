plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.connecct"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.connecct"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            pickFirsts += listOf(
                "org/bouncycastle/x509/CertPathReviewerMessages_de.properties",
                "org/bouncycastle/x509/CertPathReviewerMessages.properties"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation (libs.sshj)
    implementation("org.bouncycastle:bcpkix-jdk18on:1.82")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// 🛠 Paksa semua library Bouncy Castle versi 1.82
configurations.all {
    resolutionStrategy {
        force("org.bouncycastle:bcprov-jdk15to18:1.82")
        force("org.bouncycastle:bcpkix-jdk15to18:1.82")
        // Hapus semua versi lama
        eachDependency {
            if (requested.group == "org.bouncycastle" && requested.name.contains("bcprov")) {
                useVersion("1.82")
            }
            if (requested.group == "org.bouncycastle" && requested.name.contains("bcpkix")) {
                useVersion("1.82")
            }
        }
    }
}

