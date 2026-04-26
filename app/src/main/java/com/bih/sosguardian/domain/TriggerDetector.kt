package com.bih.sosguardian.domain

import android.util.Log
import android.view.KeyEvent
import com.bih.sosguardian.data.SosSettings

class TriggerDetector {
    private var lastUpPress = 0L
    private var lastDownPress = 0L
    private var upHeld = false
    private var downHeld = false
    private var triggerLatched = false

    fun processKeyEvent(
        event: KeyEvent,
        settings: SosSettings,
        now: Long = System.currentTimeMillis(),
        onTrigger: () -> Unit
    ): Boolean {
        if (!settings.enabled) return false

        val isVolumeUp = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
        val isVolumeDown = event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolumeUp && !isVolumeDown) return false

        val action = event.action
        
        if (action == KeyEvent.ACTION_DOWN) {
            if (isVolumeUp) {
                lastUpPress = now
                upHeld = true
            } else {
                lastDownPress = now
                downHeld = true
            }

            // TRIGGER CONDITION: If both were pressed within the chord window
            val diff = kotlin.math.abs(lastUpPress - lastDownPress)
            if (diff <= settings.chordWindowMs && !triggerLatched) {
                triggerLatched = true
                Log.d("TriggerDetector", "QUICK PRESS SOS TRIGGERED (diff: $diff ms)")
                onTrigger()
            }
        } else if (action == KeyEvent.ACTION_UP) {
            if (isVolumeUp) upHeld = false else downHeld = false
            // Reset latch when both are released
            if (!upHeld && !downHeld) {
                triggerLatched = false
            }
        }

        // Only consume once the actual SOS chord is in progress or has triggered.
        return (upHeld && downHeld) || triggerLatched
    }

    fun reset() {
        upHeld = false
        downHeld = false
        triggerLatched = false
        lastUpPress = 0L
        lastDownPress = 0L
    }
}
