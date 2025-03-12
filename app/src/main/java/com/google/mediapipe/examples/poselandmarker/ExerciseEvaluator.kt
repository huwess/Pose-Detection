package com.google.mediapipe.examples.poselandmarker



// Data class for a progress indicator (its position, progress, and colors)
data class ProgressIndicatorData(
    val x: Float,
    val y: Float,
    val progress: Float,     // 0 to 100 percent progress
    val mainColor: Int,      // Fill color for the progress indicator
    val bgColor: Int         // Background (or outline) color
)

data class ExerciseEvaluationResult(
    val reps: Int,
    val stage: String,    // e.g. "up" or "down"
    val feedback: String,
    val warning: String,
    val progressIndicators: List<ProgressIndicatorData>?
)

interface ExerciseEvaluator {
    fun evaluatePose(points: List<Pair<Float, Float>>): ExerciseEvaluationResult
}