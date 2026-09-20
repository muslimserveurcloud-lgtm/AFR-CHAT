package com.afrchat.app.ui.chat

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Enregistreur de messages vocaux basé sur MediaRecorder (format AAC/M4A, compression raisonnable).
 * Le fichier est stocké temporairement dans le cache de l'app puis envoyé via MediaRepository.
 */
class VoiceMessageRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = 0L

    fun start(): Boolean {
        return try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            outputFile = file
            recorder = (if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            startTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Retourne l'URI du fichier enregistré et sa durée en millisecondes, ou null en cas d'échec. */
    fun stop(): Pair<Uri, Long>? {
        return try {
            recorder?.apply { stop(); release() }
            recorder = null
            val duration = System.currentTimeMillis() - startTime
            val file = outputFile ?: return null
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            uri to duration
        } catch (e: Exception) {
            null
        }
    }

    fun cancel() {
        try { recorder?.apply { stop(); release() } } catch (_: Exception) { }
        recorder = null
        outputFile?.delete()
    }
}
