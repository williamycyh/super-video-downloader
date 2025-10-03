package com.example.tubedown.component.binding

import android.net.Uri
import android.widget.VideoView
import androidx.core.content.FileProvider
import androidx.databinding.BindingAdapter
import java.io.File

object VideoViewBinding {

    @BindingAdapter("app:videoURI")
    @JvmStatic
    fun VideoView.setVideoURI(videoPath: String?) {
        videoPath?.let { path ->
            val uri = if (path.startsWith("http")) {
                Uri.parse(path)
            } else {
                FileProvider.getUriForFile(context, context.packageName + ".provider", File(path))
            }
            setVideoURI(uri)
        }
    }
}