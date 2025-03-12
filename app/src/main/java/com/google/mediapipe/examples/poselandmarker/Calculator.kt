package com.google.mediapipe.examples.poselandmarker

import kotlin.math.abs
import kotlin.math.atan2

class Calculator {

    // Helper to calculate the angle at point b given points a, b, and c.
    fun calculateAngle(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>): Float {
        val radians = atan2(c.second - b.second, c.first - b.first) - atan2(a.second - b.second, a.first - b.first)
        var angle = abs(radians * 180.0 / Math.PI).toFloat()
        if (angle > 180f) angle = 360f - angle
        return angle
    }

    fun calculateDistance(pointA: Pair<Float, Float>, pointB: Pair<Float, Float>): Float {
        val deltaX = pointB.first - pointA.first
        val deltaY = pointB.second - pointA.second
        return Math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()
    }
}