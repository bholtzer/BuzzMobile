package com.bih.sosguardian

import android.app.Application
import com.bih.sosguardian.data.device.AndroidMonitoringServiceController
import com.bih.sosguardian.data.device.AndroidSosPlatformActions
import com.bih.sosguardian.data.device.CallHandler
import com.bih.sosguardian.data.device.CameraHandler
import com.bih.sosguardian.data.device.FlashBlinkController
import com.bih.sosguardian.data.device.LocationShareHandler
import com.bih.sosguardian.data.device.SirenPlayer
import com.bih.sosguardian.data.SosSettingsStore
import com.bih.sosguardian.domain.EmergencyCaller
import com.bih.sosguardian.domain.FlashController
import com.bih.sosguardian.domain.LocationMessenger
import com.bih.sosguardian.domain.MonitoringServiceController
import com.bih.sosguardian.domain.PhotoCapturer
import com.bih.sosguardian.domain.SirenController
import com.bih.sosguardian.domain.SosCoordinator
import com.bih.sosguardian.domain.SosPlatformActions
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
            platformActions = AndroidSosPlatformActions(this),
            monitoringServiceController = AndroidMonitoringServiceController(this),
            appScope = appScope,
        )
    }
}

class AppContainer(
    val settingsStore: SosSettingsStore,
    sirenPlayer: SirenController,
    flashBlinkController: FlashController,
    callHandler: EmergencyCaller,
    locationShareHandler: LocationMessenger,
    val cameraHandler: PhotoCapturer,
    platformActions: SosPlatformActions,
    val monitoringServiceController: MonitoringServiceController,
    appScope: CoroutineScope,
) {
    val sosCoordinator = SosCoordinator(
        settingsRepository = settingsStore,
        sirenController = sirenPlayer,
        flashController = flashBlinkController,
        emergencyCaller = callHandler,
        locationMessenger = locationShareHandler,
        photoCapturer = cameraHandler,
        platformActions = platformActions,
        appScope = appScope,
    )
}
