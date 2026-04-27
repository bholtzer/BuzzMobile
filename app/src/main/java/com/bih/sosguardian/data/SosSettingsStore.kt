package com.bih.sosguardian.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore by preferencesDataStore(name = "sos_settings")

class SosSettingsStore(
    val context: Context,
    appScope: CoroutineScope,
) {
    private object Keys {
        val emergencyNumber = stringPreferencesKey("emergency_number")
        val whatsappNumber = stringPreferencesKey("whatsapp_number")
        val enabled = booleanPreferencesKey("enabled")
        val onboardingSeen = booleanPreferencesKey("onboarding_seen")
        val sirenVolumeFraction = floatPreferencesKey("siren_volume_fraction")
        val triggerType = stringPreferencesKey("trigger_type")
        val triggerHoldMs = longPreferencesKey("trigger_hold_ms")
        val chordWindowMs = longPreferencesKey("chord_window_ms")
        val flashBlinkMs = longPreferencesKey("flash_blink_ms")
        val cooldownMs = longPreferencesKey("cooldown_ms")
        val testMode = booleanPreferencesKey("test_mode")
    }

    val settings: StateFlow<SosSettings> = context.dataStore.data
        .map(::mapPreferences)
        .stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            initialValue = SosSettings(),
        )

    suspend fun updateSettings(transform: (SosSettings) -> SosSettings) {
        context.dataStore.edit { prefs ->
            val updated = transform(mapPreferences(prefs))
            prefs[Keys.emergencyNumber] = updated.emergencyNumber
            prefs[Keys.whatsappNumber] = updated.whatsappNumber
            prefs[Keys.enabled] = updated.enabled
            prefs[Keys.onboardingSeen] = updated.onboardingSeen
            prefs[Keys.sirenVolumeFraction] = updated.sirenVolumeFraction
            prefs[Keys.triggerType] = updated.triggerType.name
            prefs[Keys.triggerHoldMs] = updated.triggerHoldMs
            prefs[Keys.chordWindowMs] = updated.chordWindowMs
            prefs[Keys.flashBlinkMs] = updated.flashBlinkMs
            prefs[Keys.cooldownMs] = updated.cooldownMs
            prefs[Keys.testMode] = updated.testMode
        }
    }

    private fun mapPreferences(preferences: Preferences): SosSettings {
        return SosSettings(
            emergencyNumber = preferences[Keys.emergencyNumber].orEmpty(),
            whatsappNumber = preferences[Keys.whatsappNumber].orEmpty(),
            enabled = preferences[Keys.enabled] ?: false,
            onboardingSeen = preferences[Keys.onboardingSeen] ?: false,
            sirenVolumeFraction = preferences[Keys.sirenVolumeFraction] ?: 1f,
            triggerType = try {
                TriggerType.valueOf(preferences[Keys.triggerType] ?: TriggerType.VOLUME_CHORD.name)
            } catch (e: Exception) {
                TriggerType.VOLUME_CHORD
            },
            triggerHoldMs = preferences[Keys.triggerHoldMs] ?: 500L,
            chordWindowMs = preferences[Keys.chordWindowMs] ?: 600L,
            flashBlinkMs = preferences[Keys.flashBlinkMs] ?: 350L,
            cooldownMs = preferences[Keys.cooldownMs] ?: 5000L,
            testMode = preferences[Keys.testMode] ?: true,
        )
    }
}
