package com.example.sosapp.domain

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.sosapp.MainActivity
import com.example.sosapp.data.CallStatus
import com.example.sosapp.data.LocationShareStatus
import com.example.sosapp.data.SosMode
import com.example.sosapp.data.SosRuntimeState
import com.example.sosapp.data.SosSettingsStore
import com.example.sosapp.data.StopReason
import com.example.sosapp.data.TriggerSource
import com.example.sosapp.service.SosForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SosCoordinator(
    private val context: Context,
    private val settingsStore: SosSettingsStore,
    private val sirenPlayer: SirenPlayer,
    private val flashBlinkController: FlashBlinkController,
    private val callHandler: CallHandler,
    private val locationShareHandler: LocationShareHandler,
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
        val settings = settingsStore.settings.value
        _runtimeState.value = SosRuntimeState(
            mode = SosMode.TRIGGER_DETECTED,
            lastSource = source,
            message = "SOS trigger detected. Starting emergency actions...",
        )
        appScope.launch {
            runCatching { sirenPlayer.start(settings.sirenVolumeFraction) }
            if (flashBlinkController.hasFlash()) {
                flashBlinkController.start(appScope, settings.flashBlinkMs)
            }
            val contacts = PhoneNumberValidator.parseContacts(settings.emergencyNumber)
            val primaryContact = contacts.firstOrNull().orEmpty()
            val callStatus = if (settings.testMode) {
                CallStatus.SKIPPED_TEST_MODE
            } else if (primaryContact.isBlank()) {
                CallStatus.FAILED
            } else {
                callHandler.placeDirectCall(primaryContact)
            }
            val locationShareStatus = if (settings.testMode) {
                LocationShareStatus.SKIPPED_TEST_MODE
            } else {
                locationShareHandler.sendLocationMessage(settings.emergencyNumber)
            }
            _runtimeState.value = SosRuntimeState(
                mode = SosMode.SOS_ACTIVE,
                sirenActive = true,
                flashActive = flashBlinkController.hasFlash(),
                callStatus = callStatus,
                locationShareStatus = locationShareStatus,
                lastSource = source,
                message = buildStatusMessage(callStatus, locationShareStatus),
            )
            startForegroundService()
            launchMainActivity()
        }
    }

    fun stopSos(reason: StopReason) {
        appScope.launch {
            sirenPlayer.stop()
            flashBlinkController.stop()
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
                delay(settingsStore.settings.value.cooldownMs)
                setArmed(settingsStore.settings.value.enabled)
            }
            startForegroundService()
        }
    }

    private fun startForegroundService() {
        val intent = Intent(context, SosForegroundService::class.java).apply {
            action = SosForegroundService.ACTION_SYNC_NOTIFICATION
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun launchMainActivity() {
        val intent = MainActivity.createLaunchIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    }

    private fun buildStatusMessage(
        callStatus: CallStatus,
        locationShareStatus: LocationShareStatus,
    ): String {
        val callMessage = when (callStatus) {
            CallStatus.SKIPPED_TEST_MODE -> "Test mode active: emergency call skipped."
            CallStatus.PLACED_DIRECT_CALL -> "Emergency call placed."
            CallStatus.OPENED_DIALER_FALLBACK -> "Direct call failed, so the dialer was opened instead."
            CallStatus.FAILED -> "Emergency call could not be started."
            CallStatus.IDLE -> "Emergency call status is idle."
        }
        val locationMessage = when (locationShareStatus) {
            LocationShareStatus.SKIPPED_TEST_MODE -> "Location SMS skipped in test mode."
            LocationShareStatus.SENT -> "Location SMS sent to all emergency contacts."
            LocationShareStatus.PERMISSION_DENIED -> "Location SMS could not be sent because SMS or location permission is missing."
            LocationShareStatus.LOCATION_UNAVAILABLE -> "Location SMS could not be sent because no recent location was available."
            LocationShareStatus.NO_CONTACTS -> "No emergency contacts were available for location sharing."
            LocationShareStatus.FAILED -> "Location SMS failed."
            LocationShareStatus.IDLE -> "Location sharing is idle."
        }
        return "$callMessage $locationMessage Siren and flashlight are active."
    }
}
