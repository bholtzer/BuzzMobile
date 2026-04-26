package com.bih.sosguardian.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bih.sosguardian.MainActivity
import com.bih.sosguardian.data.CallStatus
import com.bih.sosguardian.data.LocationShareStatus
import com.bih.sosguardian.data.SosMode
import com.bih.sosguardian.data.SosRuntimeState
import com.bih.sosguardian.data.SosSettingsStore
import com.bih.sosguardian.data.StopReason
import com.bih.sosguardian.data.TriggerSource
import com.bih.sosguardian.service.SosForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SosCoordinator(
    private val context: Context,
    private val settingsStore: SosSettingsStore,
    private val sirenPlayer: SirenPlayer,
    private val flashBlinkController: FlashBlinkController,
    private val callHandler: CallHandler,
    private val locationShareHandler: LocationShareHandler,
    private val cameraHandler: CameraHandler,
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

    fun startSos(source: TriggerSource, forceTestMode: Boolean = false) {
        if (_runtimeState.value.mode == SosMode.SOS_ACTIVE) return
        val settings = settingsStore.settings.value
        val isTestMode = forceTestMode || settings.testMode
        
        // 1. Immediate State Change & Tactile Feedback
        _runtimeState.value = SosRuntimeState(
            mode = SosMode.TRIGGER_DETECTED,
            lastSource = source,
            message = "SOS trigger detected! Starting emergency actions...",
        )
        vibrate()

        appScope.launch {
            // 2. Immediate UI launch before any slow background tasks (like Camera)
            startForegroundService()
            launchMainActivity()
            
            // Start siren as fast as possible
            runCatching { sirenPlayer.start(settings.sirenVolumeFraction) }
            
            // 3. Parallelize flash and photo capture
            if (flashBlinkController.hasFlash()) {
                flashBlinkController.start(appScope, settings.flashBlinkMs)
            }
            
            // Capture photo (this is usually the slowest part)
            val photoFile = cameraHandler.capturePhoto()
            
            val contacts = PhoneNumberValidator.parseContacts(settings.emergencyNumber)
            val primaryContact = contacts.firstOrNull().orEmpty()
            
            val callStatus = if (isTestMode) {
                CallStatus.SKIPPED_TEST_MODE
            } else if (primaryContact.isBlank()) {
                CallStatus.FAILED
            } else {
                callHandler.placeDirectCall(primaryContact)
            }
            
            val locationShareStatus = if (isTestMode) {
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
            
            // Refresh service notification with final status
            startForegroundService()
            
            if (!isTestMode && settings.whatsappNumber.isNotBlank()) {
                sendWhatsAppAlert(settings.whatsappNumber, photoFile)
            }
        }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    private fun sendWhatsAppAlert(whatsappNumber: String, photoFile: File?) {
        val pkg = when {
            isAppInstalled("com.whatsapp") -> "com.whatsapp"
            isAppInstalled("com.whatsapp.w4b") -> "com.whatsapp.w4b"
            else -> return
        }

        val lastLoc = locationShareHandler.getBestLastKnownLocation()
        val message = buildString {
            append("SOS EMERGENCY ALERT! I need help immediately. ")
            if (lastLoc != null) {
                append("\nMy location: https://maps.google.com/?q=${lastLoc.latitude},${lastLoc.longitude}")
            }
        }
        
        var digits = whatsappNumber.filter { it.isDigit() }
        if (digits.startsWith("0") && digits.length == 10) {
            digits = "972" + digits.substring(1)
        } else if (digits.length == 9 && digits.startsWith("5")) {
            digits = "972" + digits
        } else if (digits.length == 10 && !digits.startsWith("972")) {
            digits = "972" + digits
        }
        
        val jid = "$digits@s.whatsapp.net"

        val intent = if (photoFile != null) {
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra("jid", jid)
                putExtra("com.whatsapp.EXTRA_JID", jid)
                putExtra("com.whatsapp.contact.ID", jid)
                putExtra("address", digits)
                setPackage(pkg)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
            Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(pkg)
            }
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("SosCoordinator", "Failed to launch WhatsApp", e)
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: Exception) {
            false
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
            action = SosForegroundService.Companion.ACTION_SYNC_NOTIFICATION
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun launchMainActivity() {
        val intent = MainActivity.Companion.createLaunchIntent(context).apply {
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
