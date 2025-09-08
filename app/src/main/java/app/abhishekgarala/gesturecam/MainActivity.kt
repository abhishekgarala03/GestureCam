package app.abhishekgarala.gesturecam


import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.lifecycleScope
import app.abhishekgarala.gesturecam.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var gestureRecognizerHelper: GestureRecognizerHelper
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var camera: Camera? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private lateinit var viewModel: CameraViewModel
    private lateinit var prefs: SharedPreferences
    private var cameraControl: CameraControl? = null
    private lateinit var backgroundExecutor: ExecutorService

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.all { it }) {
                startCamera()
            } else {
                Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG)
                    .show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        backgroundExecutor = Executors.newSingleThreadExecutor()

        viewModel = ViewModelProviders.of(this)[CameraViewModel::class.java]

        cameraExecutor = Executors.newSingleThreadExecutor()
        prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            activityResultLauncher.launch(REQUIRED_PERMISSIONS)
        }

        gestureRecognizerHelper = GestureRecognizerHelper(this, ::onGestureResult)

        lifecycleScope.launch {
            viewModel.currentGesture.collectLatest { gesture ->
                handleGesture(gesture)
            }
        }

        lifecycleScope.launch {
            viewModel.zoomRatio.collectLatest { zoom ->
                cameraControl?.setZoomRatio(zoom)
            }
        }

        binding.fabCapture.setOnClickListener { capturePhoto() }
        binding.settingsButton.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                )
            )
        }
        binding.flashSwitch.setOnCheckedChangeListener { _, isChecked ->
            camera?.cameraControl?.enableTorch(isChecked)
        }

        showOnboardingIfFirstLaunch()
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1080, 1920))
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(720, 1280))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        recognizeHand(image)
                    }
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    videoCapture,
                    imageAnalysis
                )
                cameraControl = camera?.cameraControl

                val zoomState = camera?.cameraInfo?.zoomState?.value
                zoomState?.let {
                    viewModel.setMaxZoomRatio(it.maxZoomRatio)
                }
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun recognizeHand(imageProxy: ImageProxy) {
        gestureRecognizerHelper.recognizeLiveStream(
            imageProxy = imageProxy,
        )
    }

    private fun onGestureResult(gestureData: GestureData) {
        binding.overlayView.setResults(
            gestureData.result,
            gestureData.imageHeight,
            gestureData.imageWidth,
            RunningMode.LIVE_STREAM
        )
        viewModel.processGestureResult(
            gestureData.result,
            prefs.getFloat("confidence_threshold", 0.6f),
            gestureData.landmarks
        )
    }

    private fun handleGesture(gesture: String?) {
        when (gesture) {
            "Open_Palm" -> capturePhoto()
            "Victory" -> toggleVideoRecording()
            "Thumb_Up" -> switchCamera()
            "OK" -> openGallery()
            "Pinch" -> {}
            null -> {}
        }
    }

    private fun capturePhoto() {
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GestureCamera")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(this@MainActivity, "Photo saved!", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            })
    }

    private fun toggleVideoRecording() {
        if (recording != null) {
            recording?.stop()
            recording = null
            return
        }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GestureCamera")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        recording = videoCapture?.output?.prepareRecording(this, mediaStoreOutputOptions)
            ?.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            Toast.makeText(this, "Video saved!", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e(TAG, "Video capture error: ${recordEvent.error}")
                        }
                        recording = null
                    }
                }
            }
    }

    private fun switchCamera() {
        lensFacing =
            if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        startCamera()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivity(intent)
    }

    private fun showOnboardingIfFirstLaunch() {
        if (prefs.getBoolean("first_launch", true)) {
            AlertDialog.Builder(this)
                .setTitle("Welcome to Gesture Camera")
                .setMessage(
                    "Control the camera with hand gestures:\n\n" +
                            getString(R.string.gesture_open_palm) + "\n" +
                            getString(R.string.gesture_peace) + "\n" +
                            getString(R.string.gesture_thumbs_up) + "\n" +
                            getString(R.string.gesture_ok) + "\n" +
                            getString(R.string.gesture_pinch)
                )
                .setPositiveButton("Got it") { _, _ ->
                    prefs.edit().putBoolean("first_launch", false).apply()
                }
                .show()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        gestureRecognizerHelper.close()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }
}