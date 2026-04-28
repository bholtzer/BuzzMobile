package com.bih.sosguardian.data.device

import android.content.Context
import com.bih.sosguardian.domain.MonitoringServiceController
import com.bih.sosguardian.service.SosForegroundService

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
