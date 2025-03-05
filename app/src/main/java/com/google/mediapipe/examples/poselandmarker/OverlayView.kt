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
    fun onZAxisUpdated(zAxis: String)  // New method to update Z-Axis
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
    private var stage = "down"
    private var sign = ""
    private var quad = 0
    var overlayUpdateListener: OverlayUpdateListener? = null

    init {
        initPaints()
    }

    fun clear() {
        results = null
        pointPaint.reset()
//        linePaint.reset()
        textPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
//        linePaint.color = ContextCompat.getColor(context!!, R.color.mp_color_primary)
//        linePaint.strokeWidth = 12f
//        linePaint.style = Paint.Style.STROKE

//        pointPaint.color = Color.RED
//        pointPaint.strokeWidth = 60f
//        pointPaint.style = Paint.Style.FILL

        textPaint.color = Color.WHITE
        textPaint.textSize = 40f
        textPaint.style = Paint.Style.FILL
        textPaint.textAlign = Paint.Align.CENTER
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseLandmarkerResult ->
            for (landmark in poseLandmarkerResult.landmarks()) {
                val points = landmark.map {
                    Pair(
                        it.x() * imageWidth * scaleFactor,
                        it.y() * imageHeight * scaleFactor
                    )
                }

                // Draw landmarks and connections
//                PoseLandmarker.POSE_LANDMARKS.forEach {
//                    canvas.drawLine(
//                        points[it.start()].first,
//                        points[it.start()].second,
//                        points[it.end()].first,
//                        points[it.end()].second,
//                        linePaint
//                    )
//                }

                // This is for the Circle and Landmark Specifications
                val importantLandmarkIndices = setOf(11, 12, 13, 14, 15, 16)

                val indicators = mutableListOf<ProgressIndicator>()
                val angles = calculatePoseAngles(points)
                val leftShoulderAngle = angles["LHipLShoulderLElbow"] ?: 0f
                val rightShoulderAngle = angles["RHipRShoulderRElbow"] ?: 0f
                val leftElbowAngle = angles["LShoulderLElbowLWrist"] ?: 0f
                val rightElbowAngle = angles["RShoulderRElbowRWrist"] ?: 0f
                val leftShoulderShoulderAngle = angles["LElbowLShoulderRShoulder"] ?: 0f
                val rightShoulderShoulderAngle = angles["RElbowRShoulderLShoulder"] ?: 0f

                // Track Z-axis values for forward/backward check
                val leftShoulderZ = landmark[12].z() // Left shoulder
                val rightShoulderZ = landmark[11].z() // Right shoulder
                val leftElbowZ = landmark[14].z() // Left elbow
                val rightElbowZ = landmark[13].z() // Right elbow



                for (normalizedLandmark in landmark.withIndex()) {
                    val index = normalizedLandmark.index
                    if (index in importantLandmarkIndices) {
                        val x = normalizedLandmark.value.x() * imageWidth * scaleFactor
                        val y = normalizedLandmark.value.y() * imageHeight * scaleFactor

                        val z = normalizedLandmark.value.z()

                        // Default progress values
                        var progress = 0f
                        var color = Color.RED // Default to red

                        if (stage == "down") {
                            progress = 0f
                            color = if (sign == "Proper") Color.YELLOW else Color.RED
                        } else if (stage == "up") {
                            progress = 100f
                            color = if (sign == "Proper") Color.GREEN else Color.RED
                        }

                        // Adjust progress based on angles
                        when (index) {
                            11, 12 -> { // Shoulders
                                progress = (leftShoulderAngle / 180f) * 100f
                            }
                            13 -> { // Left Elbow
                                if (quad == 0) {
                                    progress = 50f // If quad == 0, set progress to 50f and skip angle calculation
                                } else {
                                    progress = (leftElbowAngle / 180f) * 100f // Normal angle calculation when quad != 0
                                }
                            }
                            14 -> { // Right Elbow
                                if (quad == 0) {
                                    progress = 50f // If quad == 0, set progress to 50f and skip angle calculation
                                } else {
                                    progress = (rightElbowAngle / 180f) * 100f // Normal angle calculation when quad != 0
                                }
                            }
                            15, 16 -> { // Wrists
                                progress = ((leftShoulderAngle + rightShoulderAngle + leftElbowAngle + rightElbowAngle) / 720f) * 100f
                            }
                            23, 24 -> { // Hips
                                progress = (rightShoulderAngle / 180f) * 100f
                            }
                        }


                        indicators.add(ProgressIndicator(
                            x = x,
                            y = y,
                            progress = progress.coerceIn(0f, 100f),
                            mainColor = color,
                            bgColor = Color.LTGRAY
                        ))
                    }
                }
                indicators.forEach { it.draw(canvas) }

                // Calculate and draw angles



                //Visualize Angles
                leftShoulderAngle?.let {
                    val point = poseLandmarkerResult.landmarks().get(0).get(12) // Example: Left Shoulder (point 12)
                    val x = point.x() * imageWidth * scaleFactor
                    val y = point.y() * imageHeight * scaleFactor + 20 // Adjust Y position by -10
//                    canvas.drawText("Left Shoulder: ${it.toInt()}°", x, y, textPaint)
                }

                rightShoulderAngle?.let {
                    val point = poseLandmarkerResult.landmarks().get(0).get(11) // Example: Right Shoulder (point 11)
                    val x = point.x() * imageWidth * scaleFactor
                    val y = point.y() * imageHeight * scaleFactor + 20 // Adjust Y position by -10
//                    canvas.drawText("Right Shoulder: ${it.toInt()}°", x, y, textPaint)
                }

                leftElbowAngle?.let {
                    val point = poseLandmarkerResult.landmarks().get(0).get(13) // Example: Left Elbow (point 14)
                    val x = point.x() * imageWidth * scaleFactor
                    val y = point.y() * imageHeight * scaleFactor - 10 // Adjust Y position by -10
                    val z = point.z()
                    overlayUpdateListener?.onZAxisUpdated(z.toString())
                    canvas.drawText("Left Elbow: ${z}°", x, y, textPaint)
                }

                rightElbowAngle?.let {
                    val point = poseLandmarkerResult.landmarks().get(0).get(14) // Example: Right Elbow (point 13)
                    val x = point.x() * imageWidth * scaleFactor
                    val y = point.y() * imageHeight * scaleFactor - 10 // Adjust Y position by -10
                    val z = point.z()
                    overlayUpdateListener?.onZAxisUpdated(z.toString())
                    canvas.drawText("Right Elbow: ${z}°", x, y, textPaint)
                }

                rightShoulderShoulderAngle?.let {
                    val point = poseLandmarkerResult.landmarks().get(0).get(11) // Example: Right Shoulder (point 11)
                    val x = point.x() * imageWidth * scaleFactor
                    val y = point.y() * imageHeight * scaleFactor - 10 // Adjust Y position by +10
//                    canvas.drawText("URight Shoulder: ${it.toInt()}°", x, y, textPaint)
                }

                leftElbowAngle?.let {
                    val point = poseLandmarkerResult.landmarks().get(0).get(12) // Example: Left Shoulder (point 12)
                    val x = point.x() * imageWidth * scaleFactor
                    val y = point.y() * imageHeight * scaleFactor - 10 // Adjust Y position by +10
//                    canvas.drawText("ULeft Shoulder: ${it.toInt()}°", x, y, textPaint)
                }

                // Visual feedback for Z-axis values (too forward or too back)
                Log.d("PoseDetection", "Left Shoulder Z: $leftShoulderZ")
                Log.d("PoseDetection", "Right Shoulder Z: $rightShoulderZ")
                Log.d("PoseDetection", "Left Elbow Z: $leftElbowZ")
                Log.d("PoseDetection", "Right Elbow Z: $rightElbowZ")

//                // Left Shoulder Z-axis
//                if (leftShoulderZ > 0.05) { // Threshold for too forward
//                    sign = "Left Shoulder Too Forward"
//                    Log.d("PoseDetection", "Sign: $sign")
//                    overlayUpdateListener?.onZAxisUpdated(sign)
//
//                } else if (leftShoulderZ < -0.05) { // Threshold for too back
//                    sign = "Left Shoulder Too Back"
//                    Log.d("PoseDetection", "Sign: $sign")
//                    overlayUpdateListener?.onZAxisUpdated(sign)
//
//                }

//                // Right Shoulder Z-axis
//                else if (rightShoulderZ > 0.05) { // Threshold for too forward
//                    sign = "Right Shoulder Too Forward"
//                    Log.d("PoseDetection", "Sign: $sign")
//                    overlayUpdateListener?.onZAxisUpdated(sign)
//
//                } else if (rightShoulderZ < -0.05) { // Threshold for too back
//                    sign = "Right Shoulder Too Back"
//                    Log.d("PoseDetection", "Sign: $sign")
//                    overlayUpdateListener?.onZAxisUpdated(sign)
//
//                }

                // Left Elbow Z-axis
                if (leftElbowZ > 0.05) { // Threshold for too forward
                    sign = "Left Elbow Too Forward"
                    Log.d("PoseDetection", "Sign: $sign")
                    overlayUpdateListener?.onZAxisUpdated(sign)
                    overlayUpdateListener?.onSignUpdated("Left Elbow Too Forward")

                } else if (leftElbowZ < -0.05) { // Threshold for too back
                    sign = "Left Elbow Too Back"
                    Log.d("PoseDetection", "Sign: $sign")
                    overlayUpdateListener?.onZAxisUpdated(sign)
                    overlayUpdateListener?.onSignUpdated("Left Elbow Too Back")
                }

                // Right Elbow Z-axis
                else if (rightElbowZ > 0.05) { // Threshold for too forward
                    sign = "Right Elbow Too Forward"
                    Log.d("PoseDetection", "Sign: $sign")
                    overlayUpdateListener?.onZAxisUpdated(sign)
                    overlayUpdateListener?.onSignUpdated("Right Elbow Too Forward")

                } else if (rightElbowZ < -0.05) { // Threshold for too back
                    sign = "Right Elbow Too Back"
                    Log.d("PoseDetection", "Sign: $sign")
                    overlayUpdateListener?.onZAxisUpdated(sign)
                    overlayUpdateListener?.onSignUpdated("Right Elbow Too Back")
                }

                if (leftShoulderAngle != null) {
                    if (rightShoulderAngle != null) {
                        if(leftShoulderAngle < 90f &&  rightShoulderAngle < 90){
                            quad = 0
                        } else {
                            quad = 1
                        }
                        if(leftShoulderAngle < 70 && rightShoulderAngle < 70) {
                            stage = "down"

                            //wrist, elbow and shoulder progress is zero or low during this part

                            overlayUpdateListener?.onStageUpdated(stage)
                        }

                        if((leftShoulderAngle > 160 && rightShoulderAngle > 160) && (stage == "down")) {
                            stage = "up"

                            //wrist, elbow and shoulder progress is complete or 100%

                            reps += 1
                            overlayUpdateListener?.onStageUpdated(stage)
                            overlayUpdateListener?.onRepsUpdated(reps)
                        }
                        if(quad == 1) {
                            if(leftShoulderAngle > 160 && rightShoulderAngle > 160) {
                                if (leftElbowAngle != null) {
                                    if (rightElbowAngle != null) {
                                        if(leftElbowAngle <= 165 && rightElbowAngle <= 165) {
                                            sign = "Proper"

                                            //wrist, elbow and shoulder progress is complete or 100%

                                        } else {
                                            sign = "Too High"

                                            //wrist, elbow and shoulder progress is red but full in this part because it is too high
                                        }
                                        overlayUpdateListener?.onSignUpdated(sign)
                                    }
                                }

                            } else if (leftShoulderShoulderAngle != null) {
                                if (rightShoulderShoulderAngle != null) {
                                    if((leftShoulderShoulderAngle > 90 && leftShoulderShoulderAngle <=160) &&
                                        (rightShoulderShoulderAngle > 90 && rightShoulderShoulderAngle <=160) ) {
                                        if (leftElbowAngle != null) {
                                            if (rightElbowAngle != null) {
                                                if(leftElbowAngle <= 150 && rightElbowAngle <= 150 ) {
                                                    sign = "Proper"

                                                    //wrist, elbow and shoulder progress is complete or 100%

                                                } else {
                                                    sign = "Too Wide"

                                                    //wrist, elbow and shoulder progress is red and decreases because too  wide
                                                }

                                                overlayUpdateListener?.onSignUpdated(sign)
                                            }
                                        }
                                    } else {
                                        sign = ""
                                        overlayUpdateListener?.onSignUpdated(sign)
                                    }
                                }
                            }

                        } else {
                            if((leftShoulderAngle < 70) && (rightShoulderAngle < 70)) {
                                if (leftShoulderAngle < 30 && rightShoulderAngle < 30) {
                                    sign = "Arms Too Low"

                                    //wrist, elbow and shoulder progress is

                                } else {
                                    sign = "Proper"

                                    pointPaint.color = Color.YELLOW
                                    pointPaint.strokeWidth = 60f
                                    pointPaint.style = Paint.Style.FILL
                                }
                            } else {
                                sign = ""
                            }
                            overlayUpdateListener?.onSignUpdated(sign)
                        }
                    }
                }
//                // Track and update reps/stage/sign
//                leftElbowAngle?.let { updateRepsAndStage(it) }
//                leftElbowAngle?.let { updateSign(it) }
            }
        }
    }

    private fun updateRepsAndStage(elbowAngle: Float) {
        val upThreshold = 45f
        val downThreshold = 160f

        if (elbowAngle <= upThreshold && stage == "down") {
            stage = "up"
            overlayUpdateListener?.onStageUpdated(stage)
        } else if (elbowAngle >= downThreshold && stage == "up") {
            stage = "down"
            reps++
            overlayUpdateListener?.onRepsUpdated(reps)
            overlayUpdateListener?.onStageUpdated(stage)
        }
    }

    private fun updateSign(elbowAngle: Float) {
        val sign = if (elbowAngle in 80f..100f) {
            "Good form!"
        } else {
            "Adjust your posture!"
        }
        overlayUpdateListener?.onSignUpdated(sign)
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

    private inner class ProgressIndicator(
        var x: Float = 0f,
        var y: Float = 0f,
        var progress: Float = 0f,
        var mainColor: Int = Color.BLUE,
        var bgColor: Int = Color.LTGRAY
    ) {
        private val strokeWidth = 12f
        private val radius = 32f

        fun draw(canvas: Canvas) {
            // Draw background circle
            val bgPaint = Paint().apply {
                color = bgColor
                style = Paint.Style.STROKE
                strokeWidth = this@ProgressIndicator.strokeWidth
                isAntiAlias = true
            }
            canvas.drawCircle(x, y, radius, bgPaint)

            // Draw progress arc
            val progressPaint = Paint().apply {
                color = mainColor
                style = Paint.Style.STROKE
                strokeWidth = this@ProgressIndicator.strokeWidth
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }
            val rect = RectF(x - radius, y - radius, x + radius, y + radius)
            canvas.drawArc(rect, -90f, 360 * (progress / 100f), false, progressPaint)
        }
    }
}

