package com.google.mediapipe.examples.poselandmarker

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class ProgressIndicator(
    var x: Float = 0f,
    var y: Float = 0f,
    var progress: Float = 0f,
    var mainColor: Int = Color.BLUE,   // Fill color
    var bgColor: Int = Color.LTGRAY      // Outline color
) {
    private val strokeWidth = 10f // Outline thickness
    private val radius = 48f      // Outer circle radius

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.STROKE
        this.strokeWidth = this@ProgressIndicator.strokeWidth
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = mainColor
        style = Paint.Style.FILL
    }

    fun draw(canvas: Canvas) {
        // Draw the outer circle (static outline)
        canvas.drawCircle(x, y, radius, outlinePaint)
        // Calculate the inner circle radius based on progress (0-100%)
        val innerRadius = radius * (progress / 100f)
        // Draw the inner filled circle
        canvas.drawCircle(x, y, innerRadius, fillPaint)
    }
}