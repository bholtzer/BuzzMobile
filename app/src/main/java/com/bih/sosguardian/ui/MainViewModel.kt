package com.bih.sosguardian.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bih.sosguardian.SosApplication
import com.bih.sosguardian.data.SosRuntimeState
import com.bih.sosguardian.data.SosSettings
import com.bih.sosguardian.data.StopReason
import com.bih.sosguardian.data.TriggerSource
import com.bih.sosguardian.domain.PhoneNumberValidator
import com.bih.sosguardian.service.SosForegroundService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContainer = (application as SosApplication).appContainer

    val settings: StateFlow<SosSettings> = appContainer.settingsStore.settings
    val runtimeState: StateFlow<SosRuntimeState> = appContainer.sosCoordinator.runtimeState

    fun saveSettings(settings: SosSettings, onInvalidNumber: () -> Unit) {
        if (!PhoneNumberValidator.isValid(settings.emergencyNumber)) {
            onInvalidNumber()
            return
        }
        viewModelScope.launch {
            appContainer.settingsStore.updateSettings { settings }
            appContainer.sosCoordinator.setArmed(settings.enabled)
            if (settings.enabled) {
                SosForegroundService.Companion.start(getApplication())
            } else {
                SosForegroundService.Companion.stop(getApplication())
            }
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
            if (enabled) {
                SosForegroundService.Companion.start(getApplication())
            } else {
                SosForegroundService.Companion.stop(getApplication())
            }
        }
    }

    fun triggerManualTest() {
        appContainer.sosCoordinator.startSos(TriggerSource.MANUAL_TEST)
    }

    fun triggerManualSos() {
        appContainer.sosCoordinator.startSos(TriggerSource.MANUAL_SOS)
    }

    fun stopSos() {
        appContainer.sosCoordinator.stopSos(StopReason.USER_STOPPED)
    }
}
