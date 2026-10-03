package com.afrchat.app.data.repository

import com.afrchat.app.data.model.AfrResult
import com.afrchat.app.data.model.Story
import com.afrchat.app.data.remote.RealtimeHub
import com.afrchat.app.data.remote.StoryInsert
import com.afrchat.app.data.remote.StoryRow
import com.afrchat.app.data.remote.TableSpec
import com.afrchat.app.data.remote.jsonOf
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Statuts éphémères (24h). Les lignes expirées sont purgées toutes les heures par pg_cron
 * (voir supabase/migrations) ; la politique RLS et le filtre ci-dessous masquent de toute façon
 * un statut expiré avant le passage de la purge.
 */
@Singleton
class StoryRepository @Inject constructor(
    private val client: SupabaseClient,
    private val hub: RealtimeHub
) {
    private val db get() = client.postgrest

    suspend fun postStory(story: Story): AfrResult<Unit> = try {
        db.from("stories").insert(
            StoryInsert(
                ownerId = story.ownerUid, type = story.type, content = story.content,
                mediaUrl = story.mediaUrl, backgroundColor = story.backgroundColor
            )
        )
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("La publication du statut a échoué.", e)
    }

    /** Statuts actifs des contacts donnés (contacts = utilisateurs avec qui une conversation existe). */
    fun observeActiveStories(ownerUids: List<String>): Flow<List<Story>> {
        if (ownerUids.isEmpty()) return flowOf(emptyList())
        return hub.observe("stories", TableSpec("stories")) {
            db.from("stories").select {
                filter {
                    isIn("owner_id", ownerUids)
                    gt("expires_at", System.currentTimeMillis())
                }
                order("expires_at", Order.DESCENDING)
            }.decodeList<StoryRow>().map { it.toModel() }
        }
    }

    suspend fun markViewed(storyId: String, viewerUid: String) {
        try { db.rpc("mark_story_viewed", jsonOf("story" to storyId)) } catch (_: Exception) { }
    }

    suspend fun deleteStory(storyId: String): AfrResult<Unit> = try {
        db.from("stories").delete { filter { eq("id", storyId) } }
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Suppression impossible.", e)
    }
}
