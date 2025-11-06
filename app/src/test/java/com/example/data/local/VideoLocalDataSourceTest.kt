package com.example.data.local

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.example.data.local.room.dao.VideoDao
import com.example.data.local.room.entity.VideoInfo
import org.junit.Before
import org.junit.Test

class VideoLocalDataSourceTest {

    private lateinit var videoDao: VideoDao

    private lateinit var videoLocalDataSource: VideoLocalDataSource

    private lateinit var videoInfo: VideoInfo

    private lateinit var url: String


    @Before
    fun setup() {
        videoDao = mock()
        videoLocalDataSource = VideoLocalDataSource(videoDao)

        url = "videoUrl"
        videoInfo = VideoInfo(title = "title", originalUrl = "originalUrl")
    }

//    @Test
//    fun `test get video info`() {
//        doReturn(Maybe.just(videoInfo)).`when`(videoDao).getVideoById(url)
//
//        videoLocalDataSource.getVideoInfo(url).test()
//            .assertNoErrors()
//            .assertValue { it == videoInfo }
//    }

    @Test
    fun `test save video info`() {
        videoLocalDataSource.saveVideoInfo(videoInfo)
        verify(videoDao).insertVideo(videoInfo)
    }

}