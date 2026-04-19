package com.bih.sosapp

import android.app.Application
import com.bih.sosapp.data.SosSettingsStore
import com.bih.sosapp.domain.CallHandler
import com.bih.sosapp.domain.CameraHandler
import com.bih.sosapp.domain.FlashBlinkController
import com.bih.sosapp.domain.LocationShareHandler
import com.bih.sosapp.domain.SirenPlayer
import com.bih.sosapp.domain.SosCoordinator
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
            cameraHandler = CameraHandler(this),
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
    val cameraHandler: CameraHandler,
    appScope: CoroutineScope,
) {
    val sosCoordinator = SosCoordinator(
        context = settingsStore.context,
        settingsStore = settingsStore,
        sirenPlayer = sirenPlayer,
        flashBlinkController = flashBlinkController,
        callHandler = callHandler,
        locationShareHandler = locationShareHandler,
        cameraHandler = cameraHandler,
        appScope = appScope,
    )
}
