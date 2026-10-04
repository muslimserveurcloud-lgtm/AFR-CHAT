plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

// Lit une valeur de config : variable d'environnement (CI), sinon propriété Gradle
// (~/.gradle/gradle.properties ou -P), sinon valeur par défaut.
fun cfg(name: String, default: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: (project.findProperty(name) as String?)?.takeIf { it.isNotBlank() }
        ?: default

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
        // En CI, le numéro de run GitHub sert de versionCode : chaque APK est ainsi
        // installable par-dessus le précédent (Android refuse un versionCode identique/inférieur).
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0.0"

        // Supabase : URL du projet + clé "anon" (publique par conception ; la sécurité repose sur la RLS).
        // Fournies par les secrets GitHub SUPABASE_URL / SUPABASE_ANON_KEY (ou gradle.properties en local).
        buildConfigField("String", "SUPABASE_URL", "\"${cfg("SUPABASE_URL", "https://example.supabase.co")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${cfg("SUPABASE_ANON_KEY", "PLACEHOLDER_ANON_KEY")}\"")

        // TURN (appels WebRTC) : injecté depuis les secrets GitHub / variables d'environnement.
        // Valeurs par défaut = "A_CONFIGURER" (l'app retombe alors sur STUN seul).
        buildConfigField("String", "TURN_URL", "\"${cfg("AFRCHAT_TURN_URL", "turn:TON_SERVEUR_TURN:3478")}\"")
        buildConfigField("String", "TURN_USERNAME", "\"${cfg("AFRCHAT_TURN_USERNAME", "A_CONFIGURER")}\"")
        buildConfigField("String", "TURN_CREDENTIAL", "\"${cfg("AFRCHAT_TURN_CREDENTIAL", "A_CONFIGURER")}\"")

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
            isDebuggable = true
            buildConfigField("boolean", "IS_DEBUG", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "IS_DEBUG", "false")
            // Signature uniquement si un keystore est fourni (sinon l'APK release reste non signé)
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,INDEX.LIST,DEPENDENCIES,io.netty.versions.properties}"
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
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Supabase (Auth + Postgres/PostgREST + Realtime) — le stockage de fichiers passe par OkHttp (REST)
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

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
