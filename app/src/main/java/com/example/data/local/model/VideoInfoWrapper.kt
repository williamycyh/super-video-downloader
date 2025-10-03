package com.example.data.local.model

import com.example.data.local.room.entity.VideoInfo
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class VideoInfoWrapper(
    @SerializedName("info")
    @Expose
    var videoInfo: VideoInfo?
)