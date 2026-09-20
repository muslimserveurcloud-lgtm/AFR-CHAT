package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.CallSession
import com.afrchat.app.data.model.IceCandidateData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Couche de signalisation WebRTC basée sur Firestore : AFR CHAT n'a pas besoin d'un serveur
 * de signalisation séparé, Firestore joue ce rôle (échange de l'offre SDP, de la réponse SDP
 * et des candidats ICE en temps réel). Le média (audio/vidéo) transite en pair-à-pair via
 * WebRTC une fois la connexion établie — voir ui/call/webrtc/WebRtcClient.kt.
 *
 * IMPORTANT — infrastructure à fournir par toi (voir README) :
 * Pour que les appels fonctionnent de façon fiable entre deux réseaux différents (4G / Wi-Fi
 * derrière NAT/pare-feu), il faut un serveur TURN. Renseigne ses identifiants dans
 * utils/Constants.kt (TURN_URL, TURN_USERNAME, TURN_CREDENTIAL). Sans TURN, les appels
 * fonctionneront uniquement entre pairs sur des réseaux qui autorisent une connexion directe.
 */
@Singleton
class CallRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun callsRef() = firestore.collection("calls")

    suspend fun createCall(callerUid: String, calleeUid: String, isVideo: Boolean, offerSdp: String): AfrResult<String> = try {
        val doc = callsRef().document()
        val call = CallSession(
            id = doc.id, callerUid = callerUid, calleeUid = calleeUid,
            isVideo = isVideo, status = "ringing", offerSdp = offerSdp
        )
        doc.set(call).await()
        AfrResult.Success(doc.id)
    } catch (e: Exception) {
        AfrResult.Error("Impossible de démarrer l'appel.", e)
    }

    fun observeCall(callId: String): Flow<CallSession?> = callbackFlow {
        val reg = callsRef().document(callId).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(CallSession::class.java))
        }
        awaitClose { reg.remove() }
    }

    /** Appels entrants pour un utilisateur (statut "ringing"), utilisé pour afficher l'écran d'appel entrant. */
    fun observeIncomingCalls(calleeUid: String): Flow<CallSession?> = callbackFlow {
        val reg = callsRef()
            .whereEqualTo("calleeUid", calleeUid)
            .whereEqualTo("status", "ringing")
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(CallSession::class.java)?.firstOrNull())
            }
        awaitClose { reg.remove() }
    }

    suspend fun acceptCall(callId: String, answerSdp: String) {
        callsRef().document(callId).update(mapOf("status" to "accepted", "answerSdp" to answerSdp)).await()
    }

    suspend fun declineCall(callId: String) {
        callsRef().document(callId).update(mapOf("status" to "declined", "endedAt" to System.currentTimeMillis())).await()
    }

    suspend fun endCall(callId: String) {
        callsRef().document(callId).update(mapOf("status" to "ended", "endedAt" to System.currentTimeMillis())).await()
    }

    suspend fun sendIceCandidate(callId: String, from: String, candidate: IceCandidateData) {
        callsRef().document(callId).collection("${from}Candidates").add(candidate).await()
    }

    fun observeRemoteIceCandidates(callId: String, from: String): Flow<IceCandidateData> = callbackFlow {
        val reg = callsRef().document(callId).collection("${from}Candidates")
            .addSnapshotListener { snap, _ ->
                snap?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        change.document.toObject(IceCandidateData::class.java).let { trySend(it) }
                    }
                }
            }
        awaitClose { reg.remove() }
    }
}
