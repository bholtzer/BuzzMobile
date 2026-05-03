package com.bih.mangosos.data.device

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.bih.mangosos.data.CallStatus
import com.bih.mangosos.domain.EmergencyCaller

class CallHandler(
    private val context: Context,
) : EmergencyCaller {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun placeDirectCall(number: String): CallStatus {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            return openDialerFallback(number)
        }

        val cleanNumber = number.filter { it.isDigit() || it == '+' }
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(cleanNumber)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        routeCallToSpeaker()
        return runIntent(intent, CallStatus.PLACED_DIRECT_CALL)
            ?.also { scheduleSpeakerRoutingRetries() }
            ?: openDialerFallback(number)
    }

    fun openDialerFallback(number: String): CallStatus {
        val cleanNumber = number.filter { it.isDigit() || it == '+' }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(cleanNumber)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runIntent(intent, CallStatus.OPENED_DIALER_FALLBACK) ?: CallStatus.FAILED
    }

    private fun runIntent(intent: Intent, successStatus: CallStatus): CallStatus? {
        return try {
            context.startActivity(intent)
            successStatus
        } catch (e: SecurityException) {
            null
        } catch (e: ActivityNotFoundException) {
            null
        }
    }

    private fun scheduleSpeakerRoutingRetries() {
        mainHandler.postDelayed({ routeCallToSpeaker() }, 600L)
        mainHandler.postDelayed({ routeCallToSpeaker() }, 1500L)
        mainHandler.postDelayed({ routeCallToSpeaker() }, 3000L)
    }

    private fun routeCallToSpeaker() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val speaker = audioManager.availableCommunicationDevices
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                if (speaker != null) {
                    audioManager.setCommunicationDevice(speaker)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
            }
        }
    }
}
