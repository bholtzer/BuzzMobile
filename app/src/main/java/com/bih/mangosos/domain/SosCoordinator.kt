package com.bih.mangosos.domain

import com.bih.mangosos.data.CallStatus
import com.bih.mangosos.data.LocationShareStatus
import com.bih.mangosos.data.SosMode
import com.bih.mangosos.data.SosRuntimeState
import com.bih.mangosos.data.StopReason
import com.bih.mangosos.data.TriggerSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SosCoordinator(
    private val settingsRepository: SettingsRepository,
    private val sirenController: SirenController,
    private val flashController: FlashController,
    private val emergencyCaller: EmergencyCaller,
    private val locationMessenger: LocationMessenger,
    private val photoCapturer: PhotoCapturer,
    private val platformActions: SosPlatformActions,
    private val emergencyCloudReporter: EmergencyCloudReporter,
    private val appScope: CoroutineScope,
) {
    private val _runtimeState = MutableStateFlow(SosRuntimeState())
    val runtimeState: StateFlow<SosRuntimeState> = _runtimeState.asStateFlow()

    private var cooldownJob: Job? = null

    fun setArmed(enabled: Boolean) {
        if (_runtimeState.value.mode == SosMode.SOS_ACTIVE) return
        _runtimeState.value = SosRuntimeState(
            mode = if (enabled) SosMode.ARMED else SosMode.IDLE,
            message = if (enabled) "SOS monitoring is armed." else "SOS monitoring is idle.",
        )
    }

    fun startSos(source: TriggerSource) {
        if (_runtimeState.value.mode == SosMode.SOS_ACTIVE) return
        val settings = settingsRepository.settings.value
        
        // 1. Immediate State Change & Tactile Feedback
        _runtimeState.value = SosRuntimeState(
            mode = SosMode.TRIGGER_DETECTED,
            lastSource = source,
            message = "SOS trigger detected! Starting emergency actions...",
        )
        platformActions.vibrate()

        appScope.launch {
            // 2. Immediate UI launch before any slow background tasks (like Camera)
            platformActions.startForegroundSync()
            platformActions.launchMainActivity()
            
            // Start siren as fast as possible
            runCatching { sirenController.start(settings.sirenVolumeFraction) }
            
            // 3. Parallelize flash and photo capture
            if (flashController.hasFlash()) {
                flashController.start(appScope, settings.flashBlinkMs)
            }
            
            // Capture photo before communication, then record video after the urgent call/SMS.
            val photoFile = photoCapturer.capturePhoto()
            
            val contacts = PhoneNumberValidator.parseContacts(settings.emergencyNumber)
            val primaryContact = contacts.firstOrNull().orEmpty()
            
            val callStatus = if (primaryContact.isBlank()) {
                CallStatus.FAILED
            } else {
                emergencyCaller.placeDirectCall(primaryContact)
            }
            
            val locationShareStatus = locationMessenger.sendLocationMessage(
                rawContacts = settings.emergencyNumber,
                protectedPersonName = settings.userName,
                languageCode = settings.languageCode,
            )
            val lastLocation = locationMessenger.getBestLastKnownLocation()
            
            _runtimeState.value = SosRuntimeState(
                mode = SosMode.SOS_ACTIVE,
                sirenActive = true,
                flashActive = flashController.hasFlash(),
                callStatus = callStatus,
                locationShareStatus = locationShareStatus,
                lastSource = source,
                message = buildStatusMessage(callStatus, locationShareStatus),
            )
            
            // Refresh service notification with final status
            platformActions.startForegroundSync()
            
            if (settings.whatsappNumber.isNotBlank()) {
                platformActions.sendWhatsAppAlert(
                    whatsappNumber = settings.whatsappNumber,
                    protectedPersonName = settings.userName,
                    languageCode = settings.languageCode,
                    photoFile = photoFile,
                    lastLocation = lastLocation,
                )
            }

            val videoFile = photoCapturer.captureVideo(SOS_VIDEO_DURATION_MS)

            emergencyCloudReporter.report(
                EmergencyCloudReport(
                    protectedPersonName = settings.userName,
                    emergencyContacts = contacts,
                    emergencyContactName = settings.emergencyContactName,
                    whatsappContact = settings.whatsappNumber,
                    whatsappContactName = settings.whatsappContactName,
                    triggerSource = source.name,
                    callStatus = callStatus.name,
                    locationShareStatus = locationShareStatus.name,
                    location = lastLocation,
                    photoFile = photoFile,
                    videoFile = videoFile,
                ),
            )
        }
    }

    fun stopSos(reason: StopReason) {
        appScope.launch {
            sirenController.stop()
            flashController.stop()
            _runtimeState.value = SosRuntimeState(
                mode = SosMode.COOLDOWN,
                callStatus = CallStatus.IDLE,
                message = when (reason) {
                    StopReason.USER_STOPPED -> "SOS stopped. Cooldown is active to prevent accidental retriggers."
                    StopReason.COOLDOWN_COMPLETE -> "Cooldown complete."
                    StopReason.ERROR -> "SOS stopped after an error."
                },
            )
            cooldownJob?.cancel()
            cooldownJob = launch {
                delay(settingsRepository.settings.value.cooldownMs)
                setArmed(settingsRepository.settings.value.enabled)
            }
            platformActions.startForegroundSync()
        }
    }

    private fun buildStatusMessage(
        callStatus: CallStatus,
        locationShareStatus: LocationShareStatus,
    ): String {
        val callMessage = when (callStatus) {
            CallStatus.PLACED_DIRECT_CALL -> "Emergency call placed."
            CallStatus.OPENED_DIALER_FALLBACK -> "Direct call failed, so the dialer was opened instead."
            CallStatus.FAILED -> "Emergency call could not be started."
            CallStatus.IDLE -> "Emergency call status is idle."
        }
        val locationMessage = when (locationShareStatus) {
            LocationShareStatus.SENT -> "Location SMS sent to all emergency contacts."
            LocationShareStatus.PERMISSION_DENIED -> "Location SMS could not be sent because SMS or location permission is missing."
            LocationShareStatus.LOCATION_UNAVAILABLE -> "Location SMS could not be sent because no recent location was available."
            LocationShareStatus.NO_CONTACTS -> "No emergency contacts were available for location sharing."
            LocationShareStatus.FAILED -> "Location SMS failed."
            LocationShareStatus.IDLE -> "Location sharing is idle."
        }
        return "$callMessage $locationMessage Siren and flashlight are active."
    }

    private companion object {
        const val SOS_VIDEO_DURATION_MS = 30_000L
    }
}
