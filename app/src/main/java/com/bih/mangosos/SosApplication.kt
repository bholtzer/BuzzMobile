package com.bih.mangosos

import android.app.Application
import com.bih.mangosos.data.device.AndroidMonitoringServiceController
import com.bih.mangosos.data.device.AndroidSosPlatformActions
import com.bih.mangosos.data.device.CallHandler
import com.bih.mangosos.data.device.CameraHandler
import com.bih.mangosos.data.device.FirebaseEmergencyCloudReporter
import com.bih.mangosos.data.device.FlashBlinkController
import com.bih.mangosos.data.device.FirebaseAnalyticsTracker
import com.bih.mangosos.data.device.LocationShareHandler
import com.bih.mangosos.data.device.LogcatEmergencyCloudReporter
import com.bih.mangosos.data.device.LogcatAnalyticsTracker
import com.bih.mangosos.data.device.SirenPlayer
import com.bih.mangosos.data.SosSettingsStore
import com.bih.mangosos.domain.AnalyticsTracker
import com.bih.mangosos.domain.EmergencyCloudReporter
import com.bih.mangosos.domain.EmergencyCaller
import com.bih.mangosos.domain.FlashController
import com.bih.mangosos.domain.LocationMessenger
import com.bih.mangosos.domain.MonitoringServiceController
import com.bih.mangosos.domain.PhotoCapturer
import com.bih.mangosos.domain.SirenController
import com.bih.mangosos.domain.SosCoordinator
import com.bih.mangosos.domain.SosPlatformActions
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
            emergencyCloudReporter = FirebaseEmergencyCloudReporter(
                context = this,
                fallback = LogcatEmergencyCloudReporter(),
            ),
            analyticsTracker = FirebaseAnalyticsTracker(
                context = this,
                fallback = LogcatAnalyticsTracker(),
            ),
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
    emergencyCloudReporter: EmergencyCloudReporter,
    val analyticsTracker: AnalyticsTracker,
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
        emergencyCloudReporter = emergencyCloudReporter,
        appScope = appScope,
    )
}
