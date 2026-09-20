package com.afrchat.app.utils

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

    // Serveur TURN — À CONFIGURER (coturn auto-hébergé, ou service comme Twilio NTS / Xirsys / Metered).
    const val TURN_URL = "turn:TON_SERVEUR_TURN:3478"
    const val TURN_USERNAME = "A_CONFIGURER"
    const val TURN_CREDENTIAL = "A_CONFIGURER"

    const val MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024
    const val MAX_VIDEO_SIZE_BYTES = 100L * 1024 * 1024
    const val MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024
    const val STORY_DURATION_MS = 24 * 60 * 60 * 1000L
}
