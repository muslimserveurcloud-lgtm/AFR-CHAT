package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.Conversation
import com.afrchat.app.data.model.Group
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun groupsRef() = firestore.collection("groups")
    private fun conversationsRef() = firestore.collection("conversations")

    suspend fun createGroup(name: String, ownerUid: String, memberUids: List<String>, photoUrl: String = ""): AfrResult<String> {
        return try {
        val groupDoc = groupsRef().document()
        val allMembers = (memberUids + ownerUid).distinct()
        val group = Group(
            id = groupDoc.id,
            name = name,
            photoUrl = photoUrl,
            ownerUid = ownerUid,
            adminUids = listOf(ownerUid),
            memberUids = allMembers
        )
        groupDoc.set(group).await()

        val convoDoc = conversationsRef().document()
        val conversation = Conversation(
            id = convoDoc.id,
            type = "group",
            groupId = groupDoc.id,
            participantIds = allMembers,
            lastMessage = "Groupe créé",
            lastMessageAt = System.currentTimeMillis()
        )
        convoDoc.set(conversation).await()
        // On relie la conversation au groupe pour navigation facile
        groupsRef().document(groupDoc.id).update("conversationId", convoDoc.id).await()

        AfrResult.Success(convoDoc.id)
    } catch (e: Exception) {
        AfrResult.Error("La création du groupe a échoué.", e)
    }

    fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val reg = groupsRef().document(groupId).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(Group::class.java))
        }
        awaitClose { reg.remove() }
    }

    suspend fun addMembers(groupId: String, conversationId: String, uids: List<String>): AfrResult<Unit> {
        return try {
        groupsRef().document(groupId).update("memberUids", FieldValue.arrayUnion(*uids.toTypedArray())).await()
        conversationsRef().document(conversationId).update("participantIds", FieldValue.arrayUnion(*uids.toTypedArray())).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Impossible d'ajouter ces membres.", e)
    }

    suspend fun removeMember(groupId: String, conversationId: String, uid: String, requesterUid: String): AfrResult<Unit> {
        return try {
        val group = groupsRef().document(groupId).get().await().toObject(Group::class.java)
            ?: return AfrResult.Error("Groupe introuvable.")
        if (requesterUid !in group.adminUids && requesterUid != uid) {
            return AfrResult.Error("Seuls les administrateurs peuvent retirer un membre.")
        }
        groupsRef().document(groupId).update(
            mapOf(
                "memberUids" to FieldValue.arrayRemove(uid),
                "adminUids" to FieldValue.arrayRemove(uid)
            )
        ).await()
        conversationsRef().document(conversationId).update("participantIds", FieldValue.arrayRemove(uid)).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Impossible de retirer ce membre.", e)
    }

    suspend fun leaveGroup(groupId: String, conversationId: String, uid: String) =
        removeMember(groupId, conversationId, uid, requesterUid = uid)

    suspend fun setAdmin(groupId: String, targetUid: String, requesterUid: String, isAdmin: Boolean): AfrResult<Unit> {
        return try {
        val group = groupsRef().document(groupId).get().await().toObject(Group::class.java)
            ?: return AfrResult.Error("Groupe introuvable.")
        if (requesterUid != group.ownerUid) return AfrResult.Error("Seul le propriétaire peut gérer les administrateurs.")

        val update = if (isAdmin) FieldValue.arrayUnion(targetUid) else FieldValue.arrayRemove(targetUid)
        groupsRef().document(groupId).update("adminUids", update).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Action impossible.", e)
    }

    suspend fun updateGroupInfo(groupId: String, name: String?, description: String?, photoUrl: String?): AfrResult<Unit> {
        return try {
        val updates = mutableMapOf<String, Any>()
        name?.let { updates["name"] = it }
        description?.let { updates["description"] = it }
        photoUrl?.let { updates["photoUrl"] = it }
        if (updates.isNotEmpty()) groupsRef().document(groupId).update(updates).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("La mise à jour du groupe a échoué.", e)
    }

    suspend fun setOnlyAdminsCanPost(groupId: String, value: Boolean) {
        groupsRef().document(groupId).update("onlyAdminsCanPost", value).await()
    }
}

}
