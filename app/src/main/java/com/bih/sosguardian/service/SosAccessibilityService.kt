package com.bih.sosguardian.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.bih.sosguardian.SosApplication
import com.bih.sosguardian.data.TriggerSource
import com.bih.sosguardian.domain.TriggerDetector

class SosAccessibilityService : AccessibilityService() {
    private val detector = TriggerDetector()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Automatically click the send button in WhatsApp during an SOS event
        val appContainer = (application as SosApplication).appContainer
        val runtimeState = appContainer.sosCoordinator.runtimeState.value
        
        if (runtimeState.mode == com.bih.sosguardian.data.SosMode.SOS_ACTIVE || 
            runtimeState.mode == com.bih.sosguardian.data.SosMode.TRIGGER_DETECTED) {
            
            val packageName = event.packageName?.toString()
            if (packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b") {
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                    event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                    findAndClickSendButton(rootInActiveWindow)
                }
            }
        }
    }

    private fun findAndClickSendButton(node: AccessibilityNodeInfo?) {
        if (node == null) return
        
        // WhatsApp's send button usually has a content description like "Send"
        // In Hebrew, it might be "שליחה" or similar. We check common IDs and descriptions.
        val sendButtonIds = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp.w4b:id/send"
        )
        
        for (id in sendButtonIds) {
            val nodes = node.findAccessibilityNodeInfosByViewId(id)
            for (sendNode in nodes) {
                if (sendNode.isEnabled && sendNode.isClickable) {
                    sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
            }
        }

        // Fallback to searching by content description if ID fails
        val descriptions = listOf("Send", "שליחה")
        for (desc in descriptions) {
            val nodes = node.findAccessibilityNodeInfosByText(desc)
            for (sendNode in nodes) {
                if (sendNode.isClickable && sendNode.isEnabled) {
                    sendNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
            }
        }

        // Recursive search
        for (i in 0 until node.childCount) {
            findAndClickSendButton(node.getChild(i))
        }
    }

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val appContainer = (application as SosApplication).appContainer
        val settings = appContainer.settingsStore.settings.value
        
        return detector.processKeyEvent(event, settings) {
            Log.d("SosAccessibilityService", "Trigger detected! Starting SOS...")
            appContainer.sosCoordinator.startSos(TriggerSource.HARDWARE_BUTTONS)
        }
    }
}
