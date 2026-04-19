package com.example.sosapp.domain

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CameraHandler(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    @SuppressLint("MissingPermission")
    fun capturePhoto(): File? {
        val cameraId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val lensFacing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                lensFacing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (e: Exception) {
            null
        } ?: return null

        val handlerThread = HandlerThread("CameraBackground").apply { start() }
        val handler = Handler(handlerThread.looper)
        val imageReader = ImageReader.newInstance(1280, 720, ImageFormat.JPEG, 2)
        val latch = CountDownLatch(1)
        var capturedFile: File? = null

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            val file = File(context.cacheDir, "sos_capture_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { it.write(bytes) }
            capturedFile = file
            image.close()
            latch.countDown()
        }, handler)

        var cameraDevice: CameraDevice? = null
        var captureSession: CameraCaptureSession? = null

        try {
            val deviceLatch = CountDownLatch(1)
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    deviceLatch.countDown()
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    deviceLatch.countDown()
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    deviceLatch.countDown()
                }
            }, handler)

            if (!deviceLatch.await(3, TimeUnit.SECONDS) || cameraDevice == null) return null

            val sessionLatch = CountDownLatch(1)
            cameraDevice?.createCaptureSession(listOf(imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    sessionLatch.countDown()
                }
                override fun onConfigureFailed(session: CameraCaptureSession) {
                    sessionLatch.countDown()
                }
            }, handler)

            if (sessionLatch.await(3, TimeUnit.SECONDS) && captureSession != null) {
                val captureRequest = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)?.apply {
                    addTarget(imageReader.surface)
                }?.build()
                if (captureRequest != null) {
                    captureSession?.capture(captureRequest, null, handler)
                    latch.await(5, TimeUnit.SECONDS)
                }
            }
        } catch (e: Exception) {
            Log.e("CameraHandler", "Error capturing photo", e)
        } finally {
            captureSession?.close()
            cameraDevice?.close()
            imageReader.close()
            handlerThread.quitSafely()
        }

        return capturedFile
    }
}
