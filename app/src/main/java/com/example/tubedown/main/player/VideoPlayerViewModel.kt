package com.example.tubedown.main.player

import android.net.Uri
import androidx.databinding.ObservableField
//import com.allVideoDownloaderXmaster.OpenForTesting
import com.example.tubedown.main.base.BaseViewModel
import com.example.util.SingleLiveEvent
import javax.inject.Inject

//@OpenForTesting
class VideoPlayerViewModel @Inject constructor() : BaseViewModel() {

    val videoName = ObservableField("")
    val videoUrl = ObservableField(Uri.EMPTY)
    val videoHeaders = ObservableField(emptyMap<String, String>())

    val stopPlayerEvent = SingleLiveEvent<Void?>()

    override fun start() {
    }

    override fun stop() {
        stopPlayerEvent.call()
    }
}