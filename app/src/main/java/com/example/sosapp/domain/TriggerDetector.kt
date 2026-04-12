package com.example.sosapp.domain

import android.view.KeyEvent
import com.example.sosapp.data.SosSettings

class TriggerDetector {
    private var upDownAt: Long? = null
    private var downDownAt: Long? = null
    private var upHeld = false
    private var downHeld = false
    private var triggerLatched = false
    private var cooldownUntil = 0L

    fun onKeyEvent(event: KeyEvent, settings: SosSettings, now: Long = System.currentTimeMillis()): Boolean {
        if (now < cooldownUntil || !settings.enabled) return false

        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> updateState(isUp = true, action = event.action, now = now)
            KeyEvent.KEYCODE_VOLUME_DOWN -> updateState(isUp = false, action = event.action, now = now)
            else -> return false
        }

        if (!upHeld || !downHeld) {
            if (event.action == KeyEvent.ACTION_UP) {
                triggerLatched = false
            }
            return false
        }

        val firstPressedAt = minOf(upDownAt ?: Long.MAX_VALUE, downDownAt ?: Long.MAX_VALUE)
        val lastPressedAt = maxOf(upDownAt ?: Long.MIN_VALUE, downDownAt ?: Long.MIN_VALUE)
        val withinChordWindow = lastPressedAt - firstPressedAt <= settings.chordWindowMs
        val heldLongEnough = now - firstPressedAt >= settings.triggerHoldMs

        if (withinChordWindow && heldLongEnough && !triggerLatched) {
            triggerLatched = true
            cooldownUntil = now + settings.cooldownMs
            return true
        }
        return false
    }

    fun reset() {
        upDownAt = null
        downDownAt = null
        upHeld = false
        downHeld = false
        triggerLatched = false
        cooldownUntil = 0L
    }

    private fun updateState(isUp: Boolean, action: Int, now: Long) {
        when {
            isUp && action == KeyEvent.ACTION_DOWN -> {
                if (!upHeld) upDownAt = now
                upHeld = true
            }
            !isUp && action == KeyEvent.ACTION_DOWN -> {
                if (!downHeld) downDownAt = now
                downHeld = true
            }
            isUp && action == KeyEvent.ACTION_UP -> {
                upHeld = false
                upDownAt = null
            }
            !isUp && action == KeyEvent.ACTION_UP -> {
                downHeld = false
                downDownAt = null
            }
        }
    }
}
