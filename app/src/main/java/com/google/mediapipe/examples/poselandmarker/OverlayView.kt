package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import android.util.Log

// Callback interface for updating the UI
interface OverlayUpdateListener {
    fun onRepsUpdated(reps: Int)
    fun onStageUpdated(stage: String)
    fun onSignUpdated(sign: String)
    fun onRightZAxisUpdated(zAxis: String)
    fun onLeftZAxisUpdated(zAxis: String)// New method to update Z-Axis
    fun onRightShoulderZAxisUpdated(zAxis: String)
    fun onLeftShoulderZAxisUpdated(zAxis: String)
    fun onSpeed(speed: String)
}

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: PoseLandmarkerResult? = null
    private var pointPaint = Paint()
    //    private var linePaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    private var reps = 0
    private var stage = "Start"
    private var sign = ""
    private var quad = 0
    var overlayUpdateListener: OverlayUpdateListener? = null



    private var lastStageChangeTime: Long = 0L
    private val FAST_THRESHOLD_MS = 900L  // Adjust as needed


    // Track previous stage to reset progress indicators when stage changes.
    private var lastStage: String = stage

    // Set this from your Activity or Fragment when an exercise is selected.
    var exerciseEvaluator: ExerciseEvaluator? = null

    init {
        initPaints()
    }

    fun clear() {
        results = null
        pointPaint.reset()
        textPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {

        textPaint.color = Color.WHITE
        textPaint.textSize = 40f
        textPaint.style = Paint.Style.FILL
        textPaint.textAlign = Paint.Align.CENTER
    }

    // Call this method to specify which exercise to detect.
    fun setExerciseType(exercise: String) {
        exerciseEvaluator = when (exercise) {
            "DumbbellPress" -> DumbbellPressEvaluator()
            // Add more cases for additional exercises.
            else -> null
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkerResult ->
            val landmarksList = poseLandmarkerResult.landmarks()
            if (landmarksList.isEmpty()) return
            val landmarkSet = landmarksList[0]
            val points = landmarkSet.map {
                Pair(
                    it.x() * imageWidth * scaleFactor,
                    it.y() * imageHeight * scaleFactor
                )
            }

            exerciseEvaluator?.let { evaluator ->
                val evaluation = evaluator.evaluatePose(points)
                overlayUpdateListener?.onRepsUpdated(evaluation.reps)
                overlayUpdateListener?.onStageUpdated(evaluation.stage)
                overlayUpdateListener?.onSignUpdated(evaluation.feedback)

                // Draw progress indicators based on evaluation.
                evaluation.progressIndicators?.forEach { indicator ->
                    val progressIndicator = ProgressIndicator(
                        x = indicator.x,
                        y = indicator.y,
                        progress = indicator.progress,
                        mainColor = indicator.mainColor,
                        bgColor = indicator.bgColor
                    )
                    progressIndicator.draw(canvas)
                }
            }

//
        }
        // Update lastStage so that indicators are only reset once on stage change.
//        lastStage = stage
    }



    private fun calculatePoseAngles(points: List<Pair<Float, Float>>): Map<String, Float> {
        return mapOf(
            "RHipRShoulderRElbow" to calculateAngle(points[23], points[11], points[13]), // R-Hip, R-Shoulder, R-Elbow Angle
            "RShoulderRElbowRWrist" to calculateAngle(points[11], points[13], points[15]), // R-Shoulder, R-Elbow, R-Wrist Angle
            "LHipLShoulderLElbow" to calculateAngle(points[24], points[12], points[14]), // L-Hip, L-Shoulder, L-Elbow Angle
            "LShoulderLElbowLWrist" to calculateAngle(points[12], points[14], points[16]), // L-Shoulder, L-Elbow, L-Wrist Angle
            "LElbowLShoulderRShoulder" to calculateAngle(points[14], points[12], points[11]), // L-Elbow, L-Shoulder, R-Shoulder Angle
            "RElbowRShoulderLShoulder" to calculateAngle(points[13], points[11], points[12]), // R-Elbow, R-Shoulder, L-Shoulder Angle
        )
    }

    private fun calculateAngle(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>): Float {
        // Convert points to x, y coordinates
        val ax = a.first
        val ay = a.second
        val bx = b.first
        val by = b.second
        val cx = c.first
        val cy = c.second

        // Calculate the angle in radians
        val radians = atan2(cy - by, cx - bx) - atan2(ay - by, ax - bx)

        // Convert radians to degrees
        var angle = abs(radians * 180.0 / Math.PI).toFloat()

        // Ensure the angle is within 0-180 degrees
        if (angle > 180.0f) {
            angle = 360.0f - angle
        }

        return angle
    }

    private fun calculateDistance(pointA: Pair<Float, Float>, pointB: Pair<Float, Float>): Float {
        val deltaX = pointB.first - pointA.first
        val deltaY = pointB.second - pointA.second
        return Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()
    }

    private fun calculateDistanceToCamera(leftShoulder: Pair<Float, Float>, rightShoulder: Pair<Float, Float>): Float {
        // Real-world average shoulder width in cm (adjustable)
        val actualWidth = 40f

        // Approximate focal length of phone camera in pixels (you can calibrate this)
        val focalLength = 600f

        // Calculate the pixel distance between the shoulders
        val pixelWidth = calculateDistance(leftShoulder, rightShoulder)

        // Avoid division by zero
        if (pixelWidth == 0f) return -1f

        // Calculate the distance to the camera
        val distance = (actualWidth * focalLength) / pixelWidth

        return distance
    }



    fun setResults(
        poseLandmarkerResults: PoseLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseLandmarkerResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE, RunningMode.VIDEO -> min(width * 1f / imageWidth, height * 1f / imageHeight)
            RunningMode.LIVE_STREAM -> max(width * 1f / imageWidth, height * 1f / imageHeight)
        }
        invalidate()
    }


}

