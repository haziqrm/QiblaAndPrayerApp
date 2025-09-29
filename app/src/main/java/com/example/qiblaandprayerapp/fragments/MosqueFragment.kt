package com.example.qiblaandprayerapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.example.qiblaandprayerapp.ui.theme.MosqueNearMeTheme
import com.example.qiblaandprayerapp.screens.MainScreen

class MosqueFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ArcGISEnvironment.apiKey = ApiKey.create("AAPTxy8BH1VEsoebNVZXo8HurMnmg8UQLxaAh9D6E1-rXGca6rI1iDleE5TBscxf9qB82WOUXaTeEHLeQtxalmXZM5wQ1o-dVaSftv-q3UNQMOAvHWSLA2uID-wwvROgDZvYSu1acKybtCIaDDMp1f3GwOEag9ITTkDE1hYJ518TmkpIOWLGsaNe4MhCwBHIT7u-ulyw2Nav0KTS06Wq4CzzQmaKv2hJd-AtvOQa9ubREoI.AT1_oSbwS3uR")

        return ComposeView(requireContext()).apply {
            setContent {
                MosqueNearMeTheme {
                    MainScreen()
                }
            }
        }
    }
}
