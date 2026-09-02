package com.bih.mangosos.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.bih.mangosos.SosApplication
import com.bih.mangosos.data.TriggerSource
import com.bih.mangosos.domain.TriggerDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Provides only the user-enabled hardware volume-button SOS trigger.
 *
 * This service deliberately does not inspect window content, dispatch gestures, or interact
 * with other apps. Messages opened by Mango Guardian always require the user to press Send.
 */
class SosAccessibilityService : AccessibilityService() {
    private val detector = TriggerDetector()
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var chordHoldJob: Job? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

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
            Log.d("SosAccessibilityService", "Volume-button SOS trigger detected")
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
