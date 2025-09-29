package com.example.qiblaandprayerapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.tabs.TabLayoutMediator
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout

import com.example.qiblaandprayerapp.adapters.ViewPagerAdapter
import com.example.qiblaandprayerapp.fragments.MosqueFragment
import com.example.qiblaandprayerapp.fragments.QiblaFragment
import com.example.qiblaandprayerapp.fragments.PrayerTimesFragment

class MainActivity : FragmentActivity() {
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    var lastLocation: Pair<Double, Double>? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                getLastKnownLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = ViewPagerAdapter(this)
        viewPager.isUserInputEnabled = false

        val tabIcons = listOf(
            R.drawable.iconqibla,   // Tab 0
            R.drawable.iconprayer,  // Tab 1
            R.drawable.iconmosque   // Tab 2
        )

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setIcon(tabIcons[position])
        }.attach()

        tabLayout.setTabIconTintResource(R.color.white)

        if (isLocationPermissionGranted()) {
            getLastKnownLocation()
        }
        hideNavigationBar()
    }

    private fun hideNavigationBar() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
    }

    private fun isLocationPermissionGranted(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
                        updateFragments(latitude, longitude)
                        Log.d("MainActivity", "Got location: $latitude, $longitude")
                    } else {
                        Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Log.e("MainActivity", "Failed to get location: ${it.message}")
                }
        }
    }

    private fun updateFragments(latitude: Double, longitude: Double) {
        Log.d("MainActivity", "Updating fragments with location: $latitude, $longitude")
        lastLocation = latitude to longitude  // <-- store the location here
        val fragments = supportFragmentManager.fragments
        for (fragment in fragments) {
            when (fragment) {
                is QiblaFragment -> fragment.updateLocation(latitude, longitude)
                is PrayerTimesFragment -> fragment.updatePrayerTimes(latitude, longitude)
            }
        }
    }
}