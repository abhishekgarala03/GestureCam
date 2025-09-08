# GestureCam
A modern Android application that integrates the Camera2 API with real-time gesture recognition using MediaPipe, enabling users to control camera functions through hand gestures.

## Overview

This app leverages the Camera2 API for advanced camera functionality and MediaPipe for real-time hand gesture recognition. It supports five distinct gestures to control photo capture, video recording, camera switching, gallery access, and zoom functionality, all while maintaining smooth performance and a polished user experience.

## Features

- **Camera Module**:
  - Real-time 1080p preview with gesture overlay
  - Photo capture
  - Video recording
  - Front/back camera switching
  - Flash control
- **Gesture Processing**:
  - Real-time hand detection and classification
  - Supports five gestures:
    - ✋ Open palm: Capture photo
    - ✌️ Peace sign: Start/stop video recording
    - 👍 Thumbs up: Switch camera
    - 👌 OK sign: Open gallery
    - 🤏 Pinch: Zoom in/out
  - Confidence thresholding and gesture debouncing
- **UI/UX**:
  - Material Design 3 components
  - Gesture indicator overlay with smooth animations
  - Accessibility support
  - Settings screen for user customization
- **Performance**:
  - 30+ FPS camera preview
  - <100ms gesture detection latency
  - <200MB additional memory usage
  - Battery-efficient processing

## Technical Architecture

The app follows this processing pipeline:
```
CameraX/Camera2 → FrameProcessor → MediaPipe → GestureHandler → UIUpdater
```

- **CameraX/Camera2**: Handles camera preview, photo capture, and video recording.
- **FrameProcessor**: Extracts frames for gesture analysis.
- **MediaPipe**: Performs real-time hand detection and gesture classification.
- **GestureHandler**: Processes gesture results with confidence thresholding and debouncing.
- **UIUpdater**: Updates the UI with visual feedback and animations.

## Usage

1. Launch the app and grant camera and storage permissions.
2. Use the following gestures in front of the camera:
   - ✋ Open palm to take a photo.
   - ✌️ Peace sign to start/stop video recording.
   - 👍 Thumbs up to switch between front and back cameras.
   - 👌 OK sign to open the gallery.
   - 🤏 Pinch to zoom in or out.
3. Access the settings screen to customize preferences.

## Code Quality

- **Architecture**: MVVM with clean separation of concerns
- **Concurrency**: Kotlin Coroutines and Flow for asynchronous operations
- **Testing**: Unit tests for business logic
- **Documentation**: Public APIs are fully documented
- **Error Handling**: Robust handling of camera and ML errors

## Performance Optimizations

- Optimized for 1080p @ 30fps minimum
- Gesture detection latency <100ms
- Memory usage kept below 200MB
- Battery-efficient processing with minimal background activity

## UI/UX Guidelines

- Follows Material Design 3 for a modern, consistent look
- Smooth animations for gesture feedback
- Responsive design for various screen sizes
- Accessibility support for inclusive usage
- Onboarding flow to guide new users

## Development Considerations

- **Orientation**: Supports all device orientations
- **Lifecycle**: Properly handles app lifecycle events
- **Theming**: Supports both light and dark themes
- **Permissions**: Runtime permission handling for camera and storage
