package com.example.di.module.activity

import android.app.Activity
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.example.di.ActivityScoped
import com.example.di.FragmentScoped
import com.example.tubedown.main.bookmarks.BookmarksFragment
import com.example.tubedown.main.help.HelpFragment
import com.example.tubedown.main.history.HistoryFragment
import com.example.tubedown.main.home.browser.BrowserFragment
import com.example.tubedown.main.home.MainActivity
import com.example.tubedown.main.home.browser.homeTab.BrowserHomeFragment
import com.example.tubedown.main.home.browser.webTab.WebTabFragment
import com.example.tubedown.main.home.browser.detectedVideos.DetectedVideosTabFragment
import com.example.tubedown.main.progress.ProgressFragment
import com.example.tubedown.main.proxies.ProxiesFragment
import com.example.tubedown.main.settings.SettingsFragment
import com.example.tubedown.main.video.VideoFragment
import com.example.util.fragment.FragmentFactory
import com.example.util.fragment.FragmentFactoryImpl
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class MainModule {

    @OptIn(UnstableApi::class)
    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun bindBrowserFragment(): BrowserFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun bindProxiesFragment(): ProxiesFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun bindHistoryFragment(): HistoryFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun bindHelpFragment(): HelpFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun bindProgressFragment(): ProgressFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun bindVideoFragment(): VideoFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun bindSettingsFragment(): SettingsFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun bindWebTabFragment(): WebTabFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun bindDetectedVideosFragment(): DetectedVideosTabFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun bindBrowserHomeFragment(): BrowserHomeFragment

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun bindBookmarksFragment(): BookmarksFragment

    @ActivityScoped
    @Binds
    abstract fun bindMainActivity(mainActivity: MainActivity): Activity

    @ActivityScoped
    @Binds
    abstract fun bindFragmentFactory(fragmentFactory: FragmentFactoryImpl): FragmentFactory
}