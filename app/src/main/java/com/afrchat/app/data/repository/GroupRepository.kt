package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.Group
import com.afrchat.app.data.remote.GroupRow
import com.afrchat.app.data.remote.RealtimeHub
import com.afrchat.app.data.remote.TableSpec
import com.afrchat.app.data.remote.jsonOf
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Groupes. Toutes les écritures passent par des fonctions RPC (supabase/migrations) qui vérifient
 * côté serveur les rôles (propriétaire / admin / membre) : le client ne peut pas les contourner.
 */
@Singleton
class GroupRepository @Inject constructor(
    private val client: SupabaseClient,
    private val hub: RealtimeHub
) {
    private val db get() = client.postgrest

    /** Crée le groupe et sa conversation ; renvoie l'id de la conversation. */
    suspend fun createGroup(name: String, ownerUid: String, memberUids: List<String>, photoUrl: String = ""): AfrResult<String> = try {
        val conversationId = db.rpc(
            "create_group",
            jsonOf("p_name" to name, "p_member_ids" to memberUids.filter { it != ownerUid }, "p_photo_url" to photoUrl)
        ).decodeAs<String>()
        AfrResult.Success(conversationId)
    } catch (e: Exception) {
        AfrResult.Error("La création du groupe a échoué.", e)
    }

    private suspend fun fetchGroup(groupId: String): Group? =
        db.from("groups").select(Columns.raw("*, group_members(*)")) { filter { eq("id", groupId) } }
            .decodeList<GroupRow>().firstOrNull()?.toModel()

    fun observeGroup(groupId: String): Flow<Group?> =
        hub.observe(
            "group-$groupId",
            TableSpec("groups", "id", groupId),
            TableSpec("group_members", "group_id", groupId)
        ) { fetchGroup(groupId) }

    suspend fun addMembers(groupId: String, conversationId: String, uids: List<String>): AfrResult<Unit> = try {
        db.rpc("add_group_members", jsonOf("g" to groupId, "uids" to uids))
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Impossible d'ajouter ces membres.", e)
    }

    suspend fun removeMember(groupId: String, conversationId: String, uid: String, requesterUid: String): AfrResult<Unit> = try {
        db.rpc("remove_group_member", jsonOf("g" to groupId, "uid" to uid))
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Seuls les administrateurs peuvent retirer un membre.", e)
    }

    suspend fun leaveGroup(groupId: String, conversationId: String, uid: String) =
        removeMember(groupId, conversationId, uid, requesterUid = uid)

    suspend fun setAdmin(groupId: String, targetUid: String, requesterUid: String, isAdmin: Boolean): AfrResult<Unit> = try {
        db.rpc("set_group_admin", jsonOf("g" to groupId, "uid" to targetUid, "make_admin" to isAdmin))
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Seul le propriétaire peut gérer les administrateurs.", e)
    }

    suspend fun updateGroupInfo(groupId: String, name: String?, description: String?, photoUrl: String?): AfrResult<Unit> = try {
        db.rpc(
            "update_group_info",
            jsonOf("g" to groupId, "p_name" to name, "p_description" to description, "p_photo_url" to photoUrl)
        )
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("La mise à jour du groupe a échoué.", e)
    }

    suspend fun setOnlyAdminsCanPost(groupId: String, value: Boolean) {
        db.rpc("set_only_admins_can_post", jsonOf("g" to groupId, "v" to value))
    }
}
