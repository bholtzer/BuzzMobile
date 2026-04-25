package com.bih.sosguardian.domain

import android.util.Log
import android.view.KeyEvent
import com.bih.sosguardian.data.SosSettings
import com.bih.sosguardian.data.TriggerType

class TriggerDetector {
    private var upDownAt: Long? = null
    private var downDownAt: Long? = null
    private var upHeld = false
    private var downHeld = false
    private var triggerLatched = false
    private var cooldownUntil = 0L

    /**
     * Returns true if the event should be consumed (preventing system volume change).
     * The [onTrigger] callback is invoked when the SOS condition is met.
     */
    fun processKeyEvent(
        event: KeyEvent,
        settings: SosSettings,
        now: Long = System.currentTimeMillis(),
        onTrigger: () -> Unit
    ): Boolean {
        // If not enabled or in cooldown, don't interfere with volume keys
        // Removed now < cooldownUntil check to allow re-trigger if needed, 
        // though triggerLatched still prevents spam.
        if (!settings.enabled) {
            return false
        }

        val isVolumeUp = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
        val isVolumeDown = event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN

        if (!isVolumeUp && !isVolumeDown) return false

        val action = event.action
        
        updateInternalState(isVolumeUp, action, now)

        val triggered = checkTriggerCondition(settings, now)

        if (triggered && !triggerLatched) {
            triggerLatched = true
            cooldownUntil = now + settings.cooldownMs
            Log.d("TriggerDetector", "SOS Triggered via ${settings.triggerType}")
            onTrigger()
            return true
        }

        // Reset latch when buttons are released
        if (action == KeyEvent.ACTION_UP) {
            if (settings.triggerType == TriggerType.VOLUME_CHORD) {
                // For chord, reset if either button is released to allow fresh trigger
                if (!upHeld || !downHeld) triggerLatched = false
            } else {
                triggerLatched = false
            }
        }

        // Consume events to prevent volume UI and beeping
        return true
    }

    private fun updateInternalState(isUp: Boolean, action: Int, now: Long) {
        if (isUp) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (!upHeld) {
                    upDownAt = now
                    upHeld = true
                }
            } else if (action == KeyEvent.ACTION_UP) {
                upHeld = false
                upDownAt = null
            }
        } else {
            if (action == KeyEvent.ACTION_DOWN) {
                if (!downHeld) {
                    downDownAt = now
                    downHeld = true
                }
            } else if (action == KeyEvent.ACTION_UP) {
                downHeld = false
                downDownAt = null
            }
        }
    }

    private fun checkTriggerCondition(settings: SosSettings, now: Long): Boolean {
        return when (settings.triggerType) {
            TriggerType.VOLUME_CHORD -> {
                if (upHeld && downHeld) {
                    val firstPressedAt = minOf(upDownAt ?: Long.MAX_VALUE, downDownAt ?: Long.MAX_VALUE)
                    val lastPressedAt = maxOf(upDownAt ?: Long.MIN_VALUE, downDownAt ?: Long.MIN_VALUE)
                    
                    val withinChordWindow = (lastPressedAt - firstPressedAt) <= settings.chordWindowMs
                    val heldLongEnough = (now - lastPressedAt) >= settings.triggerHoldMs
                    
                    withinChordWindow && heldLongEnough
                } else false
            }
            TriggerType.VOL_UP_LONG_PRESS -> {
                if (upHeld && !downHeld) {
                    val pressedAt = upDownAt ?: now
                    (now - pressedAt) >= settings.triggerHoldMs
                } else false
            }
            TriggerType.VOL_DOWN_LONG_PRESS -> {
                if (downHeld && !upHeld) {
                    val pressedAt = downDownAt ?: now
                    (now - pressedAt) >= settings.triggerHoldMs
                } else false
            }
        }
    }

    fun reset() {
        upDownAt = null
        downDownAt = null
        upHeld = false
        downHeld = false
        triggerLatched = false
        cooldownUntil = 0L
    }
}
