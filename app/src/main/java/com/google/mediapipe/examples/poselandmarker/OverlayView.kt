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

//            for (landmark in poseLandmarkerResult.landmarks()) {
//                val points = landmark.map {
//                    Pair(
//                        it.x() * imageWidth * scaleFactor,
//                        it.y() * imageHeight * scaleFactor
//                    )
//                }
//
//                // Use the currently selected evaluator.
//                exerciseEvaluator?.let { evaluator ->
//                    val evaluation = evaluator.evaluatePose(points)
//                    overlayUpdateListener?.onRepsUpdated(evaluation.reps)
//                    overlayUpdateListener?.onStageUpdated(evaluation.stage)
//                    overlayUpdateListener?.onSignUpdated(evaluation.feedback)
//                }
//
//
//
//
//                // This is for the Circle and Landmark Specifications
////                val importantLandmarkIndices = setOf(11, 12, 13, 14, 15, 16)
////
////
////                val leftdistance = calculateDistance(points[14], points[12])
////                val rightdistance = calculateDistance(points[13], points[11])
//////                Log.d("DISTANCE", "$leftdistance")
////                val camDistance = calculateDistanceToCamera(points[11], points[12])
////                overlayUpdateListener?.onLeftZAxisUpdated("$camDistance")
////
////                overlayUpdateListener?.onLeftShoulderZAxisUpdated("$leftdistance")
////                overlayUpdateListener?.onRightShoulderZAxisUpdated("$rightdistance")
//
//
////                val indicators = mutableListOf<ProgressIndicator>()
////                val angles = calculatePoseAngles(points)
////                val leftShoulderAngle = angles["LHipLShoulderLElbow"] ?: 0f
////                val rightShoulderAngle = angles["RHipRShoulderRElbow"] ?: 0f
////                val leftElbowAngle = angles["LShoulderLElbowLWrist"] ?: 0f
////                val rightElbowAngle = angles["RShoulderRElbowRWrist"] ?: 0f
////                val leftShoulderShoulderAngle = angles["LElbowLShoulderRShoulder"] ?: 0f
////                val rightShoulderShoulderAngle = angles["RElbowRShoulderLShoulder"] ?: 0f
////                for (normalizedLandmark in landmark.withIndex()) {
////                    val index = normalizedLandmark.index
////                    if (index in importantLandmarkIndices) {
////                        val x = normalizedLandmark.value.x() * imageWidth * scaleFactor
////                        val y = normalizedLandmark.value.y() * imageHeight * scaleFactor
////
////                        val z = normalizedLandmark.value.z() * imageWidth * scaleFactor
////
////                        // Default progress values
//////                        var progress = 0f
//////                        var color = Color.RED // Default to red
////
////                        var progress = if (stage == "down") 0f else 100f
////                        var color = if (stage == "down") {
////                            if (sign == "Proper") Color.YELLOW else Color.RED
////                        } else {
////                            if (sign == "Proper") Color.GREEN else Color.RED
////                        }
////
////                        // Adjust progress based on angles
////                        if(stage == "down") {
////                            when (index) {
////                                11 -> { // Shoulders
////                                    progress = (rightShoulderAngle / 180f) * 100f
////
////                                }
////                                12 -> { // Shoulders
////                                    progress = (leftShoulderAngle / 180f) * 100f
////                                }
////                                13 -> { // Elbows
////                                    progress = (rightShoulderAngle / 175f) * 100f
////
////                                }
////                                14 -> { // Elbows
////                                    progress = (leftShoulderAngle / 175f) * 100f
////                                }
////                                15 -> { // Wrists
////                                    progress = ((rightShoulderAngle + rightElbowAngle) / 360f) * 100f
////                                }
////                                16 -> { // Wrists
////                                    progress = ((leftShoulderAngle + leftElbowAngle) / 360f) * 100f
////                                }
////
////                            }
////                        } else {
////                            when (index) {
////                                11 -> { // Shoulders
////                                    progress = 125f - (rightShoulderAngle / 180f) * 100f
////                                }
////                                12 -> { // Shoulders
////                                    progress = 125f - (leftShoulderAngle / 180f) * 100f
////                                }
////                                13 -> { // Elbows
////                                    progress = 125f - (rightShoulderAngle / 175f) * 100f
////                                }
////                                14 -> { // Elbows
////                                    progress = 125f - (leftShoulderAngle / 175f) * 100f
////                                }
////                                15 -> { // Wrists
////                                    progress = 125f - ((rightShoulderAngle + rightElbowAngle) / 360f) * 100f
////                                }
////                                16 -> { // Wrists
////                                    progress = 125f - ((leftShoulderAngle + leftElbowAngle) / 360f) * 100f
////                                }
////                            }
////
////                        }
////
////                        // Reset progress if stage has just changed.
////                        if (lastStage != stage) {
////                            progress = 0f
////                        }
////
////                        indicators.add(ProgressIndicator(
////                            x = x,
////                            y = y,
////                            progress = progress.coerceIn(0f, 100f),
////                            mainColor = color,
////                            bgColor = Color.LTGRAY
////                        ))
////                    }
////                }
////                indicators.forEach { it.draw(canvas) }
////
////
////
////
////                quad = if(leftShoulderAngle < 90f &&  rightShoulderAngle < 90){
////                    0
////                } else {
////                    1
////                }
////                if (leftShoulderAngle < 70 && rightShoulderAngle < 70) {
////                    // Transition to "down" only if not already in "down"
////                    if (stage != "down") {
////                        val now = System.currentTimeMillis()
////                        // If the interval from the last stage change is too short, warn the user.
////                        if (lastStageChangeTime != 0L && ((now - lastStageChangeTime) < FAST_THRESHOLD_MS)) {
////                            overlayUpdateListener?.onSpeed("Too Fast")
////                            Log.d("Speed", "Too Fast")
////                        } else {
////                            overlayUpdateListener?.onSpeed("")
////                        }
////                        lastStageChangeTime = now
////                        stage = "down"
////                        overlayUpdateListener?.onStageUpdated(stage)
////                    }
////                }
////
////                if ((leftShoulderAngle > 160 && rightShoulderAngle > 160) && (stage == "down")) {
////                    // Transition to "up" only if currently in "down"
////                    val now = System.currentTimeMillis()
////                    if (lastStageChangeTime != 0L && ((now - lastStageChangeTime) < FAST_THRESHOLD_MS)) {
////                        overlayUpdateListener?.onSpeed("Too Fast")
////                        Log.d("Speed", "Too Fast")
////                    } else {
////                        overlayUpdateListener?.onSpeed("")
////                    }
////                    lastStageChangeTime = now
////                    stage = "up"
////                    reps += 1
////                    overlayUpdateListener?.onStageUpdated(stage)
////                    overlayUpdateListener?.onRepsUpdated(reps)
////                }
////                if(quad == 1) {
////                    if(leftShoulderAngle > 160 && rightShoulderAngle > 160) {
////                        if(leftElbowAngle <= 175 && rightElbowAngle <= 175) {
////                            sign = "Proper"
////
////                            //wrist, elbow and shoulder progress is complete or 100%
////
////                        } else {
////                            sign = "Too High"
////
////                            //wrist, elbow and shoulder progress is red but full in this part because it is too high
////                        }
////                        overlayUpdateListener?.onSignUpdated(sign)
////
////                    } else {
////                        if((leftShoulderShoulderAngle > 90 && leftShoulderShoulderAngle <=160) &&
////                            (rightShoulderShoulderAngle > 90 && rightShoulderShoulderAngle <=160) ) {
////                            if(leftElbowAngle <= 150 && rightElbowAngle <= 150 ) {
////                                sign = "Proper"
////
////                                //wrist, elbow and shoulder progress is complete or 100%
////
////                            } else {
////                                sign = "Too Wide"
////
////                                //wrist, elbow and shoulder progress is red and decreases because too  wide
////                            }
////
////                            overlayUpdateListener?.onSignUpdated(sign)
////                        } else {
////                            sign = "Proper"
////                            overlayUpdateListener?.onSignUpdated(sign)
////                        }
////                    }
////
////                } else {
////
////
////                    if((leftShoulderAngle < 70) && (rightShoulderAngle < 70)) {
////
////                        if (leftElbowAngle < 30 || rightElbowAngle < 30) {
////
////                            if(leftdistance > 220 || rightdistance > 220) {
////                                sign = "Elbows too low and far out"
////                            } else {
////                                sign = "Elbows Too Low"
////                            }
////                            //wrist, elbow and shoulder progress is
////
////                        } else {
////                            if(leftdistance > 220 || rightdistance > 220) {
////                                sign = "Elbows too far out"
////                            } else {
////                                sign = "Proper"
////                            }
////
////                        }
////                    } else {
////                        sign = "Proper"
////                    }
////                    overlayUpdateListener?.onSignUpdated(sign)
////                }
//            }
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

