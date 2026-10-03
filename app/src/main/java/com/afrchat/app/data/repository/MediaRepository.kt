package com.afrchat.app.data.repository

import android.content.Context
import android.net.Uri
import com.afrchat.app.BuildConfig
import com.afrchat.app.data.model.AfrResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Upload des médias (photos, vidéos, fichiers, messages vocaux) vers Supabase Storage (bucket public
 * "media") via l'API REST, avec progression réelle. Les politiques de stockage (voir
 * supabase/migrations) n'autorisent chaque utilisateur à écrire que dans son propre dossier
 * "{dossier}/{uid}/". Les URL publiques contiennent un UUID impossible à deviner.
 * Attention : le plan gratuit Supabase limite la taille d'un fichier à 50 Mo.
 */
@Singleton
class MediaRepository @Inject constructor(
    private val client: SupabaseClient,
    private val http: OkHttpClient
) {
    private val base get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val publicPrefix get() = "$base/storage/v1/object/public/media/"

    /** Émet la progression (0..100) puis l'URL publique finale. */
    fun uploadFile(
        context: Context,
        localUri: Uri,
        folder: String, // "chat_images" | "chat_videos" | "chat_files" | "voice_messages" | "profile_photos" | "stories" | "group_photos"
        ownerUid: String,
        contentType: String
    ): Flow<UploadState> = callbackFlow {
        val token = client.auth.currentAccessTokenOrNull()
        if (token == null) {
            trySend(UploadState.Error("Session expirée, reconnecte-toi."))
            close()
            return@callbackFlow
        }

        val extension = contentType.substringAfterLast("/", "bin").filter { it.isLetterOrDigit() }.take(8).ifEmpty { "bin" }
        val path = "$folder/$ownerUid/${UUID.randomUUID()}.$extension"
        val size = try {
            context.contentResolver.openAssetFileDescriptor(localUri, "r")?.use { it.length } ?: -1L
        } catch (_: Exception) { -1L }

        val body = object : RequestBody() {
            override fun contentType() = contentType.toMediaTypeOrNull()
            override fun contentLength() = size
            override fun writeTo(sink: BufferedSink) {
                val input = context.contentResolver.openInputStream(localUri) ?: throw IOException("Fichier illisible")
                input.use { stream ->
                    val buffer = ByteArray(16 * 1024)
                    var sent = 0L
                    var lastPercent = -1
                    while (true) {
                        val read = stream.read(buffer)
                        if (read < 0) break
                        sink.write(buffer, 0, read)
                        sent += read
                        if (size > 0) {
                            val percent = (100L * sent / size).toInt()
                            if (percent != lastPercent) { lastPercent = percent; trySend(UploadState.Progress(percent)) }
                        }
                    }
                }
            }
        }

        val request = Request.Builder()
            .url("$base/storage/v1/object/media/$path")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .header("x-upsert", "false")
            .post(body)
            .build()

        val call = http.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                trySend(UploadState.Error(e.message ?: "Échec de l'envoi du fichier."))
                close()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) trySend(UploadState.Success(publicPrefix + path))
                    else trySend(UploadState.Error("Échec de l'envoi du fichier (${it.code})."))
                }
                close()
            }
        })
        awaitClose { call.cancel() }
    }

    suspend fun deleteFile(url: String): AfrResult<Unit> {
        val token = client.auth.currentAccessTokenOrNull() ?: return AfrResult.Error("Session expirée.")
        if (!url.startsWith(publicPrefix)) return AfrResult.Error("Fichier inconnu.")
        val path = url.removePrefix(publicPrefix)
        val request = Request.Builder()
            .url("$base/storage/v1/object/media/$path")
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        return suspendCancellableCoroutine { cont ->
            val call = http.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resume(AfrResult.Error("Suppression du fichier impossible.", e))
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        cont.resume(
                            if (it.isSuccessful) AfrResult.Success(Unit)
                            else AfrResult.Error("Suppression du fichier impossible.")
                        )
                    }
                }
            })
        }
    }

    sealed class UploadState {
        data class Progress(val percent: Int) : UploadState()
        data class Success(val url: String) : UploadState()
        data class Error(val message: String) : UploadState()
    }
}
