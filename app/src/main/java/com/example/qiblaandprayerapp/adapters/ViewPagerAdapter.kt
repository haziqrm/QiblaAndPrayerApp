package com.example.qiblaandprayerapp.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.qiblaandprayerapp.fragments.PrayerTimesFragment
import com.example.qiblaandprayerapp.fragments.QiblaFragment
import com.example.qiblaandprayerapp.fragments.MosqueFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    // Keep a reference to the PrayerTimesFragment when created
    private var prayerTimesFragment: PrayerTimesFragment? = null

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> QiblaFragment()
            1 -> {
                // store and return instance
                val f = PrayerTimesFragment()
                prayerTimesFragment = f
                f
            }
            2 -> MosqueFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }

    fun getPrayerTimesFragment(): PrayerTimesFragment? = prayerTimesFragment
}
