plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.afrchat.app"
    compileSdk = 35

    // Nom de fichier de sortie : app-debug.apk / app-release.apk deviennent
    // AFR-CHAT-debug.apk / AFR-CHAT-release.apk (renomme-le simplement en AFR-CHAT.apk après export).
    base.archivesName.set("AFR-CHAT")

    defaultConfig {
        applicationId = "com.afrchat.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            // Renseigne ces variables via ~/.gradle/gradle.properties ou des variables d'environnement.
            // AFRCHAT_STORE_FILE, AFRCHAT_STORE_PASSWORD, AFRCHAT_KEY_ALIAS, AFRCHAT_KEY_PASSWORD
            val storeFilePath = System.getenv("AFRCHAT_STORE_FILE") ?: (project.findProperty("AFRCHAT_STORE_FILE") as String?)
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = System.getenv("AFRCHAT_STORE_PASSWORD") ?: (project.findProperty("AFRCHAT_STORE_PASSWORD") as String?)
                keyAlias = System.getenv("AFRCHAT_KEY_ALIAS") ?: (project.findProperty("AFRCHAT_KEY_ALIAS") as String?)
                keyPassword = System.getenv("AFRCHAT_KEY_PASSWORD") ?: (project.findProperty("AFRCHAT_KEY_PASSWORD") as String?)
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            buildConfigField("boolean", "IS_DEBUG", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "IS_DEBUG", "false")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.2")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Hilt (injection de dépendances)
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Images
    implementation("io.coil-kt:coil-compose:2.7.0")

    // WorkManager (mode hors-ligne / synchronisation différée)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // WebRTC (appels audio/vidéo) — build maintenu par webrtc-sdk (miroir Google WebRTC pour Android)
    implementation("io.github.webrtc-sdk:android:125.6422.07")

    // Permissions & médias
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // DataStore (préférences: thème, session)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.02"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
