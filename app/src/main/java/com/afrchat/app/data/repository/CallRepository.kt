package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.CallSession
import com.afrchat.app.data.model.IceCandidateData
import com.afrchat.app.data.remote.CallInsert
import com.afrchat.app.data.remote.CallRow
import com.afrchat.app.data.remote.CandidateInsert
import com.afrchat.app.data.remote.CandidateRow
import com.afrchat.app.data.remote.RealtimeHub
import com.afrchat.app.data.remote.TableSpec
import com.afrchat.app.data.remote.jsonOf
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signalisation WebRTC via Supabase (tables "calls" et "call_candidates" + Realtime) : pas de serveur
 * de signalisation séparé. Le média transite en pair-à-pair une fois la connexion établie
 * (voir ui/call/webrtc/WebRtcClient.kt).
 *
 * IMPORTANT : pour des appels fiables entre deux réseaux différents (4G / Wi-Fi derrière NAT), un
 * serveur TURN est nécessaire — secrets AFRCHAT_TURN_URL / _USERNAME / _CREDENTIAL (voir README).
 */
@Singleton
class CallRepository @Inject constructor(
    private val client: SupabaseClient,
    private val hub: RealtimeHub
) {
    private val db get() = client.postgrest

    /** [callId] est l'identifiant (UUID) déjà utilisé par les deux écrans d'appel. */
    suspend fun createCall(callId: String, callerUid: String, calleeUid: String, isVideo: Boolean, offerSdp: String): AfrResult<String> = try {
        db.from("calls").insert(CallInsert(id = callId, callerId = callerUid, calleeId = calleeUid, isVideo = isVideo, offerSdp = offerSdp))
        AfrResult.Success(callId)
    } catch (e: Exception) {
        AfrResult.Error("Impossible de démarrer l'appel.", e)
    }

    fun observeCall(callId: String): Flow<CallSession?> =
        hub.observe("call-$callId", TableSpec("calls", "id", callId)) {
            db.from("calls").select { filter { eq("id", callId) } }.decodeList<CallRow>().firstOrNull()?.toModel()
        }

    /** Appel entrant récent (statut "ringing", moins d'une minute) pour afficher l'écran d'appel entrant. */
    fun observeIncomingCalls(calleeUid: String): Flow<CallSession?> =
        hub.observe("incoming-calls-$calleeUid", TableSpec("calls", "callee_id", calleeUid)) {
            db.from("calls").select {
                filter {
                    eq("callee_id", calleeUid)
                    eq("status", "ringing")
                    gt("created_at", System.currentTimeMillis() - 60_000L)
                }
                order("created_at", Order.DESCENDING)
                limit(1)
            }.decodeList<CallRow>().firstOrNull()?.toModel()
        }

    /** Historique des appels de l'utilisateur (la RLS ne renvoie que les appels où il est impliqué). */
    suspend fun loadCallLog(limit: Long = 60): List<CallSession> =
        db.from("calls").select {
            order("created_at", Order.DESCENDING)
            limit(limit)
        }.decodeList<CallRow>().map { it.toModel() }

    suspend fun acceptCall(callId: String, answerSdp: String) {
        db.from("calls").update(jsonOf("status" to "accepted", "answer_sdp" to answerSdp)) { filter { eq("id", callId) } }
    }

    suspend fun declineCall(callId: String) {
        db.from("calls").update(jsonOf("status" to "declined", "ended_at" to System.currentTimeMillis())) { filter { eq("id", callId) } }
    }

    suspend fun endCall(callId: String) {
        db.from("calls").update(jsonOf("status" to "ended", "ended_at" to System.currentTimeMillis())) { filter { eq("id", callId) } }
    }

    suspend fun sendIceCandidate(callId: String, from: String, candidate: IceCandidateData) {
        try {
            db.from("call_candidates").insert(
                CandidateInsert(callId, from, candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate)
            )
        } catch (_: Exception) { }
    }

    /** Candidats ICE de l'autre partie : ceux déjà présents, puis les nouveaux (dédoublonnés par id). */
    fun observeRemoteIceCandidates(callId: String, from: String): Flow<IceCandidateData> = flow {
        val seen = HashSet<Long>()
        hub.observe("ice-$callId-$from", TableSpec("call_candidates", "call_id", callId)) {
            db.from("call_candidates").select {
                filter { eq("call_id", callId); eq("from_role", from) }
                order("id", Order.ASCENDING)
            }.decodeList<CandidateRow>()
        }.collect { rows ->
            rows.filter { seen.add(it.id) }.forEach { emit(it.toModel()) }
        }
    }
}
