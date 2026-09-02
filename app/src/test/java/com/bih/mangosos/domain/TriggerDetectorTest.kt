package com.bih.mangosos.domain

import android.view.KeyEvent
import com.bih.mangosos.data.SosSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerDetectorTest {
    private val settings = SosSettings(
        emergencyNumber = "112",
        enabled = true,
        accessibilityConsentGranted = true,
        triggerHoldMs = 1000L,
        cooldownMs = 5000L,
    )

    @Test
    fun refusesHardwareKeysWithoutDisclosureConsent() {
        val detector = TriggerDetector()
        val noConsent = settings.copy(accessibilityConsentGranted = false)
        val onTrigger = { throw AssertionError("SOS must not start without consent") }
        assertFalse(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, noConsent, 0L, onTrigger))
        assertFalse(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, noConsent, 2000L, onTrigger))
        assertFalse(detector.triggerIfChordHeld(noConsent, onTrigger))
    }

    @Test
    fun pendingHoldCannotTriggerAfterConsentIsRemoved() {
        val detector = TriggerDetector()
        val onTrigger = { throw AssertionError("Revoked consent must block pending SOS") }
        detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, settings, 0L, onTrigger)
        detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, settings, 100L, onTrigger)
        assertFalse(detector.triggerIfChordHeld(settings.copy(accessibilityConsentGranted = false), onTrigger))
    }

    @Test
    fun triggersWhenBothButtonsOverlapLongEnough() {
        val detector = TriggerDetector()
        var triggered = false
        val onTrigger = { triggered = true }
        
        assertFalse(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, settings, 0L, onTrigger))
        assertTrue(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, settings, 200L, onTrigger))
        assertFalse(triggered)
        // At 1200ms both buttons have been held together for 1000ms.
        assertTrue(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, settings, 1200L, onTrigger))
        assertTrue(triggered)
    }

    @Test
    fun delayedCheckTriggersWhenBothButtonsRemainHeldWithoutRepeatEvents() {
        val detector = TriggerDetector()
        var triggered = false
        val onTrigger = { triggered = true }

        assertFalse(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, settings, 0L, onTrigger))
        assertTrue(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, settings, 200L, onTrigger))
        assertTrue(detector.isChordHeld())
        assertTrue(detector.triggerIfChordHeld(settings, onTrigger))
        assertTrue(triggered)
    }

    @Test
    fun completedOnboardingStillAllowsHardwareTriggerWhenHiddenEnabledFlagIsOff() {
        val detector = TriggerDetector()
        var triggered = false
        val onTrigger = { triggered = true }
        val completedSettings = settings.copy(enabled = false, onboardingSeen = true)

        assertFalse(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, completedSettings, 0L, onTrigger))
        assertTrue(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, completedSettings, 200L, onTrigger))
        assertTrue(detector.triggerIfChordHeld(completedSettings, onTrigger))
        assertTrue(triggered)
    }

    @Test
    fun doesNotTriggerSequentialPressesThatAreNotHeldTogether() {
        val detector = TriggerDetector()
        var triggered = false
        val onTrigger = { triggered = true }

        assertFalse(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, settings, 0L, onTrigger))
        assertFalse(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_UP, settings, 300L, onTrigger))
        assertFalse(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, settings, 400L, onTrigger))
        assertFalse(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, settings, 1400L, onTrigger))
        assertFalse(triggered)
    }

    @Test
    fun resetsHoldTimerWhenEitherButtonIsReleased() {
        val detector = TriggerDetector()
        var triggered = false
        val onTrigger = { triggered = true }

        assertFalse(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, settings, 0L, onTrigger))
        assertTrue(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.ACTION_DOWN, settings, 100L, onTrigger))
        assertFalse(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_UP, settings, 500L, onTrigger))
        assertTrue(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, settings, 700L, onTrigger))
        assertTrue(detector.processVolumeKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.ACTION_DOWN, settings, 1400L, onTrigger))
        assertFalse(triggered)
    }
}
