
package com.example.browcontrol

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView

    private var faceLandmarker: FaceLandmarker? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var eyebrowBaseline: Float? = null
    private var lastActionTime = 0L
    private var lastTimestamp = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20, 20, 20, 20)
        }

        statusText = TextView(this).apply {
            text = "Brow Control Ready"
            textSize = 22f
            gravity = Gravity.CENTER
        }

        previewView = PreviewView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val startButton = Button(this).apply {
            text = "START BROW CONTROL"
            setOnClickListener {
                checkCameraPermission()
            }
        }

        layout.addView(statusText)
        layout.addView(previewView)
        layout.addView(startButton)

        setContentView(layout)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startFaceLandmarker()
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startFaceLandmarker()
            startCamera()
        }
    }

    private fun startFaceLandmarker() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.VIDEO)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.3f)
                .setMinFacePresenceConfidence(0.3f)
                .setMinTrackingConfidence(0.3f)
                .build()

            faceLandmarker =
                FaceLandmarker.createFromOptions(this, options)

            runOnUiThread {
                statusText.text = "Face detection ready"
            }

        } catch (e: Exception) {
            runOnUiThread {
                statusText.text = "Model error: ${e.message}"
            }
        }
    }

    private fun processFace(result: FaceLandmarkerResult) {

        if (result.faceLandmarks().isEmpty()) {
            runOnUiThread {
                statusText.text = "Face not detected"
            }
            return
        }

        val points = result.faceLandmarks()[0]

        val eyebrowY =
            (points[105].y() + points[334].y()) / 2f

        val eyeY =
            (points[159].y() + points[386].y()) / 2f

        val measurement = eyebrowY - eyeY

        if (eyebrowBaseline == null) {
            eyebrowBaseline = measurement

            runOnUiThread {
                statusText.text = "Keep your eyebrows relaxed"
            }
            return
        }

        val difference = measurement - eyebrowBaseline!!
        val now = SystemClock.uptimeMillis()

        if (now - lastActionTime >= 1200) {

            when {
                difference < -0.025f -> {

                    MyAccessibilityService.scrollUp()

                    runOnUiThread {
                        statusText.text = "Scroll Up"
                    }

                    lastActionTime = now
                }

                difference > 0.025f -> {

                    MyAccessibilityService.scrollDown()

                    runOnUiThread {
                        statusText.text = "Scroll Down"
                    }

                    lastActionTime = now
                }
            }
        }

        eyebrowBaseline =
            eyebrowBaseline!! * 0.98f + measurement * 0.02f
    }

    private fun startCamera() {

        runOnUiThread {
            statusText.text = "Camera starting..."
        }

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setOutputImageFormat(
                        ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
                    )
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    analyzeFrame(imageProxy)
                }

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalysis
                )

                runOnUiThread {
                    statusText.text = "Camera started"
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text =
                        "Camera error: ${e.message}"
                }
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {

        try {
            val plane = imageProxy.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            val width = imageProxy.width
            val height = imageProxy.height

            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                for (x in 0 until width) {

                    val index = y * rowStride + x * pixelStride

                    val r = buffer.get(index).toInt() and 0xFF
                    val g = buffer.get(index + 1).toInt() and 0xFF
                    val b = buffer.get(index + 2).toInt() and 0xFF
                    val a = buffer.get(index + 3).toInt() and 0xFF

                    pixels[y * width + x] =
                        (a shl 24) or
                        (r shl 16) or
                        (g shl 8) or b
                }
            }

            val bitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

            bitmap.setPixels(
                pixels,
                0,
                width,
                0,
                0,
                width,
                height
            )

            val mpImage = BitmapImageBuilder(bitmap).build()

            val imageOptions = ImageProcessingOptions.builder()
                .setRotationDegrees(
                    imageProxy.imageInfo.rotationDegrees
                )
                .build()

            val timestamp = maxOf(
                SystemClock.uptimeMillis(),
                lastTimestamp + 1
            )

            lastTimestamp = timestamp

            val result = faceLandmarker?.detectForVideo(
                mpImage,
                imageOptions,
                timestamp
            )

            if (result != null) {
                processFace(result)
            }

        } catch (e: Exception) {

            runOnUiThread {
                statusText.text =
                    "Frame error: ${e.javaClass.simpleName}: ${e.message}"
            }

        } finally {
            imageProxy.close()
        }
    }

    override fun onDestroy() {
        faceLandmarker?.close()
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}
