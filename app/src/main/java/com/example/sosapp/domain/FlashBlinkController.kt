package com.example.sosapp.domain

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FlashBlinkController(
    context: Context,
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = cameraManager.cameraIdList.firstOrNull { id ->
        val characteristics = cameraManager.getCameraCharacteristics(id)
        val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
        hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK
    }
    private var blinkJob: Job? = null

    fun hasFlash(): Boolean = cameraId != null

    fun start(scope: CoroutineScope, blinkMs: Long) {
        val safeCameraId = cameraId ?: return
        if (blinkJob?.isActive == true) return

        blinkJob = scope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    runCatching { cameraManager.setTorchMode(safeCameraId, true) }
                    delay(blinkMs)
                    runCatching { cameraManager.setTorchMode(safeCameraId, false) }
                    delay(blinkMs)
                }
            } finally {
                runCatching { cameraManager.setTorchMode(safeCameraId, false) }
            }
        }
    }

    suspend fun stop() {
        blinkJob?.cancelAndJoin()
        blinkJob = null
        cameraId?.let { safeCameraId ->
            runCatching { cameraManager.setTorchMode(safeCameraId, false) }
        }
    }
}
