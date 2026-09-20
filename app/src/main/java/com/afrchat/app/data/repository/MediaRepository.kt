package com.afrchat.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import com.afrchat.app.data.model.AfrResult
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Upload des médias (photos, vidéos, fichiers, messages vocaux) vers Firebase Storage.
 * Les règles storage.rules garantissent qu'un utilisateur ne peut écrire que dans son propre
 * dossier et que seuls les participants d'une conversation peuvent lire les fichiers associés.
 */
@Singleton
class MediaRepository @Inject constructor(
    private val storage: FirebaseStorage
) {
    /** Émet la progression (0..100) puis l'URL de téléchargement finale. */
    fun uploadFile(
        context: Context,
        localUri: Uri,
        folder: String, // "chat_images" | "chat_videos" | "chat_files" | "voice_messages" | "profile_photos" | "stories" | "group_photos"
        ownerUid: String,
        contentType: String
    ): Flow<UploadState> = callbackFlow {
        val extension = contentType.substringAfterLast("/", "bin")
        val path = "$folder/$ownerUid/${UUID.randomUUID()}.$extension"
        val ref = storage.reference.child(path)
        val metadata = StorageMetadata.Builder().setContentType(contentType).build()

        val task = ref.putFile(localUri, metadata)
        task.addOnProgressListener { snapshot ->
            val progress = ((100.0 * snapshot.bytesTransferred) / snapshot.totalByteCount).toInt()
            trySend(UploadState.Progress(progress))
        }
        task.addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { uri ->
                trySend(UploadState.Success(uri.toString()))
                close()
            }
        }
        task.addOnFailureListener {
            trySend(UploadState.Error(it.message ?: "Échec de l'envoi du fichier."))
            close()
        }
        awaitClose { task.cancel() }
    }

    suspend fun deleteFile(url: String): AfrResult<Unit> = try {
        storage.getReferenceFromUrl(url).delete().await()
        AfrResult.Success(Unit)
    } catch (e: Exception) {
        AfrResult.Error("Suppression du fichier impossible.", e)
    }

    sealed class UploadState {
        data class Progress(val percent: Int) : UploadState()
        data class Success(val url: String) : UploadState()
        data class Error(val message: String) : UploadState()
    }
}
