package com.afrchat.app.data.model

/**
 * Session d'appel WebRTC, utilisée comme canal de signalisation via Firestore.
 * Collection "calls/{callId}". Sous-collections "callerCandidates" et "calleeCandidates".
 */
data class CallSession(
    val id: String = "",
    val callerUid: String = "",
    val calleeUid: String = "",
    val isVideo: Boolean = false,
    val status: String = "ringing", // ringing | accepted | declined | ended | missed
    val offerSdp: String? = null,
    val answerSdp: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null
)

data class IceCandidateData(
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val candidate: String = ""
)
