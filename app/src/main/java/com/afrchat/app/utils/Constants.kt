package com.afrchat.app.utils

import com.afrchat.app.BuildConfig

/**
 * Constantes globales AFR CHAT.
 * ⚠️ Les identifiants TURN ci-dessous sont des exemples et doivent être remplacés par les
 * tiens (voir README section "Configurer les appels audio/vidéo"). Ne mets jamais de secrets
 * réels dans le contrôle de version en clair pour une app publiée : passe par un fichier local
 * non versionné ou une Cloud Function qui génère des identifiants TURN de courte durée.
 */
object Constants {
    // Serveurs STUN publics (gratuits, suffisent pour découvrir l'IP publique)
    val STUN_SERVERS = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302"
    )

    // Serveur TURN — injecté au build via les secrets GitHub AFRCHAT_TURN_URL / _USERNAME / _CREDENTIAL
    // (voir app/build.gradle.kts). Sans secrets : valeurs "A_CONFIGURER" => STUN seul.
    val TURN_URL: String = BuildConfig.TURN_URL
    val TURN_USERNAME: String = BuildConfig.TURN_USERNAME
    val TURN_CREDENTIAL: String = BuildConfig.TURN_CREDENTIAL

    const val MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024
    const val MAX_VIDEO_SIZE_BYTES = 100L * 1024 * 1024
    const val MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024
    const val STORY_DURATION_MS = 24 * 60 * 60 * 1000L
}
