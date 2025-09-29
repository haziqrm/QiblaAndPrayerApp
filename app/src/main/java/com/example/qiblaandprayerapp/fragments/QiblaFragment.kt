package com.example.qiblaandprayerapp.fragments

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.qiblaandprayerapp.R

class QiblaFragment : Fragment(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)

    private var currentDegree = 0f
    private var smoothedDegree = 0f
    private val alpha = 0.075f

    private lateinit var compassImage: ImageView
    private lateinit var degrees: TextView

    private var kaabaBearing: Float = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_qibla, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rootLayout = view.findViewById<RelativeLayout>(R.id.root_layout)

        compassImage = view.findViewById(R.id.imageView)
        degrees = view.findViewById(R.id.textView)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    fun updateLocation(latitude: Double, longitude: Double) {
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

        // Set initial rotation
        val initialRotationAni = RotateAnimation(
            0f, kaabaBearing,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f,
            RotateAnimation.RELATIVE_TO_SELF, 0.5f
        )
        initialRotationAni.duration = 0
        initialRotationAni.fillAfter = true
        compassImage.startAnimation(initialRotationAni)
        Log.d("QiblaFragment", "updateLocation called with $latitude, $longitude")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, event.values.size)
        }
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
        }

        val rotationMatrix = FloatArray(9)
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            val degree = Math.toDegrees(orientation[0].toDouble()).toFloat()
            smoothedDegree = alpha * degree + (1 - alpha) * smoothedDegree

            val adjustedDegree = smoothedDegree - kaabaBearing
            if (((adjustedDegree > -1.5) && (adjustedDegree < 1.5)) ||
                ((adjustedDegree > -361.5) && (adjustedDegree < -359.5))) {
                degrees.text = "Pointing to Kaaba"
            } else {
                degrees.text = adjustedDegree.toString()
            }

            val rotationAni = RotateAnimation(
                currentDegree,
                -adjustedDegree,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f
            )
            rotationAni.duration = 210
            rotationAni.fillAfter = true
            compassImage.startAnimation(rotationAni)

            currentDegree = -adjustedDegree
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }
}