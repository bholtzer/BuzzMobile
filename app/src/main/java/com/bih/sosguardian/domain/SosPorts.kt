package com.bih.sosguardian.domain

import com.bih.sosguardian.data.CallStatus
import com.bih.sosguardian.data.LocationShareStatus
import com.bih.sosguardian.data.SosSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface SettingsRepository {
    val settings: StateFlow<SosSettings>

    suspend fun updateSettings(transform: (SosSettings) -> SosSettings)
}

interface EmergencyCaller {
    fun placeDirectCall(number: String): CallStatus
}

data class SosLocation(
    val latitude: Double,
    val longitude: Double,
)

interface LocationMessenger {
    fun sendLocationMessage(rawContacts: String): LocationShareStatus
    fun getBestLastKnownLocation(): SosLocation?
}

interface PhotoCapturer {
    fun capturePhoto(): File?
}

interface SirenController {
    fun start(volumeFraction: Float)
    fun stop()
}

interface FlashController {
    fun hasFlash(): Boolean
    fun start(scope: CoroutineScope, blinkMs: Long)
    suspend fun stop()
}

interface SosPlatformActions {
    fun vibrate()
    fun startForegroundSync()
    fun launchMainActivity()
    fun sendWhatsAppAlert(
        whatsappNumber: String,
        photoFile: File?,
        lastLocation: SosLocation?,
    )
}

interface MonitoringServiceController {
    fun start()
    fun stop()
}
