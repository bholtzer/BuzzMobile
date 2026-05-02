package com.bih.mangosos.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.bih.mangosos.MainActivity
import com.bih.mangosos.SosApplication
import com.bih.mangosos.domain.TriggerDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SosAccessibilityService : AccessibilityService() {
    private val detector = TriggerDetector()
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val appContainer = (application as SosApplication).appContainer
        val runtimeState = appContainer.sosCoordinator.runtimeState.value
        
        if (runtimeState.mode == com.bih.mangosos.data.SosMode.SOS_ACTIVE ||
            runtimeState.mode == com.bih.mangosos.data.SosMode.TRIGGER_DETECTED) {
            
            val packageName = event.packageName?.toString()
            if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
                findAndClickSendButton(rootInActiveWindow)
            }
        }
    }

    private fun findAndClickSendButton(node: AccessibilityNodeInfo?) {
        if (node == null) return
        
        val sendButtonIds = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp.w4b:id/send",
            "com.whatsapp:id/confirm_button",
            "com.whatsapp.w4b:id/confirm_button",
            "com.whatsapp:id/done",
            "com.whatsapp.w4b:id/done"
        )
        
        for (id in sendButtonIds) {
            val nodes = node.findAccessibilityNodeInfosByViewId(id)
            for (sendNode in nodes) {
                if (sendNode.isClickable && sendNode.isEnabled) {
                    if (sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        autoReturn()
                        return
                    }
                }
            }
        }

        val descriptions = listOf("Send", "שליחה", "שלח")
        for (desc in descriptions) {
            val nodes = node.findAccessibilityNodeInfosByText(desc)
            for (sendNode in nodes) {
                if (sendNode.isClickable && sendNode.isEnabled) {
                    if (sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        autoReturn()
                        return
                    }
                }
            }
        }

        for (i in 0 until node.childCount) {
            findAndClickSendButton(node.getChild(i))
        }
    }

    private fun autoReturn() {
        serviceScope.launch {
            delay(800)
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val appContainer = (application as SosApplication).appContainer
        val settings = appContainer.settingsStore.settings.value
        
        return detector.processKeyEvent(event, settings) {
            Log.d("SosAccessibilityService", "Trigger detected! Opening app and starting SOS...")
            val launchIntent = MainActivity.createLaunchIntent(
                context = this,
                triggerSos = true,
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(launchIntent)
        }
    }
}
