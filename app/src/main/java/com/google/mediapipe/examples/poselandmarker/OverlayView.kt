
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private lateinit var interpreter: Interpreter
    private var keypoints: Array<FloatArray>? = null

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    private val pointPaint = Paint()
    private val linePaint = Paint()

    init {
        initPaints()
        loadModel()
    }

    private fun loadModel() {
        val model = FileUtil.loadMappedFile(context, "movenet-tflite-singlepose-lightning-v1.tflite")
        interpreter = Interpreter(model)
    }

    private fun initPaints() {
        linePaint.color = Color.RED
        linePaint.strokeWidth = 5f
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.GREEN
        pointPaint.strokeWidth = 10f
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        keypoints?.let { kp ->
            drawKeypoints(canvas, kp)
            drawConnections(canvas, kp)
        }
    }

    private fun drawKeypoints(canvas: Canvas, keypoints: Array<FloatArray>) {
        for (kp in keypoints) {
            val x = kp[1] * imageWidth * scaleFactor
            val y = kp[0] * imageHeight * scaleFactor
            val confidence = kp[2]

            if (confidence > 0.4) {  // Confidence threshold (same as in Python)
                canvas.drawCircle(x, y, 5f, pointPaint)
            }
        }
    }

    private fun drawConnections(canvas: Canvas, keypoints: Array<FloatArray>) {
        val edges = listOf(
            Pair(0, 1), Pair(0, 2), Pair(1, 3), Pair(2, 4), Pair(0, 5), Pair(0, 6),
            Pair(5, 7), Pair(7, 9), Pair(6, 8), Pair(8, 10), Pair(5, 6), Pair(5, 11),
            Pair(6, 12), Pair(11, 12), Pair(11, 13), Pair(13, 15), Pair(12, 14), Pair(14, 16)
        )

        for (edge in edges) {
            val p1 = keypoints[edge.first]
            val p2 = keypoints[edge.second]
            val confidence1 = p1[2]
            val confidence2 = p2[2]

            if (confidence1 > 0.4 && confidence2 > 0.4) {
                val x1 = p1[1] * imageWidth * scaleFactor
                val y1 = p1[0] * imageHeight * scaleFactor
                val x2 = p2[1] * imageWidth * scaleFactor
                val y2 = p2[0] * imageHeight * scaleFactor
                canvas.drawLine(x1, y1, x2, y2, linePaint)
            }
        }
    }

    fun setResults(keypoints: Array<FloatArray>, imageHeight: Int, imageWidth: Int) {
        this.keypoints = keypoints
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        scaleFactor = min(width * 1f / imageWidth, height * 1f / imageHeight)
        invalidate()
    }

    fun makeDetections(frame: ByteArray) {
        val inputBuffer = ByteBuffer.allocateDirect(4 * 192 * 192 * 3).order(ByteOrder.nativeOrder())
        val img = frame.map { it.toFloat() / 255f }.toFloatArray()
        inputBuffer.asFloatBuffer().put(img)

        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 17, 3), org.tensorflow.lite.DataType.FLOAT32)
        interpreter.run(inputBuffer, outputBuffer.buffer)

        val result = outputBuffer.floatArray

        val keypoints = Array(17) { FloatArray(3) }  // Array of keypoints (x, y, confidence)
        for (i in result.indices step 3) {
            keypoints[i / 3] = floatArrayOf(result[i], result[i + 1], result[i + 2])
        }

        setResults(keypoints, height, width)
    }
}


// --------------------------------------------------------------------
//
//package com.google.mediapipe.examples.poselandmarker
//
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.util.AttributeSet
//import android.view.View
//import androidx.core.content.ContextCompat
//import com.google.mediapipe.tasks.vision.core.RunningMode
//import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
//import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
//import kotlin.math.abs
//import kotlin.math.atan2
//import kotlin.math.max
//import kotlin.math.min
//
//// Callback interface for updating the UI
//interface OverlayUpdateListener {
//    fun onRepsUpdated(reps: Int)
//    fun onStageUpdated(stage: String)
//    fun onSignUpdated(sign: String)
//}
//
//class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
//
//    private var results: PoseLandmarkerResult? = null
//    private var pointPaint = Paint()
//    private var linePaint = Paint()
//    private var textPaint = Paint()
//
//    private var scaleFactor: Float = 1f
//    private var imageWidth: Int = 1
//    private var imageHeight: Int = 1
//
//    private var reps = 0
//    private var stage = "down"
//    private var sign = ""
//    private var quad = 0
//    var overlayUpdateListener: OverlayUpdateListener? = null
//
//    init {
//        initPaints()
//    }
//
//    fun clear() {
//        results = null
//        pointPaint.reset()
//        linePaint.reset()
//        textPaint.reset()
//        invalidate()
//        initPaints()
//    }
//
//    private fun initPaints() {
//        linePaint.color = ContextCompat.getColor(context!!, R.color.mp_color_primary)
//        linePaint.strokeWidth = 12f
//        linePaint.style = Paint.Style.STROKE
//
//        pointPaint.color = Color.YELLOW
//        pointPaint.strokeWidth = 12f
//        pointPaint.style = Paint.Style.FILL
//
//        textPaint.color = Color.WHITE
//        textPaint.textSize = 40f
//        textPaint.style = Paint.Style.FILL
//        textPaint.textAlign = Paint.Align.CENTER
//    }
//
//    override fun draw(canvas: Canvas) {
//        super.draw(canvas)
//        results?.let { poseLandmarkerResult ->
//            for (landmark in poseLandmarkerResult.landmarks()) {
//                val points = landmark.map {
//                    Pair(
//                        it.x() * imageWidth * scaleFactor,
//                        it.y() * imageHeight * scaleFactor
//                    )
//                }
//
//                // Draw landmarks and connections
//                PoseLandmarker.POSE_LANDMARKS.forEach {
//                    canvas.drawLine(
//                        points[it.start()].first,
//                        points[it.start()].second,
//                        points[it.end()].first,
//                        points[it.end()].second,
//                        linePaint
//                    )
//                }
//
//                for (normalizedLandmark in landmark) {
//                    canvas.drawPoint(
//                        normalizedLandmark.x() * imageWidth * scaleFactor,
//                        normalizedLandmark.y() * imageHeight * scaleFactor,
//                        pointPaint
//                    )
//                }
//
//                // Calculate and draw angles
//                val angles = calculatePoseAngles(points)
//                val leftShoulderAngle = angles["LHipLShoulderLElbow"]
//                val rightShoulderAngle = angles["RHipRShoulderRElbow"]
//                val leftElbowAngle = angles["LShoulderLElbowLWrist"]
//                val rightElbowAngle = angles["RShoulderRElbowRWrist"]
//                val leftShoulderShoulderAngle = angles["LElbowLShoulderRShoulder"]
//                val rightShoulderShoulderAngle = angles["RElbowRShoulderLShoulder"]
//
//                //Visualize Angles
//                leftShoulderAngle?.let {
//                    val point = poseLandmarkerResult.landmarks().get(0).get(12) // Example: Left Shoulder (point 12)
//                    val x = point.x() * imageWidth * scaleFactor
//                    val y = point.y() * imageHeight * scaleFactor + 20 // Adjust Y position by -10
//                    canvas.drawText("Left Shoulder: ${it.toInt()}°", x, y, textPaint)
//                }
//
//                rightShoulderAngle?.let {
//                    val point = poseLandmarkerResult.landmarks().get(0).get(11) // Example: Right Shoulder (point 11)
//                    val x = point.x() * imageWidth * scaleFactor
//                    val y = point.y() * imageHeight * scaleFactor + 20 // Adjust Y position by -10
//                    canvas.drawText("Right Shoulder: ${it.toInt()}°", x, y, textPaint)
//                }
//
//                leftElbowAngle?.let {
//                    val point = poseLandmarkerResult.landmarks().get(0).get(14) // Example: Left Elbow (point 14)
//                    val x = point.x() * imageWidth * scaleFactor
//                    val y = point.y() * imageHeight * scaleFactor - 10 // Adjust Y position by -10
//                    canvas.drawText("Left Elbow: ${it.toInt()}°", x, y, textPaint)
//                }
//
//                rightElbowAngle?.let {
//                    val point = poseLandmarkerResult.landmarks().get(0).get(13) // Example: Right Elbow (point 13)
//                    val x = point.x() * imageWidth * scaleFactor
//                    val y = point.y() * imageHeight * scaleFactor - 10 // Adjust Y position by -10
//                    canvas.drawText("Right Elbow: ${it.toInt()}°", x, y, textPaint)
//                }
//
//                rightShoulderShoulderAngle?.let {
//                    val point = poseLandmarkerResult.landmarks().get(0).get(11) // Example: Right Shoulder (point 11)
//                    val x = point.x() * imageWidth * scaleFactor
//                    val y = point.y() * imageHeight * scaleFactor - 10 // Adjust Y position by +10
//                    canvas.drawText("URight Shoulder: ${it.toInt()}°", x, y, textPaint)
//                }
//
//                leftElbowAngle?.let {
//                    val point = poseLandmarkerResult.landmarks().get(0).get(12) // Example: Left Shoulder (point 12)
//                    val x = point.x() * imageWidth * scaleFactor
//                    val y = point.y() * imageHeight * scaleFactor - 10 // Adjust Y position by +10
//                    canvas.drawText("ULeft Shoulder: ${it.toInt()}°", x, y, textPaint)
//                }
//
//                if (leftShoulderAngle != null) {
//                    if (rightShoulderAngle != null) {
//                        if(leftShoulderAngle < 90f &&  rightShoulderAngle < 90){
//                            quad = 0
//                        } else {
//                            quad = 1
//                        }
//                        if(leftShoulderAngle < 70 && rightShoulderAngle < 70) {
//                            stage = "down"
//                            overlayUpdateListener?.onStageUpdated(stage)
//                        }
//
//                        if((leftShoulderAngle > 160 && rightShoulderAngle > 160) && (stage == "down")) {
//                            stage = "up"
//                            reps += 1
//                            overlayUpdateListener?.onStageUpdated(stage)
//                            overlayUpdateListener?.onRepsUpdated(reps)
//                        }
//                        if(quad == 1) {
//                            if(leftShoulderAngle > 160 && rightShoulderAngle > 160) {
//                                if (leftElbowAngle != null) {
//                                    if (rightElbowAngle != null) {
//                                        if(leftElbowAngle <= 165 && rightElbowAngle <= 165) {
//                                            sign = "Proper"
//                                        } else {
//                                            sign = "Too High"
//                                        }
//                                        overlayUpdateListener?.onSignUpdated(sign)
//                                    }
//                                }
//
//                            } else if (leftShoulderShoulderAngle != null) {
//                                if (rightShoulderShoulderAngle != null) {
//                                    if((leftShoulderShoulderAngle > 90 && leftShoulderShoulderAngle <=160) &&
//                                        (rightShoulderShoulderAngle > 90 && rightShoulderShoulderAngle <=160) ) {
//                                        if (leftElbowAngle != null) {
//                                            if (rightElbowAngle != null) {
//                                                if(leftElbowAngle <= 150 && rightElbowAngle <= 150 ) {
//                                                    sign = "Proper"
//                                                } else {
//                                                    sign = "Too Wide"
//                                                }
//
//                                                overlayUpdateListener?.onSignUpdated(sign)
//                                            }
//                                        }
//                                    } else {
//                                        sign = ""
//                                        overlayUpdateListener?.onSignUpdated(sign)
//                                    }
//                                }
//                            }
//
//                        } else {
//                            if((leftShoulderAngle < 70) && (rightShoulderAngle < 70)) {
//                                if (leftShoulderAngle < 30 && rightShoulderAngle < 30) {
//                                    sign = "Arms Too Low"
//                                } else {
//                                    sign = "Proper"
//                                }
//                            } else {
//                                sign = ""
//                            }
//                            overlayUpdateListener?.onSignUpdated(sign)
//                        }
//                    }
//                }
////                // Track and update reps/stage/sign
////                leftElbowAngle?.let { updateRepsAndStage(it) }
////                leftElbowAngle?.let { updateSign(it) }
//            }
//        }
//    }
//
//    private fun updateRepsAndStage(elbowAngle: Float) {
//        val upThreshold = 45f
//        val downThreshold = 160f
//
//        if (elbowAngle <= upThreshold && stage == "down") {
//            stage = "up"
//            overlayUpdateListener?.onStageUpdated(stage)
//        } else if (elbowAngle >= downThreshold && stage == "up") {
//            stage = "down"
//            reps++
//            overlayUpdateListener?.onRepsUpdated(reps)
//            overlayUpdateListener?.onStageUpdated(stage)
//        }
//    }
//
//    private fun updateSign(elbowAngle: Float) {
//        val sign = if (elbowAngle in 80f..100f) {
//            "Good form!"
//        } else {
//            "Adjust your posture!"
//        }
//        overlayUpdateListener?.onSignUpdated(sign)
//    }
//
//    private fun calculatePoseAngles(points: List<Pair<Float, Float>>): Map<String, Float> {
//        return mapOf(
//            "RHipRShoulderRElbow" to calculateAngle(points[23], points[11], points[13]), // R-Hip, R-Shoulder, R-Elbow Angle
//            "RShoulderRElbowRWrist" to calculateAngle(points[11], points[13], points[15]), // R-Shoulder, R-Elbow, R-Wrist Angle
//            "LHipLShoulderLElbow" to calculateAngle(points[24], points[12], points[14]), // L-Hip, L-Shoulder, L-Elbow Angle
//            "LShoulderLElbowLWrist" to calculateAngle(points[12], points[14], points[16]), // L-Shoulder, L-Elbow, L-Wrist Angle
//            "LElbowLShoulderRShoulder" to calculateAngle(points[14], points[12], points[11]), // L-Elbow, L-Shoulder, R-Shoulder Angle
//            "RElbowRShoulderLShoulder" to calculateAngle(points[13], points[11], points[12]), // R-Elbow, R-Shoulder, L-Shoulder Angle
//        )
//    }
//
//    private fun calculateAngle(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>): Float {
//        // Convert points to x, y coordinates
//        val ax = a.first
//        val ay = a.second
//        val bx = b.first
//        val by = b.second
//        val cx = c.first
//        val cy = c.second
//
//        // Calculate the angle in radians
//        val radians = atan2(cy - by, cx - bx) - atan2(ay - by, ax - bx)
//
//        // Convert radians to degrees
//        var angle = abs(radians * 180.0 / Math.PI).toFloat()
//
//        // Ensure the angle is within 0-180 degrees
//        if (angle > 180.0f) {
//            angle = 360.0f - angle
//        }
//
//        return angle
//    }
//
//    fun setResults(
//        poseLandmarkerResults: PoseLandmarkerResult,
//        imageHeight: Int,
//        imageWidth: Int,
//        runningMode: RunningMode = RunningMode.IMAGE
//    ) {
//        results = poseLandmarkerResults
//        this.imageHeight = imageHeight
//        this.imageWidth = imageWidth
//
//        scaleFactor = when (runningMode) {
//            RunningMode.IMAGE, RunningMode.VIDEO -> min(width * 1f / imageWidth, height * 1f / imageHeight)
//            RunningMode.LIVE_STREAM -> max(width * 1f / imageWidth, height * 1f / imageHeight)
//        }
//        invalidate()
//    }
//}
