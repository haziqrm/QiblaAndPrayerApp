package com.example.qiblaandprayerapp.fragments

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.example.qiblaandprayerapp.MainActivity
import com.example.qiblaandprayerapp.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class PrayerTimesFragment : Fragment() {
    private lateinit var fajrTime: TextView
    private lateinit var dhuhrTime: TextView
    private lateinit var asrTime: TextView
    private lateinit var maghribTime: TextView
    private lateinit var ishaTime: TextView
    private lateinit var currentPrayerText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_prayer_times, container, false)
    }

    override fun onResume() {
        super.onResume()
        val activity = activity as? MainActivity
        activity?.lastLocation?.let { (lat, lng) ->
            updatePrayerTimes(lat, lng)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val animDrawable = view.background as? AnimationDrawable
        animDrawable?.apply {
            setEnterFadeDuration(2000)
            setExitFadeDuration(4000)
            start()
        }

        fajrTime = view.findViewById(R.id.fajrTime)
        dhuhrTime = view.findViewById(R.id.dhuhrTime)
        asrTime = view.findViewById(R.id.asrTime)
        maghribTime = view.findViewById(R.id.maghribTime)
        ishaTime = view.findViewById(R.id.ishaTime)
        currentPrayerText = view.findViewById(R.id.currentPrayerName)

    }

    fun updatePrayerTimes(latitude: Double, longitude: Double) {
        if (latitude == 0.0 && longitude == 0.0) return

        try {
            val cal = Calendar.getInstance()
            val currentDate = DateComponents(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )

            val coords = Coordinates(latitude, longitude)

            val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
                .copy(madhab = Madhab.HANAFI, prayerAdjustments = PrayerAdjustments(fajr = 2))

            val prayerTimes = PrayerTimes(coords, currentDate, params)

            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }

            val now = Date()

            val currentPrayer = when {
                now.before(Date(prayerTimes.fajr.toEpochMilliseconds())) -> "Before Fajr"
                now.before(Date(prayerTimes.dhuhr.toEpochMilliseconds())) -> "Fajr"
                now.before(Date(prayerTimes.asr.toEpochMilliseconds())) -> "Dhuhr"
                now.before(Date(prayerTimes.maghrib.toEpochMilliseconds())) -> "Asr"
                now.before(Date(prayerTimes.isha.toEpochMilliseconds())) -> "Maghrib"
                else -> "Isha"
            }

            val fajr = formatter.format(Date(prayerTimes.fajr.toEpochMilliseconds()))
            val dhuhr = formatter.format(Date(prayerTimes.dhuhr.toEpochMilliseconds()))
            val asr = formatter.format(Date(prayerTimes.asr.toEpochMilliseconds()))
            val maghrib = formatter.format(Date(prayerTimes.maghrib.toEpochMilliseconds()))
            val isha = formatter.format(Date(prayerTimes.isha.toEpochMilliseconds()))

            activity?.runOnUiThread {
                fajrTime.text = "$fajr"
                dhuhrTime.text = "$dhuhr"
                asrTime.text = "$asr"
                maghribTime.text = "$maghrib"
                ishaTime.text = "$isha"
                currentPrayerText.text = "$currentPrayer"
            }
        } catch (ex: Exception) {
            activity?.runOnUiThread {
                fajrTime.text = "Error calculating times"
                dhuhrTime.text = "Error calculating times"
                maghribTime.text = "Error calculating times"
                ishaTime.text = "Error calculating times"
                currentPrayerText.text = "Error calculating times"
            }
        }
    }
}