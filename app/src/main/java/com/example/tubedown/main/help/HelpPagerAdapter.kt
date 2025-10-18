package com.example.tubedown.main.help

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class HelpPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HelpVideoDetectionFragment()
            1 -> HelpVideoDownloadFragment()
            2 -> HelpDisclaimerFragment()
            3 -> HelpTipsFragment()
            4 -> HelpTroubleshootingFragment()
            else -> HelpVideoDetectionFragment()
        }
    }
}
