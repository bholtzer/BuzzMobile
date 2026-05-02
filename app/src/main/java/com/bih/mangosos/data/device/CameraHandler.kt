package com.bih.mangosos.data.device

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.util.Log
import com.bih.mangosos.domain.PhotoCapturer
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class CameraHandler(private val context: Context) : PhotoCapturer {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    @SuppressLint("MissingPermission")
    override fun capturePhoto(): File? {
        val cameraId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                lensFacing == CameraCharacteristics.LENS_FACING_BACK
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

    @SuppressLint("MissingPermission")
    override fun captureVideo(durationMs: Long): File? {
        val cameraId = findBackCameraId() ?: return null
        val handlerThread = HandlerThread("VideoBackground").apply { start() }
        val handler = Handler(handlerThread.looper)
        val videoFile = File(context.cacheDir, "sos_video_${System.currentTimeMillis()}.mp4")
        val mediaRecorder = createVideoRecorder(videoFile)

        var cameraDevice: CameraDevice? = null
        var captureSession: CameraCaptureSession? = null
        var recorded = false

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

            val recorderSurface = mediaRecorder.surface
            val sessionLatch = CountDownLatch(1)
            cameraDevice?.createCaptureSession(listOf(recorderSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    sessionLatch.countDown()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    sessionLatch.countDown()
                }
            }, handler)

            if (!sessionLatch.await(3, TimeUnit.SECONDS) || captureSession == null) return null

            val request = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)?.apply {
                addTarget(recorderSurface)
            }?.build() ?: return null

            captureSession?.setRepeatingRequest(request, null, handler)
            mediaRecorder.start()
            recorded = true
            Thread.sleep(durationMs.coerceIn(MIN_VIDEO_MS, MAX_VIDEO_MS))
            captureSession?.stopRepeating()
        } catch (e: Exception) {
            Log.e("CameraHandler", "Error capturing video", e)
        } finally {
            if (recorded) {
                runCatching { mediaRecorder.stop() }
            }
            runCatching { mediaRecorder.reset() }
            runCatching { mediaRecorder.release() }
            captureSession?.close()
            cameraDevice?.close()
            handlerThread.quitSafely()
        }

        return videoFile.takeIf { it.exists() && it.length() > 0L }
    }

    private fun findBackCameraId(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                lensFacing == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun createVideoRecorder(videoFile: File): MediaRecorder {
        return MediaRecorder().apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(videoFile.absolutePath)
            setVideoEncodingBitRate(2_500_000)
            setVideoFrameRate(30)
            setVideoSize(1280, 720)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setOrientationHint(90)
            prepare()
        }
    }

    private companion object {
        const val MIN_VIDEO_MS = 1_000L
        const val MAX_VIDEO_MS = 30_000L
    }
}
