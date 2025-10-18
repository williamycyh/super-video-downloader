package com.example.di.module

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.di.ViewModelKey
import com.example.tubedown.main.history.HistoryViewModel
import com.example.tubedown.main.home.browser.BrowserViewModel
import com.example.tubedown.main.home.MainViewModel
import com.example.tubedown.main.help.HelpViewModel
import com.example.tubedown.main.home.browser.homeTab.BrowserHomeViewModel
import com.example.tubedown.main.home.browser.detectedVideos.GlobalVideoDetectionModel
import com.example.tubedown.main.home.browser.webTab.WebTabViewModel
import com.example.tubedown.main.home.browser.detectedVideos.VideoDetectionTabViewModel
import com.example.tubedown.main.player.VideoPlayerViewModel
import com.example.tubedown.main.progress.ProgressViewModel
import com.example.tubedown.main.proxies.ProxiesViewModel
import com.example.tubedown.main.settings.SettingsViewModel
import com.example.tubedown.main.splash.SplashViewModel
import com.example.tubedown.main.video.VideoViewModel
import com.example.util.ViewModelFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module(includes = [AppModule::class])
abstract class ViewModelModule {

    @Singleton
    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(SplashViewModel::class)
    abstract fun bindSplashViewModel(viewModel: SplashViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun bindMainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BrowserViewModel::class)
    abstract fun bindBrowserViewModel(viewModel: BrowserViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(VideoPlayerViewModel::class)
    abstract fun bindVideoPlayerViewModel(viewModel: VideoPlayerViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProgressViewModel::class)
    abstract fun bindProgressViewModel(viewModel: ProgressViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(VideoViewModel::class)
    abstract fun bindVideoViewModel(viewModel: VideoViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HistoryViewModel::class)
    abstract fun bindHistoryViewModel(viewModel: HistoryViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(HelpViewModel::class)
    abstract fun bindHelpViewModel(viewModel: HelpViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ProxiesViewModel::class)
    abstract fun bindProxiesViewModel(viewModel: ProxiesViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(WebTabViewModel::class)
    abstract fun bindWebTabViewModel(viewModel: WebTabViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(BrowserHomeViewModel::class)
    abstract fun bindBrowserHomeViewModel(viewModel: BrowserHomeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(GlobalVideoDetectionModel::class)
    abstract fun bindVideoDetectionAlgViewModel(viewModel: GlobalVideoDetectionModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(VideoDetectionTabViewModel::class)
    abstract fun bindVideoDetectionDetectedViewModel(viewModel: VideoDetectionTabViewModel): ViewModel
}
