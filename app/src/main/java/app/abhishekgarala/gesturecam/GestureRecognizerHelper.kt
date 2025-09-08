package app.abhishekgarala.gesturecam

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import android.util.Log
import androidx.camera.core.CameraSelector
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.Delegate

class GestureRecognizerHelper(
    private val context: Context,
    private val resultListener: (GestureData) -> Unit,
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
) {
    private var gestureRecognizer: GestureRecognizer? = null

    init {
        setupGestureRecognizer()
    }

    private fun setupGestureRecognizer() {
        val baseOptions = BaseOptions.builder()
            .setDelegate(Delegate.GPU)
            .setModelAssetPath("gesture_recognizer.task")
            .build()

        val options = GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setMinHandDetectionConfidence(0.5f)
            .setMinHandPresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setNumHands(1)
            .setResultListener(::handleResult)
            .setErrorListener { error -> Log.e(TAG, "Error: $error") }
            .build()

        gestureRecognizer = GestureRecognizer.createFromOptions(context, options)
    }

    fun recognizeLiveStream(imageProxy: ImageProxy, lensFacing: Int) {
        this.lensFacing = lensFacing // Update lensFacing
        val frameTime = SystemClock.uptimeMillis()

        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            // Only flip horizontally for front camera
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                postScale(-1f, 1f, imageProxy.width.toFloat(), imageProxy.height.toFloat())
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmapBuffer,
            0,
            0,
            bitmapBuffer.width,
            bitmapBuffer.height,
            matrix,
            true
        )

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        gestureRecognizer?.recognizeAsync(mpImage, frameTime)
    }

    private fun handleResult(result: GestureRecognizerResult, inputImage: MPImage) {
        val landmarks = if (result.landmarks().isNotEmpty()) result.landmarks()[0] else null
        resultListener(GestureData(result, inputImage.width, inputImage.height, landmarks))
    }

    fun close() {
        gestureRecognizer?.close()
    }

    companion object {
        private const val TAG = "GestureRecognizerHelper"
    }
}