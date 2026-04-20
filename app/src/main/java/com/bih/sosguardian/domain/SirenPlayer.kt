package com.bih.sosguardian.domain

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

class SirenPlayer(
    private val context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioTrack: AudioTrack? = null
    private var previousAlarmVolume: Int? = null
    private var isPlaying = false

    fun start(volumeFraction: Float) {
        if (isPlaying) return
        isPlaying = true

        val sampleRate = 44100
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        val targetVolume = (maxVolume * volumeFraction.coerceIn(0.2f, 1f)).toInt().coerceAtLeast(1)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        // Generate a high-attention police-style "Wail" siren
        Thread {
            val samples = ShortArray(minBufferSize)
            var phase = 0.0
            var wailPhase = 0.0
            
            while (isPlaying) {
                // Police wail frequency modulation: oscillates between 600Hz and 1400Hz
                val wailFreq = 0.5 // Modulation speed (Hz)
                val baseFreq = 1000.0
                val freqRange = 400.0
                val currentFreq = baseFreq + sin(2.0 * PI * wailPhase) * freqRange
                wailPhase += wailFreq / sampleRate
                if (wailPhase > 1.0) wailPhase -= 1.0

                for (i in samples.indices) {
                    samples[i] = (sin(2.0 * PI * phase) * Short.MAX_VALUE).toInt().toShort()
                    phase += currentFreq / sampleRate
                    if (phase > 1.0) phase -= 1.0
                }
                audioTrack?.write(samples, 0, samples.size)
            }
        }.start()
    }

    fun stop() {
        isPlaying = false
        audioTrack?.runCatching {
            stop()
            release()
        }
        audioTrack = null
        previousAlarmVolume?.let {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0)
        }
        previousAlarmVolume = null
    }
}
