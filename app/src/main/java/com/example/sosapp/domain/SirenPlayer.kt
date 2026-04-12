package com.example.sosapp.domain

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager

class SirenPlayer(
    private val context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var previousAlarmVolume: Int? = null

    fun start(volumeFraction: Float) {
        if (mediaPlayer?.isPlaying == true) return

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val targetVolume = (maxVolume * volumeFraction.coerceIn(0.2f, 1f)).toInt().coerceAtLeast(1)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(context, alarmUri)
            isLooping = true
            prepare()
            start()
        }
    }

    fun stop() {
        mediaPlayer?.runCatching {
            stop()
            release()
        }
        mediaPlayer = null
        previousAlarmVolume?.let {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0)
        }
        previousAlarmVolume = null
    }
}
