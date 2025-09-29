package com.example.qiblaandprayerapp

import android.os.Build
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerAdjustments
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Helper to calculate prayer times using batoulapps/adhan2 library.
 * Provide a TextView where results will be shown.
 */
class PrayerTime(private val prayerTextView: TextView) {

    @RequiresApi(Build.VERSION_CODES.O)
    fun calculatePrayerTime(latitude: Double, longitude: Double) {
        // If location not set, skip (your MainActivity uses 0.0 sentinel)
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

            val fajr = formatter.format(Date(prayerTimes.fajr.toEpochMilliseconds()))
            val dhuhr = formatter.format(Date(prayerTimes.dhuhr.toEpochMilliseconds()))
            val asr = formatter.format(Date(prayerTimes.asr.toEpochMilliseconds()))
            val maghrib = formatter.format(Date(prayerTimes.maghrib.toEpochMilliseconds()))
            val isha = formatter.format(Date(prayerTimes.isha.toEpochMilliseconds()))

            prayerTextView.text = """
                Fajr: $fajr
                Dhuhr: $dhuhr
                Asr: $asr
                Maghrib: $maghrib
                Isha: $isha
            """.trimIndent()
        } catch (ex: Exception) {
            Log.e("PrayerTime", "Error calculating prayer times", ex)
        }
    }
}
