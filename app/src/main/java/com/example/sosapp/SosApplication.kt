package com.example.sosapp

import android.app.Application
import com.example.sosapp.data.SosSettingsStore
import com.example.sosapp.domain.CallHandler
import com.example.sosapp.domain.FlashBlinkController
import com.example.sosapp.domain.LocationShareHandler
import com.example.sosapp.domain.SirenPlayer
import com.example.sosapp.domain.SosCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SosApplication : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(
            settingsStore = SosSettingsStore(this, appScope),
            sirenPlayer = SirenPlayer(this),
            flashBlinkController = FlashBlinkController(this),
            callHandler = CallHandler(this),
            locationShareHandler = LocationShareHandler(this),
            appScope = appScope,
        )
    }
}

class AppContainer(
    val settingsStore: SosSettingsStore,
    sirenPlayer: SirenPlayer,
    flashBlinkController: FlashBlinkController,
    callHandler: CallHandler,
    locationShareHandler: LocationShareHandler,
    appScope: CoroutineScope,
) {
    val sosCoordinator = SosCoordinator(
        context = settingsStore.context,
        settingsStore = settingsStore,
        sirenPlayer = sirenPlayer,
        flashBlinkController = flashBlinkController,
        callHandler = callHandler,
        locationShareHandler = locationShareHandler,
        appScope = appScope,
    )
}
