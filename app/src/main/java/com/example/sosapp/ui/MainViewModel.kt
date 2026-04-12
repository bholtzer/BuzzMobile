package com.example.sosapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sosapp.SosApplication
import com.example.sosapp.data.SosRuntimeState
import com.example.sosapp.data.SosSettings
import com.example.sosapp.data.StopReason
import com.example.sosapp.data.TriggerSource
import com.example.sosapp.domain.PhoneNumberValidator
import com.example.sosapp.service.SosForegroundService
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
                SosForegroundService.start(getApplication())
            } else {
                SosForegroundService.stop(getApplication())
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
                SosForegroundService.start(getApplication())
            } else {
                SosForegroundService.stop(getApplication())
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
