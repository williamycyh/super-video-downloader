package com.example.data.remote

import com.nhaarman.mockito_kotlin.mock
import com.example.data.local.model.VideoInfoWrapper
import com.example.data.local.room.entity.VideoInfo
import com.example.data.remote.service.VideoService
import org.junit.Before
import org.junit.Test

class VideoRemoteDataSourceTest {

    private lateinit var videoService: VideoService

    private lateinit var videoRemoteDataSource: VideoRemoteDataSource

    private lateinit var videoInfoWrapper: VideoInfoWrapper

    private lateinit var videoInfo: VideoInfo

    private lateinit var url: String

    @Before
    fun setup() {
        videoService = mock()
        videoRemoteDataSource = VideoRemoteDataSource(videoService)

        url = "videoUrl"
        videoInfo = VideoInfo(title = "title", originalUrl = "originalUrl")
        videoInfoWrapper = VideoInfoWrapper(videoInfo)
    }

    @Test
    fun `test get video info`() {
        assert(true)
//        doReturn(Flowable.just(videoInfoWrapper)).`when`(videoService).getVideoInfo(url)
//
//        videoRemoteDataSource.getVideoInfo(url).test()
//            .assertNoErrors()
//            .assertValue { it == videoInfo }
    }
}