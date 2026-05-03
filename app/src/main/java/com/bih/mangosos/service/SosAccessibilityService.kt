package com.bih.mangosos.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.bih.mangosos.MainActivity
import com.bih.mangosos.R
import com.bih.mangosos.SosApplication
import com.bih.mangosos.data.SosMode
import com.bih.mangosos.data.TriggerSource
import com.bih.mangosos.domain.TriggerDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SosAccessibilityService : AccessibilityService() {
    private val detector = TriggerDetector()
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var chordHoldJob: Job? = null
    private var whatsappClickJob: Job? = null
    private var whatsappSentForCurrentSos = false

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val appContainer = (application as SosApplication).appContainer
        val runtimeState = appContainer.sosCoordinator.runtimeState.value

        val sosActive = runtimeState.mode == SosMode.SOS_ACTIVE || runtimeState.mode == SosMode.TRIGGER_DETECTED
        if (!sosActive) {
            whatsappSentForCurrentSos = false
            whatsappClickJob?.cancel()
            whatsappClickJob = null
            return
        }

        if (whatsappSentForCurrentSos) return

        val packageName = event.packageName?.toString()
        if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
            scheduleWhatsAppAutoClick()
        }
    }

    private fun scheduleWhatsAppAutoClick() {
        if (whatsappClickJob?.isActive == true) return
        whatsappClickJob = serviceScope.launch {
            repeat(60) { attempt ->
                val root = rootInActiveWindow
                if (findAndClickSendButton(root)) {
                    Log.d("SosAccessibilityService", "WhatsApp send button clicked via ID/Text search")
                    finishWhatsAppAutomationForCurrentSos()
                    return@launch
                }
                if (attempt >= 1 && findAndClickBottomRightSendButton(root)) {
                    Log.d("SosAccessibilityService", "WhatsApp send button clicked via bottom-right heuristic")
                    finishWhatsAppAutomationForCurrentSos()
                    return@launch
                }
                if (tapWhatsAppMediaSendFallback(root, allowWithoutPreviewSignal = attempt >= 2)) {
                    Log.d("SosAccessibilityService", "WhatsApp send button triggered via fallback tap")
                    finishWhatsAppAutomationForCurrentSos()
                    return@launch
                }
                delay(100)
            }
        }
    }

    private fun findAndClickSendButton(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val sendButtonIds = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp.w4b:id/send",
            "com.whatsapp:id/fab",
            "com.whatsapp.w4b:id/fab",
            "com.whatsapp:id/composer_send_button",
            "com.whatsapp.w4b:id/composer_send_button",
            "com.whatsapp:id/send_media_btn",
            "com.whatsapp.w4b:id/send_media_btn",
            "com.whatsapp:id/media_send",
            "com.whatsapp.w4b:id/media_send",
            "com.whatsapp:id/confirm_button",
            "com.whatsapp.w4b:id/confirm_button",
            "com.whatsapp:id/done",
            "com.whatsapp.w4b:id/done",
            "com.whatsapp:id/ok",
            "com.whatsapp.w4b:id/ok",
            "com.whatsapp:id/send_button",
            "com.whatsapp.w4b:id/send_button",
            "com.whatsapp:id/next_button",
            "com.whatsapp.w4b:id/next_button",
        )

        for (id in sendButtonIds) {
            val nodes = node.findAccessibilityNodeInfosByViewId(id)
            for (sendNode in nodes) {
                if (clickNodeOrClickableParent(sendNode)) {
                    return true
                }
            }
        }

        val buttonTexts = listOf("Send", "OK", "Done", "שלח", "שליחה", "אישור", "Enviar", "Envoyer")
        for (text in buttonTexts) {
            val nodes = node.findAccessibilityNodeInfosByText(text)
            for (sendNode in nodes) {
                if (clickNodeOrClickableParent(sendNode)) {
                    return true
                }
            }
        }

        if (isWhatsAppSendTarget(node) && clickNodeOrClickableParent(node)) {
            return true
        }

        for (i in 0 until node.childCount) {
            if (findAndClickSendButton(node.getChild(i))) {
                return true
            }
        }
        return false
    }

    private fun isWhatsAppSendTarget(node: AccessibilityNodeInfo): Boolean {
        val label = listOfNotNull(node.text, node.contentDescription)
            .joinToString(" ")
            .trim()
            .lowercase()
        if (label.isBlank()) return false

        val targets = listOf(
            "send",
            "ok",
            "done",
            "send media",
            "שלח",
            "שליחה",
            "אישור",
            "אוקיי",
            "enviar",
            "envoyer",
        )
        return targets.any { target -> label == target || label.contains(target) }
    }

    private fun clickNodeOrClickableParent(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        repeat(6) {
            if (current == null) return false
            val candidate = current
            if (candidate.isEnabled && candidate.isClickable) {
                return candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = candidate.parent
        }
        return false
    }

    private fun findAndClickBottomRightSendButton(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        if (screenWidth <= 0 || screenHeight <= 0) return false

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val centerX = bounds.exactCenterX()
        val centerY = bounds.exactCenterY()
        
        // WhatsApp green circle send button is usually in the bottom-right corner
        val looksLikeBottomRightSend = bounds.width() in 40..300 &&
            bounds.height() in 40..300 &&
            centerX >= screenWidth * 0.65f &&
            centerY >= screenHeight * 0.70f

        if (node.isVisibleToUser && node.isEnabled && looksLikeBottomRightSend) {
            if (clickNodeOrClickableParent(node)) {
                Log.d("SosAccessibilityService", "Clicked potential bottom-right send node at $bounds")
                return true
            }
        }

        for (i in 0 until node.childCount) {
            if (findAndClickBottomRightSendButton(node.getChild(i))) {
                return true
            }
        }
        return false
    }

    private fun tapWhatsAppMediaSendFallback(
        root: AccessibilityNodeInfo?,
        allowWithoutPreviewSignal: Boolean = false,
    ): Boolean {
        if (root == null && !allowWithoutPreviewSignal) return false
        if (root != null && !allowWithoutPreviewSignal && !looksLikeWhatsAppMediaPreview(root)) return false

        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        if (width <= 0f || height <= 0f) return false

        // Tap standard locations for the green FAB send button
        val tapPoints = listOf(
            width * 0.915f to height * 0.902f,
            width * 0.900f to height * 0.895f,
            width * 0.930f to height * 0.885f,
            width * 0.880f to height * 0.910f,
        )
        for ((tapX, tapY) in tapPoints) {
            if (tapScreen(tapX, tapY)) {
                Log.d("SosAccessibilityService", "Tapped WhatsApp media send fallback at $tapX,$tapY")
                return true
            }
        }
        return false
    }

    private fun tapScreen(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 120L))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun looksLikeWhatsAppMediaPreview(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val label = listOfNotNull(node.text, node.contentDescription)
            .joinToString(" ")
            .lowercase()
        if (
            label.contains("swipe up for filters") ||
            label.contains("filters") ||
            label.contains("caption") ||
            label.contains("החלק") ||
            label.contains("מסננים") ||
            label.contains("כיתוב") ||
            label.contains("כתובית")
        ) {
            return true
        }

        for (i in 0 until node.childCount) {
            if (looksLikeWhatsAppMediaPreview(node.getChild(i))) return true
        }
        return false
    }

    private fun finishWhatsAppAutomationForCurrentSos() {
        whatsappSentForCurrentSos = true
        notifyWhatsAppSent()
        returnToMangoGuardian()
    }

    private fun returnToMangoGuardian() {
        serviceScope.launch {
            delay(500)
            val intent = MainActivity.createLaunchIntent(this@SosAccessibilityService).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                )
            }
            startActivity(intent)
        }
    }

    private fun notifyWhatsAppSent() {
        Toast.makeText(this, getString(R.string.toast_whatsapp_sent), Toast.LENGTH_SHORT).show()
    }

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val appContainer = (application as SosApplication).appContainer
        val settings = appContainer.settingsStore.settings.value

        fun triggerSos() {
            Log.d("SosAccessibilityService", "Trigger detected! Starting SOS...")
            appContainer.sosCoordinator.startSos(TriggerSource.HARDWARE_BUTTONS)
        }

        val consumed = detector.processKeyEvent(event, settings, onTrigger = ::triggerSos)
        if (event.action == KeyEvent.ACTION_DOWN && detector.isChordHeld()) {
            if (chordHoldJob?.isActive != true) {
                chordHoldJob = serviceScope.launch {
                    delay(settings.triggerHoldMs)
                    val latestSettings = appContainer.settingsStore.settings.value
                    detector.triggerIfChordHeld(latestSettings, ::triggerSos)
                }
            }
        } else if (event.action == KeyEvent.ACTION_UP && !detector.isChordHeld()) {
            chordHoldJob?.cancel()
            chordHoldJob = null
        }

        return consumed
    }
}
