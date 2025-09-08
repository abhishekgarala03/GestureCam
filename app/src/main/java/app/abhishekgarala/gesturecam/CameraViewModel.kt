package app.abhishekgarala.gesturecam


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.Math.pow
import kotlin.math.pow
import kotlin.math.sqrt

class CameraViewModel : ViewModel() {
    private val _currentGesture = MutableStateFlow<String?>(null)
    val currentGesture: StateFlow<String?> = _currentGesture

    private val _zoomRatio = MutableStateFlow(1f)
    val zoomRatio: StateFlow<Float> = _zoomRatio

    private var lastGesture: String? = null
    private var gestureCount = 0
    private val debounceFrames = 3

    private var lastPinchDistance: Float? = null
    private var maxZoomRatio: Float = 5f
    private var minZoomRatio: Float = 1f


    fun processGestureResult(result: GestureRecognizerResult, confidenceThreshold: Float, landmarks: List<NormalizedLandmark>?) {
        viewModelScope.launch {
            if (result.gestures().isEmpty()) {
                resetDebounce()
                _currentGesture.value = null
                return@launch
            }

            val gestures = result.gestures()[0]
            val topGesture = gestures.maxByOrNull { it.score() }
            var recognized = topGesture?.categoryName()

            if (recognized == "None" || (topGesture?.score() ?: 0f) < confidenceThreshold) {
                recognized = classifyCustom(landmarks ?: emptyList())
            }

            if (recognized == "Pinch") {
                val currentDistance = calculatePinchDistance(landmarks ?: emptyList())
                if (lastPinchDistance != null) {
                    val delta = currentDistance - (lastPinchDistance ?: 0f)
                    val sensitivity = 10f
                    var newZoom = _zoomRatio.value + (delta * sensitivity)
                    newZoom = newZoom.coerceIn(minZoomRatio, maxZoomRatio)
                    _zoomRatio.value = newZoom
                }
                lastPinchDistance = currentDistance
            } else {
                lastPinchDistance = null
            }

            if (recognized == lastGesture) {
                gestureCount++
                if (gestureCount >= debounceFrames) {
                    _currentGesture.value = recognized
                }
            } else {
                lastGesture = recognized
                gestureCount = 1
            }
        }
    }

    fun setMaxZoomRatio(maxZoom: Float) {
        maxZoomRatio = maxZoom.coerceAtLeast(1f)
    }

    private fun classifyCustom(landmarks: List<NormalizedLandmark>): String? {
        if (landmarks.isEmpty()) return null

        val thumbTip = landmarks.getOrNull(HandLandmark.THUMB_TIP) ?: return null
        val indexTip = landmarks.getOrNull(HandLandmark.INDEX_FINGER_TIP) ?: return null
        val middleTip = landmarks.getOrNull(HandLandmark.MIDDLE_FINGER_TIP) ?: return null
        val ringTip = landmarks.getOrNull(HandLandmark.RING_FINGER_TIP) ?: return null
        val pinkyTip = landmarks.getOrNull(HandLandmark.PINKY_TIP) ?: return null

        val thumbIndexDist = euclideanDistance(thumbTip, indexTip)

        val indexOpen = indexTip.y() < landmarks[HandLandmark.INDEX_FINGER_DIP].y()
        val middleOpen = middleTip.y() < landmarks[HandLandmark.MIDDLE_FINGER_DIP].y()
        val ringOpen = ringTip.y() < landmarks[HandLandmark.RING_FINGER_DIP].y()
        val pinkyOpen = pinkyTip.y() < landmarks[HandLandmark.PINKY_DIP].y()

        if (thumbIndexDist < 0.05 && middleOpen && ringOpen && pinkyOpen) return "OK"
        if (thumbIndexDist < 0.05 && !middleOpen && !ringOpen && !pinkyOpen) return "Pinch"

        return null
    }

    private fun calculatePinchDistance(landmarks: List<NormalizedLandmark>): Float {
        if (landmarks.isEmpty()) return 0f
        val thumbTip = landmarks.getOrNull(HandLandmark.THUMB_TIP) ?: return 0f
        val indexTip = landmarks.getOrNull(HandLandmark.INDEX_FINGER_TIP) ?: return 0f
        return euclideanDistance(thumbTip, indexTip)
    }

    private fun euclideanDistance(a: NormalizedLandmark, b: NormalizedLandmark): Float {
        return sqrt(
            (a.x() - b.x().toDouble()).pow(2.0) + pow(a.y() - b.y().toDouble(), 2.0)
        ).toFloat()
    }

    private fun resetDebounce() {
        lastGesture = null
        gestureCount = 0
    }
}