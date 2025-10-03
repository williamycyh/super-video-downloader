package com.example.tubedown.component.binding

import androidx.databinding.BindingAdapter
import com.example.R
import com.google.android.material.bottomnavigation.BottomNavigationView

object BottomNavigationViewBinding {

    @BindingAdapter("app:selectedItemId")
    @JvmStatic
    fun BottomNavigationView.setSelectedItemId(position: Int) {
        selectedItemId = when (position) {
            0 -> R.id.tab_browser
            1 -> R.id.tab_progress
            2 -> R.id.tab_video
            else -> R.id.tab_settings
        }
    }
}