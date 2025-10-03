package com.example.util.fragment

import androidx.fragment.app.Fragment
import com.example.tubedown.main.history.HistoryFragment
import com.example.tubedown.main.home.browser.BrowserFragment
import com.example.tubedown.main.home.browser.detectedVideos.DetectedVideosTabFragment
import com.example.tubedown.main.home.browser.homeTab.BrowserHomeFragment
import com.example.tubedown.main.home.browser.webTab.WebTabFragment
import com.example.tubedown.main.progress.ProgressFragment
import com.example.tubedown.main.settings.SettingsFragment
import com.example.tubedown.main.video.VideoFragment
import javax.inject.Inject

interface FragmentFactory {
    fun createBrowserFragment(): Fragment
    fun createProgressFragment(): Fragment
    fun createVideoFragment(): Fragment
    fun createSettingsFragment(): Fragment
    fun createHistoryFragment(): Fragment

    fun createBrowserHomeFragment(): Fragment

    fun createWebTabFragment(): Fragment

    fun createDetectedVideosTabFragment(): Fragment
}

class FragmentFactoryImpl @Inject constructor() : FragmentFactory {
    override fun createBrowserFragment() = BrowserFragment.newInstance()

    override fun createProgressFragment() = ProgressFragment.newInstance()

    override fun createVideoFragment() = VideoFragment.newInstance()

    override fun createSettingsFragment() = SettingsFragment.newInstance()

    override fun createHistoryFragment() = HistoryFragment.newInstance()

    override fun createBrowserHomeFragment() = BrowserHomeFragment.newInstance()

    override fun createWebTabFragment() = WebTabFragment.newInstance()

    override fun createDetectedVideosTabFragment() = DetectedVideosTabFragment.newInstance()
}