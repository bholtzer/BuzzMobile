package com.bih.sosguardian

import android.app.Application
import com.bih.sosguardian.data.SosSettingsStore
import com.bih.sosguardian.domain.CallHandler
import com.bih.sosguardian.domain.CameraHandler
import com.bih.sosguardian.domain.FlashBlinkController
import com.bih.sosguardian.domain.LocationShareHandler
import com.bih.sosguardian.domain.SirenPlayer
import com.bih.sosguardian.domain.SosCoordinator
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
