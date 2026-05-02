package com.bih.mangosos.domain

import com.bih.mangosos.data.CallStatus
import com.bih.mangosos.data.LocationShareStatus
import com.bih.mangosos.data.SosSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface SettingsRepository {
    val settings: StateFlow<SosSettings>

    suspend fun updateSettings(transform: (SosSettings) -> SosSettings)
}

interface AnalyticsTracker {
    fun track(
        event: String,
        properties: Map<String, String> = emptyMap(),
    )
}

data class EmergencyCloudReport(
    val protectedPersonName: String,
    val emergencyContacts: List<String>,
    val emergencyContactName: String,
    val whatsappContact: String,
    val whatsappContactName: String,
    val triggerSource: String,
    val callStatus: String,
    val locationShareStatus: String,
    val location: SosLocation?,
    val photoFile: File?,
    val videoFile: File?,
)

interface EmergencyCloudReporter {
    fun report(report: EmergencyCloudReport)
}

interface EmergencyCaller {
    fun placeDirectCall(number: String): CallStatus
}

data class SosLocation(
    val latitude: Double,
    val longitude: Double,
)

interface LocationMessenger {
    fun sendLocationMessage(
        rawContacts: String,
        protectedPersonName: String,
        languageCode: String,
    ): LocationShareStatus

    fun getBestLastKnownLocation(): SosLocation?
}

interface PhotoCapturer {
    fun capturePhoto(): File?
    fun captureVideo(durationMs: Long): File?
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
        protectedPersonName: String,
        languageCode: String,
        photoFile: File?,
        lastLocation: SosLocation?,
    )
}

interface MonitoringServiceController {
    fun start()
    fun stop()
}
