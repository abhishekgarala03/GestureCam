package app.abhishekgarala.gesturecam

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult

data class GestureData(
    val result: GestureRecognizerResult,
    val imageWidth: Int,
    val imageHeight: Int,
    val landmarks: List<NormalizedLandmark>?,
)