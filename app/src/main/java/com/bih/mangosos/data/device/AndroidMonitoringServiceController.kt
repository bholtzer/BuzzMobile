package com.bih.mangosos.data.device

import android.content.Context
import com.bih.mangosos.domain.MonitoringServiceController
import com.bih.mangosos.service.SosForegroundService

class AndroidMonitoringServiceController(
    private val context: Context,
) : MonitoringServiceController {
    override fun start() {
        SosForegroundService.start(context)
    }

    override fun stop() {
        SosForegroundService.stop(context)
    }
}
