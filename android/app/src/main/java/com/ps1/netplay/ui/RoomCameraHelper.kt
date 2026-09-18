package com.ps1.netplay.ui
import android.widget.*
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import java.util.concurrent.atomic.AtomicBoolean

/** * Lightweight hardware camera helper for zero-lag local participant video feed. * Utilizes hardware Camera2 API on a dedicated thread to ensure zero impact on the PS1 emulator core. */class RoomCameraHelper(private val context: Context) {
    companion object {
        private const val TAG = "RoomCameraHelper"
    }
private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var isFrontFacing = true
    val isCameraOn = AtomicBoolean(false)
var onCameraToggled: ((isOn: Boolean) -> Unit)? = null
    var onFacingSwitched: ((isFront: Boolean) -> Unit)? = null
    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").apply { start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }
private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping background thread: ${e.message}")
        }
    }
    @SuppressLint("MissingPermission")
fun startCamera(textureView: TextureView, front: Boolean = isFrontFacing) {
        isFrontFacing = front
        startBackgroundThread()
        if (textureView.isAvailable) {
            openCamera(textureView)
        } else {
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    openCamera(textureView)
                }
override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    closeCamera()
                    return true
                }
override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
        }
    }
    @SuppressLint("MissingPermission")
private fun openCamera(textureView: TextureView) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        try {
            val facingTarget = if (isFrontFacing) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
            var targetCameraId: String? = null
            for (id in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(id)
val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == facingTarget) {
                    targetCameraId = id
                    break
                }
            }
            if (targetCameraId == null && manager.cameraIdList.isNotEmpty()) {
                targetCameraId = manager.cameraIdList[0]
            }
            if (targetCameraId == null) {
                Log.w(TAG, "No suitable camera found")
                return
            }
            manager.openCamera(targetCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    isCameraOn.set(true)
                    createCameraPreviewSession(camera, textureView)
                    onCameraToggled?.invoke(true)
                }
override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    isCameraOn.set(false)
                    onCameraToggled?.invoke(false)
                }
override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    isCameraOn.set(false)
                    onCameraToggled?.invoke(false)
                    Log.e(TAG, "Camera error: $error")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera: ${e.message}", e)
            isCameraOn.set(false)
            onCameraToggled?.invoke(false)
        }
    }
private fun createCameraPreviewSession(camera: CameraDevice, textureView: TextureView) {
        try {
            val surfaceTexture = textureView.surfaceTexture ?: return
            surfaceTexture.setDefaultBufferSize(320, 240) // Lightweight low-res buffer for gaming cams
            val surface = Surface(surfaceTexture)
val previewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }
            camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = session
                    try {
                        session.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler)
                    } catch (e: Exception) {
                        Log.e(TAG, "Capture session repeating request error: ${e.message}")
                    }
                }
override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Camera capture session configuration failed")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating camera preview: ${e.message}", e)
        }
    }
fun switchCameraFacing(textureView: TextureView) {
        closeCamera()
        isFrontFacing = !isFrontFacing
        onFacingSwitched?.invoke(isFrontFacing)
        startCamera(textureView, isFrontFacing)
    }
fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            isCameraOn.set(false)
            stopBackgroundThread()
            onCameraToggled?.invoke(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera: ${e.message}")
        }
    }}