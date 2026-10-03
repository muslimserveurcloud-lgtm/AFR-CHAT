package com.afrchat.app.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.afrchat.app.data.repository.AuthRepository
import com.afrchat.app.data.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Tâche de resynchronisation après une coupure réseau : republie le statut "en ligne".
 * (Supabase n'a pas de file d'écriture hors-ligne intégrée : un message envoyé sans réseau
 * échoue et l'utilisateur est invité à réessayer.)
 */
@HiltWorker
class OfflineMessageSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = authRepository.currentUserId ?: return Result.success()
        return try {
            userRepository.setOnlineStatus(uid, true)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
