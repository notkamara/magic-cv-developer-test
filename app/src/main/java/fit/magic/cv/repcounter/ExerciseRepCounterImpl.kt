// Copyright (c) 2024 Magic Tech Ltd

package fit.magic.cv.repcounter

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import fit.magic.cv.PoseLandmarkerHelper
import kotlin.math.atan2

class ExerciseRepCounterImpl : ExerciseRepCounter() {

    // Variables to track previous states and progress
    private var isLunging = false
    private var lungeProgress = 0.0

    override fun setResults(resultBundle: PoseLandmarkerHelper.ResultBundle) {
        // Extract necessary landmarks from the result bundle
        val results = resultBundle.results

        if (results[0].landmarks().size > 0) {
            val leftKnee = results[0].landmarks()[0][25]
            val leftHip = results[0].landmarks()[0][23]
            val leftAnkle = results[0].landmarks()[0][27]

            val rightKnee = results[0].landmarks()[0][26]
            val rightHip = results[0].landmarks()[0][24]
            val rightAnkle = results[0].landmarks()[0][28]

            // Calculate the angles of both legs
            val leftKneeAngle = calculateAngle(leftHip, leftKnee, leftAnkle)
            val rightKneeAngle = calculateAngle(rightHip, rightKnee, rightAnkle)

            println(leftKneeAngle)

            // Update progress based on lunge depth (e.g., knee angle close to 90 degrees)
            updateProgress(leftKneeAngle, rightKneeAngle)

            // Check if a full repetition (lunge) has been completed
            if (isLungeCompleted(leftKneeAngle, rightKneeAngle)) {
                incrementRepCount() // Increment the repetition counter
                isLunging = false // Reset lunge state
            }
        } else {
            println("No detections")
        }
    }

    // Calculates the angle between three points (e.g., hip, knee, ankle)
    private fun calculateAngle(p1: NormalizedLandmark, p2: NormalizedLandmark, p3: NormalizedLandmark): Float {
        val angle = Math.toDegrees(
            (atan2(p3.y() - p2.y(), p3.x() - p2.x()) - atan2(p1.y() - p2.y(), p1.x() - p2.x())).toDouble()
        )
        return if (angle < 0) (angle + 360).toFloat() else angle.toFloat()
    }

    // Determines if a lunge is completed based on knee angles
    private fun isLungeCompleted(leftKneeAngle: Float, rightKneeAngle: Float): Boolean {
        // Criteria for lunge completion: Knee angle should be close to 90 degrees for a deep lunge
        val lungeThreshold = 100.0 // Adjustable threshold for knee angle
        return (leftKneeAngle in 85.0..lungeThreshold || rightKneeAngle in 85.0..lungeThreshold) && isLunging
    }

    // Updates the progress of the current lunge and sends a progress update
    private fun updateProgress(leftKneeAngle: Float, rightKneeAngle: Float) {
        // Example: Progress is based on how close the knee angle is to 90 degrees
        val maxAngle = 160.0 // Adjustable max angle for standing
        val minAngle = 85.0 // Adjustable min angle for deep lunge

        // Normalize progress to a 0-1 scale
        val progress = when {
            leftKneeAngle in minAngle..maxAngle -> (maxAngle - leftKneeAngle) / (maxAngle - minAngle)
            rightKneeAngle in minAngle..maxAngle -> (maxAngle - rightKneeAngle) / (maxAngle - minAngle)
            else -> 0.0
        }

        // Smooth progress to handle any fluctuations
        lungeProgress = 0.8 * lungeProgress + 0.2 * progress
        sendProgressUpdate(lungeProgress.toFloat()) // Update the UI with smoothed progress

        // Set lunging state
        isLunging = progress > 0.5
    }
}