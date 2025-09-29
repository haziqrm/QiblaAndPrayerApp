package com.example.qiblaandprayerapp

import android.location.Location
import android.util.Log
import android.view.animation.RotateAnimation
import android.widget.ImageView

/**
 * Simple helper that calculates bearing from user's location to the Kaaba
 * and applies an initial rotation animation to the compass image.
 */
class KaabaHelper(private val compassImage: ImageView) {

    var kaabaBearing: Float = 0f
        private set

    /**
     * Calculate the bearing from (latitude, longitude) to the Kaaba coordinates
     * and animate the compass image to the computed bearing.
     */
    fun calculateBearing(latitude: Double, longitude: Double) {
        try {
            val kaabaLocation = Location("").apply {
                this.latitude = 21.4225
                this.longitude = 39.8262
            }

            val userLocation = Location("").apply {
                this.latitude = latitude
                this.longitude = longitude
            }

            kaabaBearing = userLocation.bearingTo(kaabaLocation)

            // Normalize to 0..360
            if (kaabaBearing < 0) kaabaBearing += 360f
            if (kaabaBearing >= 360f) kaabaBearing -= 360f

            Log.d("KaabaHelper", "Calculated Kaaba bearing: $kaabaBearing")

            // Set initial rotation (no animation duration so it sets immediately)
            val initialRotationAni = RotateAnimation(
                0f, kaabaBearing,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f
            )
            initialRotationAni.duration = 0
            initialRotationAni.fillAfter = true
            compassImage.startAnimation(initialRotationAni)
        } catch (ex: Exception) {
            Log.e("KaabaHelper", "Error calculating bearing", ex)
        }
    }
}
