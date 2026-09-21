package com.afrchat.app.data.repository

import com.afrchat.app.data.model.Group
import com.afrchat.app.data.model.User
import com.afrchat.app.data.model.AfrResult
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class GroupRepository(
    private val firestore: FirebaseFirestore
) {

    private fun groupsRef() = firestore.collection("groups")

    suspend fun createGroup(
        group: Group
    ): AfrResult<String> {
        return try {
            val ref = groupsRef().document()
            val newGroup = group.copy(id = ref.id)
            ref.set(newGroup).await()
            AfrResult.Success(ref.id)
        } catch (e: Exception) {
            AfrResult.Error("Impossible de créer le groupe.", e)
        }
    }

    suspend fun getGroup(
        groupId: String
    ): AfrResult<Group> {
        return try {
            val snapshot = groupsRef().document(groupId).get().await()
            val group = snapshot.toObject(Group::class.java)

            if (group != null) {
                AfrResult.Success(group)
            } else {
                AfrResult.Error("Groupe introuvable.")
            }
        } catch (e: Exception) {
            AfrResult.Error("Impossible de récupérer le groupe.", e)
        }
    }

    suspend fun addMember(
        groupId: String,
        uid: String
    ): AfrResult<Unit> {
        return try {
            groupsRef().document(groupId)
                .update("memberUids", FieldValue.arrayUnion(uid))
                .await()

            AfrResult.Success(Unit)
        } catch (e: Exception) {
            AfrResult.Error("Impossible d'ajouter ce membre.", e)
        }
    }

    suspend fun removeMember(
        groupId: String,
        uid: String
    ): AfrResult<Unit> {
        return try {
            groupsRef().document(groupId)
                .update("memberUids", FieldValue.arrayRemove(uid))
                .await()

            AfrResult.Success(Unit)
        } catch (e: Exception) {
            AfrResult.Error("Impossible de retirer ce membre.", e)
        }
    }

    suspend fun setAdmin(
        groupId: String,
        targetUid: String,
        requesterUid: String,
        isAdmin: Boolean
    ): AfrResult<Unit> {
        return try {
            val group = groupsRef()
                .document(groupId)
                .get()
                .await()
                .toObject(Group::class.java)
                ?: return AfrResult.Error("Groupe introuvable.")

            if (requesterUid != group.ownerUid) {
                return AfrResult.Error(
                    "Seul le propriétaire peut gérer les administrateurs."
                )
            }

            val update = if (isAdmin) {
                FieldValue.arrayUnion(targetUid)
            } else {
                FieldValue.arrayRemove(targetUid)
            }

            groupsRef()
                .document(groupId)
                .update("adminUids", update)
                .await()

            AfrResult.Success(Unit)
        } catch (e: Exception) {
            AfrResult.Error("Action impossible.", e)
        }
    }

    suspend fun updateGroupInfo(
        groupId: String,
        name: String?,
        description: String?,
        photoUrl: String?
    ): AfrResult<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()

            name?.let {
                updates["name"] = it
            }

            description?.let {
                updates["description"] = it
            }

            photoUrl?.let {
                updates["photoUrl"] = it
            }

            if (updates.isNotEmpty()) {
                groupsRef()
                    .document(groupId)
                    .update(updates)
                    .await()
            }

            AfrResult.Success(Unit)
        } catch (e: Exception) {
            AfrResult.Error(
                "La mise à jour du groupe a échoué.",
                e
            )
        }
    }

    suspend fun setOnlyAdminsCanPost(
        groupId: String,
        value: Boolean
    ) {
        groupsRef()
            .document(groupId)
            .update("onlyAdminsCanPost", value)
            .await()
    }
}
