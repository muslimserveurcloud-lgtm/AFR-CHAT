package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.Story
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Statuts éphémères (24h). L'expiration réelle (suppression des documents et fichiers Storage)
 * est effectuée côté serveur par une Cloud Function planifiée (voir firebase/functions/index.js :
 * cleanupExpiredStories, exécutée toutes les heures). Le champ expiresAt sert aussi de filtre
 * client pour ne jamais afficher un statut expiré, même avant le passage du nettoyage planifié.
 */
@Singleton
class StoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun storiesRef() = firestore.collection("stories")

    suspend fun postStory(story: Story): AfrResult<Unit> = try {
        val doc = storiesRef().document()
        storiesRef().document(doc.id).set(story.copy(id = doc.id)).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("La publication du statut a échoué.", e)
    }

    /** Statuts actifs des contacts donnés (contacts = utilisateurs avec qui une conversation existe). */
    fun observeActiveStories(ownerUids: List<String>): Flow<List<Story>> = callbackFlow {
        if (ownerUids.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg = storiesRef()
            .whereIn("ownerUid", ownerUids.take(30)) // limite Firestore whereIn = 30
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .orderBy("expiresAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Story::class.java).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    suspend fun markViewed(storyId: String, viewerUid: String) {
        try {
            storiesRef().document(storyId).update("viewerUids", FieldValue.arrayUnion(viewerUid)).await()
        } catch (_: Exception) { }
    }

    suspend fun deleteStory(storyId: String): AfrResult<Unit> = try {
        storiesRef().document(storyId).delete().await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Suppression impossible.", e)
    }
}
