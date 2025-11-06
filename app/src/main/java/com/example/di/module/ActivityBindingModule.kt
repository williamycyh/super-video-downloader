package com.example.di.module

import com.example.di.ActivityScoped
import com.example.tubedown.main.home.MainActivity
import com.example.di.module.activity.MainModule
import com.example.tubedown.main.player.VideoPlayerActivity
import com.example.di.module.activity.VideoPlayerModule
import com.example.tubedown.main.splash.SplashActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class ActivityBindingModule {

    @ActivityScoped
    @ContributesAndroidInjector
    internal abstract fun bindSplashActivity(): SplashActivity

    @ActivityScoped
    @ContributesAndroidInjector(modules = [MainModule::class])
    internal abstract fun bindMainActivity(): MainActivity

    @ActivityScoped
    @ContributesAndroidInjector(modules = [VideoPlayerModule::class])
    internal abstract fun bindVideoPlayerActivity(): VideoPlayerActivity
}