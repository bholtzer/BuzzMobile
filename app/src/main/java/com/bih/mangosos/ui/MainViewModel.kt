package com.bih.mangosos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bih.mangosos.SosApplication
import com.bih.mangosos.data.SosRuntimeState
import com.bih.mangosos.data.SosSettings
import com.bih.mangosos.data.StopReason
import com.bih.mangosos.data.TriggerSource
import com.bih.mangosos.domain.PhoneNumberValidator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContainer = (application as SosApplication).appContainer

    val settings: StateFlow<SosSettings> = appContainer.settingsStore.settings
    val runtimeState: StateFlow<SosRuntimeState> = appContainer.sosCoordinator.runtimeState

    private fun trackIfAllowed(
        event: String,
        properties: Map<String, String> = emptyMap(),
    ) {
        if (settings.value.analyticsEnabled) {
            appContainer.analyticsTracker.track(event, properties)
        }
    }

    fun saveSettings(settings: SosSettings, onInvalidNumber: () -> Unit) {
        if (!PhoneNumberValidator.isValid(settings.emergencyNumber)) {
            onInvalidNumber()
            return
        }
        viewModelScope.launch {
            val savedSettings = settings.copy(enabled = true, onboardingSeen = true)
            appContainer.settingsStore.updateSettings { savedSettings }
            appContainer.sosCoordinator.setArmed(true)
            trackIfAllowed("settings_saved")
            appContainer.monitoringServiceController.start()
        }
    }

    fun setEnabled(enabled: Boolean, onInvalidNumber: () -> Unit = {}) {
        val current = settings.value
        if (enabled && !PhoneNumberValidator.isValid(current.emergencyNumber)) {
            onInvalidNumber()
            return
        }
        viewModelScope.launch {
            appContainer.settingsStore.updateSettings { it.copy(enabled = enabled) }
            appContainer.sosCoordinator.setArmed(enabled)
            trackIfAllowed("monitoring_${if (enabled) "enabled" else "disabled"}")
            if (enabled) {
                appContainer.monitoringServiceController.start()
            } else {
                appContainer.monitoringServiceController.stop()
            }
        }
    }

    fun triggerManualSos() {
        trackIfAllowed("manual_sos_pressed")
        appContainer.sosCoordinator.startSos(TriggerSource.MANUAL_SOS)
    }

    fun dismissOnboarding() {
        viewModelScope.launch {
            appContainer.settingsStore.updateSettings { it.copy(onboardingSeen = true) }
        }
    }

    fun saveLanguage(languageCode: String) {
        viewModelScope.launch {
            appContainer.settingsStore.updateSettings { it.copy(languageCode = languageCode) }
            trackIfAllowed("language_selected", mapOf("language" to languageCode))
        }
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appContainer.settingsStore.updateSettings { it.copy(analyticsEnabled = enabled) }
            if (enabled) {
                appContainer.analyticsTracker.track("analytics_enabled")
            }
        }
    }

    fun completeOnboarding(settings: SosSettings, onInvalidNumber: () -> Unit) {
        if (!PhoneNumberValidator.isValid(settings.emergencyNumber)) {
            onInvalidNumber()
            return
        }
        viewModelScope.launch {
            val completed = settings.copy(onboardingSeen = true, enabled = true)
            appContainer.settingsStore.updateSettings { completed }
            appContainer.sosCoordinator.setArmed(true)
            trackIfAllowed("onboarding_completed")
            appContainer.monitoringServiceController.start()
        }
    }

    fun stopSos() {
        trackIfAllowed("sos_stopped")
        appContainer.sosCoordinator.stopSos(StopReason.USER_STOPPED)
    }
}
