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
import com.bih.mangosos.MainActivity
import com.bih.mangosos.SosApplication
import com.bih.mangosos.data.SosMode
import com.bih.mangosos.data.TriggerSource
import com.bih.mangosos.domain.TriggerDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Provides the hardware volume-button SOS trigger and automates WhatsApp sending.
 */
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
            if (whatsappSentForCurrentSos) {
                Log.d("SosAccessibilityService", "SOS finished, resetting WhatsApp state")
                whatsappSentForCurrentSos = false
            }
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
            Log.d("SosAccessibilityService", "Starting WhatsApp auto-click loop")
            repeat(80) { attempt -> 
                val root = rootInActiveWindow
                if (root == null) {
                    delay(300)
                    return@repeat
                }

                if (findAndClickSendButton(root)) {
                    Log.d("SosAccessibilityService", "WhatsApp send button clicked via ID/Text search")
                    finishWhatsAppAutomationForCurrentSos()
                    return@launch
                }

                if (findAndClickIntermediateButton(root)) {
                    Log.d("SosAccessibilityService", "WhatsApp intermediate button clicked")
                    delay(800) 
                    return@repeat
                }
                
                if (attempt >= 2 && findAndClickBottomRightSendButton(root)) {
                    Log.d("SosAccessibilityService", "WhatsApp send button clicked via bottom-right heuristic")
                    finishWhatsAppAutomationForCurrentSos()
                    return@launch
                }
                
                if (tapWhatsAppMediaSendFallback(root, allowWithoutPreviewSignal = attempt >= 10)) {
                    Log.d("SosAccessibilityService", "WhatsApp send button triggered via fallback tap")
                    finishWhatsAppAutomationForCurrentSos()
                    return@launch
                }
                
                delay(300)
            }
        }
    }

    private fun findAndClickSendButton(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        val sendButtonIds = listOf(
            "com.whatsapp:id/send", "com.whatsapp.w4b:id/send",
            "com.whatsapp:id/fab", "com.whatsapp.w4b:id/fab",
            "com.whatsapp:id/composer_send_button", "com.whatsapp.w4b:id/composer_send_button",
            "com.whatsapp:id/send_media_btn", "com.whatsapp.w4b:id/send_media_btn",
            "com.whatsapp:id/media_send", "com.whatsapp.w4b:id/media_send",
            "com.whatsapp:id/send_button", "com.whatsapp.w4b:id/send_button",
            "com.whatsapp:id/menu_item_send", "com.whatsapp.w4b:id/menu_item_send",
            "com.whatsapp:id/confirm_button", "com.whatsapp.w4b:id/confirm_button",
        )

        for (id in sendButtonIds) {
            val nodes = node.findAccessibilityNodeInfosByViewId(id)
            for (sendNode in nodes) {
                if (clickNodeOrClickableParent(sendNode)) return true
            }
        }

        val buttonTexts = listOf("Send", "שלח", "שליחה", "אישור", "אוקיי", "Enviar", "Envoyer")
        for (text in buttonTexts) {
            val nodes = node.findAccessibilityNodeInfosByText(text)
            for (sendNode in nodes) {
                if (clickNodeOrClickableParent(sendNode)) return true
            }
        }

        for (i in 0 until node.childCount) {
            if (findAndClickSendButton(node.getChild(i))) return true
        }
        return false
    }

    private fun findAndClickIntermediateButton(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val ids = listOf(
            "com.whatsapp:id/next_button", "com.whatsapp.w4b:id/next_button", 
            "com.whatsapp:id/done", "com.whatsapp.w4b:id/done",
            "com.whatsapp:id/ok", "com.whatsapp.w4b:id/ok"
        )
        for (id in ids) {
            val nodes = node.findAccessibilityNodeInfosByViewId(id)
            for (n in nodes) if (clickNodeOrClickableParent(n)) return true
        }

        val texts = listOf("Next", "OK", "Done", "הבא", "אישור", "אוקיי", "Siguiente", "Aceptar", "Suivant")
        for (text in texts) {
            val nodes = node.findAccessibilityNodeInfosByText(text)
            for (n in nodes) if (clickNodeOrClickableParent(n)) return true
        }

        for (i in 0 until node.childCount) if (findAndClickIntermediateButton(node.getChild(i))) return true
        return false
    }

    private fun clickNodeOrClickableParent(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        repeat(10) { 
            val candidate = current ?: return false
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
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val looksLikeBottomRightSend = bounds.width() in 40..450 &&
            bounds.height() in 40..450 &&
            bounds.exactCenterX() >= displayMetrics.widthPixels * 0.60f &&
            bounds.exactCenterY() >= displayMetrics.heightPixels * 0.65f

        if (node.isVisibleToUser && looksLikeBottomRightSend) {
            if (clickNodeOrClickableParent(node)) return true
        }

        for (i in 0 until node.childCount) {
            if (findAndClickBottomRightSendButton(node.getChild(i))) return true
        }
        return false
    }

    private fun tapWhatsAppMediaSendFallback(root: AccessibilityNodeInfo?, allowWithoutPreviewSignal: Boolean): Boolean {
        if (root == null && !allowWithoutPreviewSignal) return false
        if (root != null && !allowWithoutPreviewSignal && !looksLikeWhatsAppMediaPreview(root)) return false

        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        
        val path = Path().apply { moveTo(width * 0.915f, height * 0.902f) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 120L))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun looksLikeWhatsAppMediaPreview(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val label = listOfNotNull(node.text, node.contentDescription).joinToString(" ").lowercase()
        if (label.contains("filters") || label.contains("caption") || label.contains("החלק") || label.contains("מסננים") || label.contains("כתובית") || label.contains("כיתוב")) return true
        for (i in 0 until node.childCount) if (looksLikeWhatsAppMediaPreview(node.getChild(i))) return true
        return false
    }

    private fun finishWhatsAppAutomationForCurrentSos() {
        whatsappSentForCurrentSos = true
        serviceScope.launch {
            delay(1500)
            val intent = MainActivity.createLaunchIntent(this@SosAccessibilityService).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        chordHoldJob?.cancel()
        detector.reset()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        detector.reset()
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val appContainer = (application as SosApplication).appContainer
        val settings = appContainer.settingsStore.settings.value

        fun triggerSos() {
            Log.d("SosAccessibilityService", "Trigger detected!")
            appContainer.sosCoordinator.startSos(TriggerSource.HARDWARE_BUTTONS)
        }

        val consumed = detector.processKeyEvent(event, settings, onTrigger = ::triggerSos)
        if (event.action == KeyEvent.ACTION_DOWN && detector.isChordHeld()) {
            if (chordHoldJob?.isActive != true) {
                chordHoldJob = serviceScope.launch {
                    delay(settings.triggerHoldMs)
                    detector.triggerIfChordHeld(appContainer.settingsStore.settings.value, ::triggerSos)
                }
            }
        } else if (event.action == KeyEvent.ACTION_UP && !detector.isChordHeld()) {
            chordHoldJob?.cancel()
            chordHoldJob = null
        }
        return consumed
    }
}
