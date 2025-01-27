package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class PoseOverlayView(
    context: Context,
    private val landmarks: List<NormalizedLandmark>,
    private val angles: Map<String, Float>
) : View(context) {

    private val linePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawPose(canvas)
    }

    private fun drawPose(canvas: Canvas) {
        // Draw key points and angles
        val points = landmarks.map { Pair(it.x() * width, it.y() * height) }

        // Example: Draw angle at the left elbow
        drawAngleLabel(canvas, points[11], angles["leftShoulderAngle"] ?: 0f) // Left Shoulder
        drawAngleLabel(canvas, points[13], angles["leftElbowAngle"] ?: 0f) // Left Elbow
        drawAngleLabel(canvas, points[15], angles["leftWristAngle"] ?: 0f) // Left Wrist

        // Example: Draw angle at the right elbow
        drawAngleLabel(canvas, points[12], angles["rightShoulderAngle"] ?: 0f) // Right Shoulder
        drawAngleLabel(canvas, points[14], angles["rightElbowAngle"] ?: 0f) // Right Elbow
        drawAngleLabel(canvas, points[16], angles["rightWristAngle"] ?: 0f) // Right Wrist

        // Draw lines connecting key points
        drawLineBetween(canvas, points[23], points[11]) // Left Hip to Left Shoulder
        drawLineBetween(canvas, points[11], points[13]) // Left Shoulder to Left Elbow
        drawLineBetween(canvas, points[13], points[15]) // Left Elbow to Left Wrist

        drawLineBetween(canvas, points[24], points[12]) // Right Hip to Right Shoulder
        drawLineBetween(canvas, points[12], points[14]) // Right Shoulder to Right Elbow
        drawLineBetween(canvas, points[14], points[16]) // Right Elbow to Right Wrist
    }

    private fun drawLineBetween(canvas: Canvas, start: Pair<Float, Float>, end: Pair<Float, Float>) {
        canvas.drawLine(start.first, start.second, end.first, end.second, linePaint)
    }

    private fun drawAngleLabel(canvas: Canvas, point: Pair<Float, Float>, angle: Float) {
        canvas.drawText("%.1f°".format(angle), point.first, point.second - 10, textPaint)
    }
}
