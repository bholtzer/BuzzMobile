package com.bih.sosguardian.domain

import android.view.KeyEvent
import com.bih.sosguardian.data.SosSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerDetectorTest {
    private val settings = SosSettings(
        emergencyNumber = "112",
        enabled = true,
        triggerHoldMs = 1000L,
        chordWindowMs = 500L,
        cooldownMs = 5000L,
    )

    @Test
    fun triggersWhenBothButtonsOverlapLongEnough() {
        val detector = TriggerDetector()
        var triggered = false
        val onTrigger = { triggered = true }
        
        assertFalse(detector.processKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP), settings, 0L, onTrigger))
        assertFalse(detector.processKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), settings, 200L, onTrigger))
        // At 1200ms, the last button (Down) has been held for 1000ms, and the chord (Up then Down) was within 200ms
        assertTrue(detector.processKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), settings, 1200L, onTrigger))
        assertTrue(triggered)
    }

    @Test
    fun doesNotTriggerOutsideChordWindow() {
        val detector = TriggerDetector()
        var triggered = false
        val onTrigger = { triggered = true }

        assertFalse(detector.processKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP), settings, 0L, onTrigger))
        // Down pressed at 700ms. Chord window is 500ms. (700 - 0) > 500, so this should not trigger SOS.
        assertFalse(detector.processKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), settings, 700L, onTrigger))
        assertFalse(detector.processKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), settings, 1700L, onTrigger))
        assertFalse(triggered)
    }
}
