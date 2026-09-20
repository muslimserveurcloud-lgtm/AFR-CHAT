package com.afrchat.app.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.afrchat.app.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Firestore rejoue déjà automatiquement les écritures locales en attente dès que la connexion
 * revient (persistance activée dans AfrChatApplication). Ce worker gère les tâches
 * complémentaires qui ne sont pas de simples écritures Firestore : republier le statut "en ligne"
 * et rafraîchir le jeton FCM après une longue coupure réseau.
 */
@HiltWorker
class OfflineMessageSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userRepository: UserRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        return try {
            userRepository.setOnlineStatus(uid, true)
            userRepository.registerFcmToken(uid)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
