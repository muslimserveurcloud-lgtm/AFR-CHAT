package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inscription, connexion, déconnexion et session via Supabase Auth (e-mail / mot de passe).
 * La session est persistée localement et rafraîchie automatiquement par supabase-kt.
 * Le profil (table "profiles") est créé côté serveur par le trigger handle_new_user.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient
) {
    private val auth get() = client.auth

    val currentUserId: String? get() = auth.currentUserOrNull()?.id
    val currentEmail: String get() = auth.currentUserOrNull()?.email.orEmpty()
    val isLoggedIn: Boolean get() = auth.currentSessionOrNull() != null

    /** true / false à chaque changement de session (connexion, déconnexion, expiration). */
    val isLoggedInFlow: Flow<Boolean> = auth.sessionStatus.map { auth.currentSessionOrNull() != null }

    /** Attend que la session sauvegardée soit chargée au démarrage de l'app. */
    suspend fun awaitReady() {
        try { auth.awaitInitialization() } catch (_: Exception) { }
    }

    suspend fun signUp(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        phone: String = ""
    ): AfrResult<String> = try {
        val info = auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("first_name", firstName)
                put("last_name", lastName)
                put("phone", phone)
            }
        }
        val uid = auth.currentUserOrNull()?.id
        when {
            uid != null -> AfrResult.Success(uid)
            // Confirmation par e-mail activée côté Supabase : le compte existe mais pas encore de session.
            info != null -> {
                // Compte créé mais pas de session (confirmation e-mail côté serveur) : on tente la connexion directe.
                try {
                    auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    AfrResult.Success(auth.currentUserOrNull()?.id.orEmpty())
                } catch (_: Exception) {
                    AfrResult.Error("Compte créé ! Confirme ton adresse e-mail (lien reçu par mail), puis connecte-toi.")
                }
            }
            else -> AfrResult.Error("Impossible de créer le compte.")
        }
    } catch (e: Exception) {
        AfrResult.Error(mapAuthError(e), e)
    }

    suspend fun login(email: String, password: String): AfrResult<String> = try {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        AfrResult.Success(auth.currentUserOrNull()?.id.orEmpty())
    } catch (e: Exception) {
        AfrResult.Error(mapAuthError(e), e)
    }

    suspend fun sendPasswordReset(email: String): AfrResult<Unit> = try {
        auth.resetPasswordForEmail(email)
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error(mapAuthError(e), e)
    }

    suspend fun logout() {
        try { auth.signOut() } catch (_: Exception) { }
    }

    /** Traduit les erreurs Supabase Auth en messages compréhensibles, en français. */
    private fun mapAuthError(e: Exception): String {
        val m = e.message.orEmpty()
        return when {
            m.contains("invalid login credentials", true) -> "E-mail ou mot de passe incorrect."
            m.contains("email not confirmed", true) -> "Confirme d'abord ton adresse e-mail (lien reçu par mail)."
            m.contains("already registered", true) || m.contains("already been registered", true) -> "Cet e-mail est déjà utilisé."
            m.contains("password should be at least", true) || m.contains("weak_password", true) -> "Le mot de passe doit contenir au moins 6 caractères."
            m.contains("valid email", true) || m.contains("invalid format", true) || m.contains("email_address_invalid", true) -> "L'adresse e-mail n'est pas valide."
            m.contains("rate limit", true) || m.contains("over_email_send_rate_limit", true) -> "Trop de tentatives. Réessaie dans quelques minutes."
            m.contains("banned", true) -> "Ce compte a été suspendu."
            m.contains("unable to resolve host", true) || m.contains("timeout", true) || m.contains("network", true) -> "Vérifiez votre connexion internet."
            else -> "Une erreur est survenue. Veuillez réessayer."
        }
    }
}
