package com.afrchat.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Table surveillée, avec un filtre d'égalité optionnel (Realtime n'accepte qu'un seul filtre `eq`). */
data class TableSpec(val table: String, val column: String? = null, val value: String? = null)

/**
 * Pont unique entre Supabase Realtime et les Flow des repositories.
 *
 * Principe (simple et robuste) : à chaque changement Postgres sur les tables surveillées, on relance la
 * requête de lecture et on émet son résultat. La RLS s'applique aussi à Realtime : on ne reçoit
 * d'événements que pour les lignes que l'utilisateur a le droit de lire.
 * Tout ce qui dépend de l'API realtime-kt est concentré dans ce fichier.
 */
@Singleton
class RealtimeHub @Inject constructor(private val client: SupabaseClient) {

    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Émet Unit une fois abonné, puis à chaque changement sur l'une des tables. */
    fun changes(name: String, vararg specs: TableSpec): Flow<Unit> = channelFlow {
        val channel = client.channel("$name-${UUID.randomUUID()}")
        val streams = specs.map { spec ->
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = spec.table
                if (spec.column != null && spec.value != null) {
                    filter(spec.column, FilterOperator.EQ, spec.value)
                }
            }
        }
        streams.forEach { stream -> launch { stream.collect { send(Unit) } } }
        try {
            channel.subscribe(blockUntilSubscribed = true)
        } catch (e: Exception) {
            // Hors-ligne / Realtime indisponible : on émet quand même une première lecture
        }
        send(Unit)
        awaitClose {
            cleanupScope.launch {
                withContext(NonCancellable) { runCatching { channel.unsubscribe() } }
            }
        }
    }

    /** Résultat de [query] à l'abonnement puis après chaque changement ; les erreurs de lecture sont ignorées. */
    fun <T> observe(name: String, vararg specs: TableSpec, query: suspend () -> T): Flow<T> =
        changes(name, *specs).conflate().transform { runCatching { query() }.onSuccess { emit(it) } }
}
