package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.Report
import com.afrchat.app.data.remote.ReportInsert
import com.afrchat.app.data.remote.ReportRow
import com.afrchat.app.data.remote.jsonOf
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Toutes les actions de modération (bannir, changer un rôle admin, traiter un signalement) passent
 * par des fonctions RPC Postgres (supabase/migrations) qui vérifient côté serveur que l'appelant est
 * admin (profiles.is_admin). Le client ne peut jamais les réaliser directement, même modifié.
 */
@Singleton
class AdminRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val db get() = client.postgrest

    suspend fun submitReport(report: Report): AfrResult<Unit> = try {
        db.from("reports").insert(
            ReportInsert(report.reporterUid, report.targetType, report.targetId, report.reason, report.details)
        )
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Le signalement a échoué.", e)
    }

    suspend fun listOpenReports(): AfrResult<List<Report>> = try {
        val rows = db.from("reports").select {
            filter { eq("status", "open") }
            order("created_at", Order.DESCENDING)
        }.decodeList<ReportRow>()
        AfrResult.Success(rows.map { it.toModel() })
    } catch (e: Exception) {
        AfrResult.Error("Impossible de charger les signalements.", e)
    }

    suspend fun banUser(targetUid: String, reason: String): AfrResult<Unit> = rpcUnit("ban_user", "target_uid" to targetUid, "reason" to reason)

    suspend fun unbanUser(targetUid: String): AfrResult<Unit> = rpcUnit("unban_user", "target_uid" to targetUid)

    suspend fun resolveReport(reportId: String, status: String): AfrResult<Unit> =
        rpcUnit("resolve_report", "report_id" to reportId, "new_status" to status)

    suspend fun grantAdmin(targetUid: String, grant: Boolean): AfrResult<Unit> =
        rpcUnit("set_admin_role", "target_uid" to targetUid, "grant_admin" to grant)

    suspend fun getStats(): AfrResult<Map<String, Any>> = try {
        val obj = db.rpc("get_admin_stats").decodeAs<JsonObject>()
        AfrResult.Success(obj.mapValues { (_, v) -> (v as? JsonPrimitive)?.content ?: v.toString() })
    } catch (e: Exception) {
        AfrResult.Error("Impossible de charger les statistiques.", e)
    }

    private suspend fun rpcUnit(function: String, vararg params: Pair<String, Any?>): AfrResult<Unit> = try {
        db.rpc(function, jsonOf(*params))
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Action impossible : ${e.message}", e)
    }
}
