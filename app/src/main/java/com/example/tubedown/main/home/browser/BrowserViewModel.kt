package com.example.tubedown.main.home.browser

import androidx.databinding.*
import androidx.lifecycle.MutableLiveData
import com.example.data.local.room.entity.VideoInfo
import com.example.tubedown.main.base.BaseViewModel
import com.example.tubedown.main.home.browser.webTab.WebTab
import com.example.util.SingleLiveEvent
import com.example.tubedown.main.settings.SettingsViewModel
import javax.inject.Inject

//@OpenForTesting
class BrowserViewModel @Inject constructor() : BaseViewModel() {

    companion object {
        const val SEARCH_URL = "https://duckduckgo.com/?t=ffab&q=%s"

        var instance: BrowserViewModel? = null
    }

    var settingsModel: SettingsViewModel? = null

    val openPageEvent = SingleLiveEvent<WebTab>()

    val closePageEvent = SingleLiveEvent<WebTab>()

    val selectWebTabEvent = SingleLiveEvent<WebTab>()

    val updateWebTabEvent = SingleLiveEvent<WebTab>()

    val workerM3u8MpdEvent = MutableLiveData<DownloadButtonState>()

    val workerMP4Event = MutableLiveData<DownloadButtonState>()

    val progress = ObservableInt(0)

    val tabs = ObservableField(listOf(WebTab.HOME_TAB))

    val currentTab = ObservableInt(HOME_TAB_INDEX)

    override fun start() {
        instance = this
    }

    override fun stop() {
        instance = null
    }
}

abstract class DownloadButtonState

class DownloadButtonStateLoading : DownloadButtonState()

class DownloadButtonStateCanDownload(val info: VideoInfo?) : DownloadButtonState()
class DownloadButtonStateCanNotDownload : DownloadButtonState()