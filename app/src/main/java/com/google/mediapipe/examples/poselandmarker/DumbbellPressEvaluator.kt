package com.google.mediapipe.examples.poselandmarker

import android.graphics.Color

class DumbbellPressEvaluator : ExerciseEvaluator {
    private var reps = 0
    private var stage = "Start"  // starting in the "up" position
    private var quad = 0
    private var feedback = ""
    private var warning = ""

    private var lastStageChangeTime: Long = 0L
    private val FAST_THRESHOLD_MS = 900L  // Adjust as needed
    private var lastStage: String = stage
    private val calculator = Calculator()

    override fun evaluatePose(points: List<Pair<Float, Float>>): ExerciseEvaluationResult {
        // Ensure the list is large enough.
        if (points.size < 16) return ExerciseEvaluationResult(reps, stage, "Insufficient landmarks", warning, null)


        val leftDistance = calculator.calculateDistance(points[14], points[12])
        val rightDistance = calculator.calculateDistance(points[13], points[11])

        val rightHip =  points[23]
        val leftHip = points[24]
        val rightShoulder = points[11]
        val leftShoulder = points[12]
        val rightElbow = points[13]
        val leftElbow = points[14]
        val rightWrist = points[15]
        val leftWrist = points[16]

        val leftShoulderAngle = calculator.calculateAngle(leftHip, leftShoulder, leftElbow) // L-Hip, L-Shoulder, L-Elbow Angle
        val rightShoulderAngle = calculator.calculateAngle(rightHip, rightShoulder, rightElbow) // R-Hip, R-Shoulder, R-Elbow Angle
        val leftElbowAngle = calculator.calculateAngle(leftShoulder, leftElbow, leftWrist) // L-Shoulder, L-Elbow, L-Wrist Angle
        val rightElbowAngle = calculator.calculateAngle(rightShoulder, rightElbow, rightWrist) // R-Shoulder, R-Elbow, R-Wrist Angle
        val leftShoulderShoulderAngle = calculator.calculateAngle(leftElbow, leftShoulder, rightShoulder) // L-Elbow, L-Shoulder, R-Shoulder Angle
        val rightShoulderShoulderAngle = calculator.calculateAngle(rightElbow, rightShoulder, leftShoulder) // R-Elbow, R-Shoulder, L-Shoulder Angle


        quad = if(leftShoulderAngle < 90f &&  rightShoulderAngle < 90){
            0
        } else {
            1
        }

        if (leftShoulderAngle < 70 && rightShoulderAngle < 70) {
            // Transition to "down" only if not already in "down"
            if (stage != "down") {
                val now = System.currentTimeMillis()
                // If the interval from the last stage change is too short, warn the user.
                warning = if (lastStageChangeTime != 0L && ((now - lastStageChangeTime) < FAST_THRESHOLD_MS)) {
                    "Too Fast"
            //                    Log.d("Speed", "Too Fast")
                } else {
                    ""
                }
                lastStageChangeTime = now
                stage = "down"
            }
        }

        if ((leftShoulderAngle > 160 && rightShoulderAngle > 160) && (stage == "down")) {
            // Transition to "up" only if currently in "down"
            val now = System.currentTimeMillis()
            warning = if (lastStageChangeTime != 0L && ((now - lastStageChangeTime) < FAST_THRESHOLD_MS)) {
                "Too Fast"
        //                Log.d("Speed", "Too Fast")
            } else {
                ""
            }
            lastStageChangeTime = now
            stage = "up"
            reps += 1
        }

        if(quad == 1) {
            if(leftShoulderAngle > 160 && rightShoulderAngle > 160) {
                if(leftElbowAngle <= 175 && rightElbowAngle <= 175) {
                    feedback = "Proper"

                    //wrist, elbow and shoulder progress is complete or 100%

                }

            } else {
                if((leftShoulderShoulderAngle > 90 && leftShoulderShoulderAngle <=160) &&
                    (rightShoulderShoulderAngle > 90 && rightShoulderShoulderAngle <=160) ) {
                    if(leftElbowAngle <= 150 && rightElbowAngle <= 150 ) {
                        feedback = "Proper"

                        //wrist, elbow and shoulder progress is complete or 100%

                    } else {
                        feedback = "Too Wide"

                        //wrist, elbow and shoulder progress is red and decreases because too  wide
                    }

                } else {
                    feedback = "Proper"
                }
            }

        } else {


            if((leftShoulderAngle < 70) && (rightShoulderAngle < 70)) {


                    if(leftDistance > 220 || rightDistance > 220) {
                        feedback = "Elbows too far out"
                    } else {
                        feedback = "Proper"
                    }
            } else {
                feedback = "Proper"
            }
        }
        lastStage = stage

        var rightShoulderProgress: Float
        var leftShoulderProgress: Float
        var rightElbowProgress: Float
        var leftElbowProgress: Float
        var rightWristProgress: Float
        var leftWristProgress: Float

        if(stage == "down") {
            rightShoulderProgress = (rightShoulderAngle / 180f) * 100f
            leftShoulderProgress = (leftShoulderAngle / 180f) * 100f
            rightElbowProgress = (rightShoulderAngle / 175f) * 100f
            leftElbowProgress = (leftShoulderAngle / 175f) * 100f
            rightWristProgress = ((rightShoulderAngle + rightElbowAngle) / 360f) * 100f
            leftWristProgress = ((leftShoulderAngle + leftElbowAngle) / 360f) * 100f
        } else {
            rightShoulderProgress = 125f - (rightShoulderAngle / 180f) * 100f
            leftShoulderProgress = 125f - (leftShoulderAngle / 180f) * 100f
            rightElbowProgress = 125f - (rightShoulderAngle / 175f) * 100f
            leftElbowProgress = 125f - (leftShoulderAngle / 175f) * 100f
            rightWristProgress = 125f - ((rightShoulderAngle + rightElbowAngle) / 360f) * 100f
            leftWristProgress = 125f - ((leftShoulderAngle + leftElbowAngle) / 360f) * 100f
        }

        if (lastStage != stage) {
            rightShoulderProgress = 0f
            leftShoulderProgress = 0f
            rightElbowProgress = 0f
            leftElbowProgress = 0f
            rightWristProgress = 0f
            leftWristProgress = 0f
        }

        val color = if (stage == "down") {
            if (feedback == "Proper") Color.YELLOW else Color.RED
        } else {
            if (feedback == "Proper") Color.GREEN else Color.RED
        }

        val indicators = listOf(
            ProgressIndicatorData(
                x = rightShoulder.first,
                y = rightShoulder.second,
                progress = rightShoulderProgress,
                mainColor = color,
                bgColor = Color.LTGRAY),
            ProgressIndicatorData(
                x = leftShoulder.first,
                y = leftShoulder.second,
                progress = leftShoulderProgress,
                mainColor = color,
                bgColor = Color.LTGRAY),
            ProgressIndicatorData(
                x = rightElbow.first,
                y = rightElbow.second,
                progress = rightElbowProgress,
                mainColor = color,
                bgColor = Color.LTGRAY),
            ProgressIndicatorData(
                x = leftElbow.first,
                y = leftElbow.second,
                progress = leftElbowProgress,
                mainColor = color,
                bgColor = Color.LTGRAY),
            ProgressIndicatorData(
                x = rightWrist.first,
                y = rightWrist.second,
                progress = rightWristProgress,
                mainColor = color,
                bgColor = Color.LTGRAY),
            ProgressIndicatorData(
                x = leftWrist.first,
                y = leftWrist.second,
                progress = leftWristProgress,
                mainColor = color,
                bgColor = Color.LTGRAY)
        )




        return ExerciseEvaluationResult(reps, stage, feedback, warning, indicators)
    }


}