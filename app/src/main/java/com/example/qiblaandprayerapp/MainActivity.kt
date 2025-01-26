package com.example.qiblaandprayerapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.TimeZone

class MainActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val alpha = 0.075f // Smoothing factor, adjust as needed
    private var smoothedDegree = 0f

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)

    private var currentDegree = 0f

    private lateinit var compassImage: ImageView
    private lateinit var kaabaImage: ImageView
    private lateinit var degrees: TextView
    private lateinit var longlat: TextView
    private lateinit var prayerTime: TextView
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Global variables for latitude and longitude
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var kaabaBearing: Float = 0f

    // Permission request launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getLastKnownLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    @SuppressLint("MissingInflatedId")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        longlat = findViewById(R.id.longlat)
        compassImage = findViewById(R.id.imageView)
        degrees = findViewById(R.id.textView)
        prayerTime = findViewById(R.id.prayerTimeText)

        // Initialize sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // Initialize location services
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Check for location permissions and get location
        if (isLocationPermissionGranted()) {
            getLastKnownLocation()
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permission using the launcher
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            false
        } else {
            true
        }
    }

    private fun getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        latitude = location.latitude
                        longitude = location.longitude
                        val formattedLongitude = String.format("%.5f", longitude)
                        val formattedLatitude = String.format("%.5f", latitude)
                        // longlat.text = "Longitude: $formattedLongitude, Latitude: $formattedLatitude"
                        // Optionally, show a Toast message
                        Toast.makeText(
                            this,
                            "Latitude: $latitude, Longitude: $longitude",
                            Toast.LENGTH_LONG
                        ).show()

                        Log.d("MainActivity", "Latitude: $latitude, Longitude: $longitude")
                        calculateBearing()
                    } else {
                        Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Log.e("MainActivity", "Failed to get location: ${it.message}")
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        // Handle accelerometer data
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, event.values.size)
        }

        // Handle magnetometer data
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
        }

        val rotationMatrix = FloatArray(9)
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            val degree = Math.toDegrees(orientation[0].toDouble()).toFloat()

            smoothedDegree = alpha * degree + (1 - alpha) * smoothedDegree

            // Adjust the compass angle by subtracting kaabaBearing
            val adjustedDegree = smoothedDegree - kaabaBearing // Inverse the bearing if needed
            if (((adjustedDegree > -1.5) && (adjustedDegree < 1.5)) || ((adjustedDegree > -361.5) && (adjustedDegree < -359.5))) {
                degrees.text = ("Pointing to kaaba")
            }
            else{
                degrees.text = adjustedDegree.toString()
            }
            // Apply rotation animation
            val rotationAni = RotateAnimation(
                currentDegree,
                -adjustedDegree,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
            )
            CalculatePrayerTime()
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
            sensorManager?.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        magnetometer?.let {
            sensorManager?.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    // Method to calculate bearing from current location to Kaaba's location
    fun calculateBearing() {
        val kaabaLocation = Location("")
        val userLocation = Location("")

        userLocation.latitude = latitude
        userLocation.longitude = longitude

        kaabaLocation.latitude = 21.4225  // Latitude of the Kaaba
        kaabaLocation.longitude = 39.8262 // Longitude of the Kaaba

        // First calculate the bearing without any correction
        kaabaBearing = userLocation.bearingTo(kaabaLocation)

        // Ensure the bearing stays within 0 to 360 degrees
        if (kaabaBearing < 0) kaabaBearing += 360
        if (kaabaBearing >= 360) kaabaBearing -= 360

        // Log the adjusted bearing
        Log.d("MainActivity", "Adjusted bearing to Kaaba: $kaabaBearing")

        // Initially set the rotation of the compass to point towards Kaaba
        val initialRotationAni = RotateAnimation(
            0f, kaabaBearing,
            Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point is relative to the parent (compass)
            Animation.RELATIVE_TO_SELF, 0.5f
        )
        initialRotationAni.duration = 0  // Instant rotation to the correct bearing
        initialRotationAni.fillAfter = true
        compassImage.startAnimation(initialRotationAni)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun CalculatePrayerTime() {
        val current = LocalDate.now()

        val year = current.year
        val month = current.monthValue
        val day = current.dayOfMonth

        val currentDate = DateComponents(year, month, day)
        val roundedLongitude = String.format("%.5f", longitude).toDouble()
        val roundedLatitude = String.format("%.5f", latitude).toDouble()

        // val coordinates = Coordinates(37.42208,-122.08392)
        val coordinates = Coordinates(roundedLatitude, roundedLongitude)
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters.copy(madhab = Madhab.HANAFI, prayerAdjustments = PrayerAdjustments(fajr = 2))

        val prayerTimes = PrayerTimes(coordinates, currentDate, params)

        val formatter = SimpleDateFormat("hh:mm a")
        val localTimeZone = TimeZone.getDefault()
        formatter.timeZone = localTimeZone
        // longlat.text = localTimeZone.toString()
        formatter.format(Date(prayerTimes.fajr.toEpochMilliseconds()))

        val fajrTime = formatter.format(Date(prayerTimes.fajr.toEpochMilliseconds()))
        val dhuhrTime = formatter.format(Date(prayerTimes.dhuhr.toEpochMilliseconds()))
        val asrTime = formatter.format(Date(prayerTimes.asr.toEpochMilliseconds()))
        val maghribTime = formatter.format(Date(prayerTimes.maghrib.toEpochMilliseconds()))
        val ishaTime = formatter.format(Date(prayerTimes.isha.toEpochMilliseconds()))

        val prayerTimeTextView: TextView = findViewById(R.id.prayerTimeText)
        prayerTimeTextView.text = """
        Fajr: $fajrTime
        Dhuhr: $dhuhrTime
        Asr: $asrTime
        Maghrib: $maghribTime
        Isha: $ishaTime
    """.trimIndent()
    }
}