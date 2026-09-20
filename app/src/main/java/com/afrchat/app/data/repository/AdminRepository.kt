package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.Report
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Toutes les actions de modération sensibles (bannir, supprimer un contenu, changer un rôle
 * admin) passent par des Cloud Functions "callable" (firebase/functions/index.js) qui
 * vérifient côté serveur le custom claim "admin" du jeton d'authentification. Le client ne
 * peut donc jamais réaliser ces actions directement, même en modifiant l'app.
 */
@Singleton
class AdminRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {
    suspend fun submitReport(report: Report): AfrResult<Unit> = try {
        val doc = firestore.collection("reports").document()
        firestore.collection("reports").document(doc.id).set(report.copy(id = doc.id)).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Le signalement a échoué.", e)
    }

    suspend fun listOpenReports(): AfrResult<List<Report>> = try {
        val snap = firestore.collection("reports").whereEqualTo("status", "open").get().await()
        AfrResult.Success(snap.toObjects(Report::class.java))
    } catch (e: Exception) {
        AfrResult.Error("Impossible de charger les signalements.", e)
    }

    suspend fun banUser(targetUid: String, reason: String): AfrResult<Unit> = try {
        functions.getHttpsCallable("banUser").call(mapOf("targetUid" to targetUid, "reason" to reason)).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Action impossible : ${e.message}", e)
    }

    suspend fun unbanUser(targetUid: String): AfrResult<Unit> = try {
        functions.getHttpsCallable("unbanUser").call(mapOf("targetUid" to targetUid)).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Action impossible : ${e.message}", e)
    }

    suspend fun resolveReport(reportId: String, status: String): AfrResult<Unit> = try {
        functions.getHttpsCallable("resolveReport").call(mapOf("reportId" to reportId, "status" to status)).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Action impossible : ${e.message}", e)
    }

    suspend fun grantAdmin(targetUid: String, grant: Boolean): AfrResult<Unit> = try {
        functions.getHttpsCallable("setAdminRole").call(mapOf("targetUid" to targetUid, "grant" to grant)).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Action impossible : ${e.message}", e)
    }

    suspend fun getStats(): AfrResult<Map<String, Any>> = try {
        val result = functions.getHttpsCallable("getAdminStats").call().await()
        @Suppress("UNCHECKED_CAST")
        AfrResult.Success(result.data as? Map<String, Any> ?: emptyMap())
    } catch (e: Exception) {
        AfrResult.Error("Impossible de charger les statistiques.", e)
    }
}
