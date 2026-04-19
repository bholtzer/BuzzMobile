package com.example.sosapp.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.example.sosapp.SosApplication
import com.example.sosapp.data.TriggerSource
import com.example.sosapp.domain.TriggerDetector

class SosAccessibilityService : AccessibilityService() {
    private val detector = TriggerDetector()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val appContainer = (application as SosApplication).appContainer
        val settings = appContainer.settingsStore.settings.value
        
        // Use the improved processKeyEvent logic
        return detector.processKeyEvent(event, settings) {
            Log.d("SosAccessibilityService", "Trigger detected! Starting SOS...")
            appContainer.sosCoordinator.startSos(TriggerSource.HARDWARE_BUTTONS)
        }
    }
}
