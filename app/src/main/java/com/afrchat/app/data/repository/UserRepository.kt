package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun usersRef() = firestore.collection("users")

    suspend fun getUser(uid: String): AfrResult<User> {
        return try {
        val snap = usersRef().document(uid).get().await()
        val user = snap.toObject(User::class.java)
        if (user != null) AfrResult.Success(user) else AfrResult.Error("Utilisateur introuvable.")
    } catch (e: Exception) {
        AfrResult.Error("Impossible de charger le profil.", e)
    }

    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val reg = usersRef().document(uid).addSnapshotListener { snap, _ ->
            trySend(snap?.toObject(User::class.java))
        }
        awaitClose { reg.remove() }
    }

    suspend fun updateProfile(uid: String, updates: Map<String, Any?>): AfrResult<Unit> {
        return try {
        usersRef().document(uid).update(updates).await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("La mise à jour du profil a échoué.", e)
    }

    suspend fun setOnlineStatus(uid: String, isOnline: Boolean) {
        try {
            usersRef().document(uid).update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()
        } catch (_: Exception) { /* best-effort, ne bloque jamais l'UI */ }
    }

    suspend fun searchUsers(query: String, excludeUid: String): AfrResult<List<User>> = try {
        if (query.isBlank()) return AfrResult.Success(emptyList())
        val trimmed = query.trim()
        val lower = trimmed.lowercase()

        // Firestore ne supporte pas les recherches "contains" natives : on recherche par préfixe
        // sur un champ dénormalisé "firstNameLower" (maintenu automatiquement par la Cloud
        // Function onUserWrite à chaque écriture d'un profil — voir firebase/functions/index.js).
        val byName = usersRef()
            .orderBy("firstNameLower")
            .startAt(lower).endAt(lower + "\uf8ff")
            .limit(20).get().await()
            .toObjects(User::class.java)

        // Si la requête ressemble à un numéro de téléphone, recherche aussi par préfixe exact du champ "phone".
        val byPhone = if (trimmed.all { it.isDigit() || it == '+' } && trimmed.length >= 3) {
            usersRef()
                .orderBy("phone")
                .startAt(trimmed).endAt(trimmed + "\uf8ff")
                .limit(20).get().await()
                .toObjects(User::class.java)
        } else emptyList()

        val users = (byName + byPhone).distinctBy { it.uid }.filter { it.uid != excludeUid && !it.isBanned }
        AfrResult.Success(users)
    } catch (e: Exception) {
        AfrResult.Error("La recherche a échoué.", e)
    }

    suspend fun registerFcmToken(uid: String) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            usersRef().document(uid).update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token)).await()
        } catch (_: Exception) { }
    }

    suspend fun removeFcmToken(uid: String, token: String) {
        try {
            usersRef().document(uid).update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayRemove(token)).await()
        } catch (_: Exception) { }
    }
}

}

}
