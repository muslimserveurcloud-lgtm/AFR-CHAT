package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.PrivacySettings
import com.afrchat.app.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gère l'inscription, la connexion, la déconnexion et la session utilisateur
 * via Firebase Authentication (e-mail/mot de passe).
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUserId: String? get() = auth.currentUser?.uid
    val isLoggedIn: Boolean get() = auth.currentUser != null

    fun observeAuthState(onChanged: (Boolean) -> Unit): FirebaseAuth.AuthStateListener {
        val listener = FirebaseAuth.AuthStateListener { onChanged(it.currentUser != null) }
        auth.addAuthStateListener(listener)
        return listener
    }

    fun removeAuthListener(listener: FirebaseAuth.AuthStateListener) =
        auth.removeAuthStateListener(listener)

    suspend fun signUp(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        phone: String = ""
    ): AfrResult<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid

            if (uid == null) {
                AfrResult.Error("Impossible de créer le compte.")
            } else {
                val user = User(
                    uid = uid,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    phone = phone,
                    privacy = PrivacySettings()
                )

                firestore.collection("users")
                    .document(uid)
                    .set(user)
                    .await()

                AfrResult.Success(uid)
            }
        } catch (e: Exception) {
            AfrResult.Error(mapAuthError(e), e)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): AfrResult<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            AfrResult.Success(result.user?.uid.orEmpty())
        } catch (e: Exception) {
            AfrResult.Error(mapAuthError(e), e)
        }
    }

    suspend fun sendPasswordReset(email: String): AfrResult<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AfrResult.Success(Unit)
        } catch (e: Exception) {
            AfrResult.Error(mapAuthError(e), e)
        }
    }

    fun logout() {
        auth.currentUser?.uid?.let { uid ->
            // Le statut hors-ligne est mis à jour par UserRepository avant l'appel à logout().
        }
        auth.signOut()
    }

    /** Traduit les erreurs Firebase en messages compréhensibles, en français. */
    private fun mapAuthError(e: Exception): String = when {
        e.message?.contains("badly formatted", true) == true ->
            "L'adresse e-mail n'est pas valide."

        e.message?.contains("email address is already in use", true) == true ->
            "Cet e-mail est déjà utilisé."

        e.message?.contains("password is invalid", true) == true ->
            "Mot de passe incorrect."

        e.message?.contains("no user record", true) == true ->
            "Aucun compte associé à cet e-mail."

        e.message?.contains("WEAK_PASSWORD", true) == true ->
            "Le mot de passe doit contenir au moins 6 caractères."

        e.message?.contains("network", true) == true ->
            "Vérifiez votre connexion internet."

        else ->
            "Une erreur est survenue. Veuillez réessayer."
    }
}
