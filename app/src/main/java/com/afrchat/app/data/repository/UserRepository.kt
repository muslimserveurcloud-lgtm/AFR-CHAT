package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.PrivacySettings
import com.afrchat.app.data.model.User
import com.afrchat.app.data.remote.ProfileRow
import com.afrchat.app.data.remote.RealtimeHub
import com.afrchat.app.data.remote.TableSpec
import com.afrchat.app.data.remote.jsonOf
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val client: SupabaseClient,
    private val hub: RealtimeHub
) {
    private val profiles get() = client.postgrest.from("profiles")

    private suspend fun fetchUser(uid: String): User? =
        profiles.select { filter { eq("id", uid) } }.decodeList<ProfileRow>().firstOrNull()?.toModel()

    suspend fun getUser(uid: String): AfrResult<User> = try {
        val user = fetchUser(uid)
        if (user != null) AfrResult.Success(user) else AfrResult.Error("Utilisateur introuvable.")
    } catch (e: Exception) {
        AfrResult.Error("Impossible de charger le profil.", e)
    }

    fun observeUser(uid: String): Flow<User?> =
        hub.observe("profile-$uid", TableSpec("profiles", "id", uid)) { fetchUser(uid) }

    /** Clés acceptées (noms "métier" des modèles) -> colonnes Postgres. */
    private val columnFor = mapOf(
        "firstName" to "first_name",
        "lastName" to "last_name",
        "phone" to "phone",
        "photoUrl" to "photo_url",
        "statusMessage" to "status_message"
    )

    suspend fun updateProfile(uid: String, updates: Map<String, Any?>): AfrResult<Unit> = try {
        val row = jsonOf(*updates.mapNotNull { (k, v) -> columnFor[k]?.let { it to v } }.toTypedArray())
        if (row.isNotEmpty()) profiles.update(row) { filter { eq("id", uid) } }
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("La mise à jour du profil a échoué.", e)
    }

    suspend fun updatePrivacy(uid: String, privacy: PrivacySettings): AfrResult<Unit> = try {
        val json = jsonOf(
            "privacy" to jsonOf(
                "showLastSeen" to privacy.showLastSeen,
                "showOnlineStatus" to privacy.showOnlineStatus,
                "showReadReceipts" to privacy.showReadReceipts,
                "whoCanAddToGroups" to privacy.whoCanAddToGroups
            )
        )
        profiles.update(json) { filter { eq("id", uid) } }
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("La mise à jour des paramètres a échoué.", e)
    }

    suspend fun setOnlineStatus(uid: String, isOnline: Boolean) {
        try {
            profiles.update(jsonOf("is_online" to isOnline, "last_seen" to System.currentTimeMillis())) {
                filter { eq("id", uid) }
            }
        } catch (_: Exception) { /* best-effort, ne bloque jamais l'UI */ }
    }

    suspend fun searchUsers(query: String, excludeUid: String): AfrResult<List<User>> = try {
        if (query.isBlank()) return AfrResult.Success(emptyList())
        val trimmed = query.trim()
        val lower = trimmed.lowercase()

        // Recherche par préfixe sur prénom, nom (colonnes générées en minuscules, indexées)
        val byFirst = profiles.select {
            filter { like("first_name_lower", "$lower%"); neq("id", excludeUid); eq("is_banned", false) }
            limit(20)
        }.decodeList<ProfileRow>()

        val byLast = profiles.select {
            filter { like("last_name_lower", "$lower%"); neq("id", excludeUid); eq("is_banned", false) }
            limit(20)
        }.decodeList<ProfileRow>()

        // Si la requête ressemble à un numéro, recherche aussi par préfixe du champ "phone".
        val byPhone = if (trimmed.all { it.isDigit() || it == '+' } && trimmed.length >= 3) {
            profiles.select {
                filter { like("phone", "$trimmed%"); neq("id", excludeUid); eq("is_banned", false) }
                limit(20)
            }.decodeList<ProfileRow>()
        } else emptyList()

        AfrResult.Success((byFirst + byLast + byPhone).distinctBy { it.id }.map { it.toModel() })
    } catch (e: Exception) {
        AfrResult.Error("La recherche a échoué.", e)
    }
}
