package com.example.sosapp.domain

import android.view.KeyEvent
import com.bih.sosapp.data.SosSettings
import com.bih.sosapp.domain.TriggerDetector
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
        assertFalse(detector.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP), settings, now = 0L))
        assertFalse(detector.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), settings, now = 200L))
        assertTrue(detector.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), settings, now = 1000L))
    }

    @Test
    fun doesNotTriggerOutsideChordWindow() {
        val detector = TriggerDetector()
        assertFalse(detector.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP), settings, now = 0L))
        assertFalse(detector.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), settings, now = 700L))
        assertFalse(detector.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN), settings, now = 1200L))
    }
}
