package com.bih.mangosos.domain

import android.view.KeyEvent
import com.bih.mangosos.data.SosSettings

class TriggerDetector {
    private var upHeld = false
    private var downHeld = false
    private var chordStartedAt = 0L
    private var triggerLatched = false

    fun processKeyEvent(
        event: KeyEvent,
        settings: SosSettings,
        now: Long = System.currentTimeMillis(),
        onTrigger: () -> Unit
    ): Boolean {
        return processVolumeKeyEvent(
            keyCode = event.keyCode,
            action = event.action,
            settings = settings,
            now = now,
            onTrigger = onTrigger,
        )
    }

    fun processVolumeKeyEvent(
        keyCode: Int,
        action: Int,
        settings: SosSettings,
        now: Long = System.currentTimeMillis(),
        onTrigger: () -> Unit
    ): Boolean {
        if (!settings.enabled) return false

        val isVolumeUp = keyCode == KeyEvent.KEYCODE_VOLUME_UP
        val isVolumeDown = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolumeUp && !isVolumeDown) return false
        
        if (action == KeyEvent.ACTION_DOWN) {
            if (isVolumeUp) {
                upHeld = true
            } else {
                downHeld = true
            }

            if (upHeld && downHeld && chordStartedAt == 0L) {
                chordStartedAt = now
            }

            val heldTogetherMs = if (upHeld && downHeld) now - chordStartedAt else 0L
            if (heldTogetherMs >= settings.triggerHoldMs && !triggerLatched) {
                triggerLatched = true
                onTrigger()
            }
        } else if (action == KeyEvent.ACTION_UP) {
            if (isVolumeUp) upHeld = false else downHeld = false
            if (!upHeld || !downHeld) {
                chordStartedAt = 0L
            }
            if (!upHeld && !downHeld) {
                triggerLatched = false
            }
        }

        // Only consume once both buttons are actively held together or SOS has triggered.
        return (upHeld && downHeld) || triggerLatched
    }

    fun reset() {
        upHeld = false
        downHeld = false
        chordStartedAt = 0L
        triggerLatched = false
    }
}
