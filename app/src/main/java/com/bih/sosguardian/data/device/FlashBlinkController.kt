package com.bih.sosguardian.data.device

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import com.bih.sosguardian.domain.FlashController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FlashBlinkController(
    context: Context,
) : FlashController {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = try {
        cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK
        }
    } catch (e: Exception) {
        null
    }

    private var blinkJob: Job? = null

    override fun hasFlash(): Boolean = cameraId != null

    override fun start(scope: CoroutineScope, blinkMs: Long) {
        val safeCameraId = cameraId ?: return
        if (blinkJob?.isActive == true) return

        blinkJob = scope.launch(Dispatchers.IO) {
            try {
                while (true) {
                    safeSetTorchMode(safeCameraId, true)
                    delay(blinkMs)
                    safeSetTorchMode(safeCameraId, false)
                    delay(blinkMs)
                }
            } finally {
                safeSetTorchMode(safeCameraId, false)
            }
        }
    }

    override suspend fun stop() {
        blinkJob?.cancelAndJoin()
        blinkJob = null
        cameraId?.let { safeCameraId ->
            safeSetTorchMode(safeCameraId, false)
        }
    }

    private fun safeSetTorchMode(id: String, enabled: Boolean) {
        try {
            cameraManager.setTorchMode(id, enabled)
        } catch (e: CameraAccessException) {
            Log.e("FlashBlinkController", "Camera access exception: ${e.message}")
        } catch (e: Exception) {
            Log.e("FlashBlinkController", "Failed to set torch mode", e)
        }
    }
}
