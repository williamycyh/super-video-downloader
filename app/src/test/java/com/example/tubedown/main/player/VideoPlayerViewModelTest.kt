package com.example.tubedown.main.player

import org.junit.Before

class VideoPlayerViewModelTest {

    private lateinit var videoPlayerViewModel: VideoPlayerViewModel

    @Before
    fun setup() {
        videoPlayerViewModel = VideoPlayerViewModel()
    }

//    @Test
//    fun `test video volume on`() {
//        assertEquals(1.0f, videoPlayerViewModel.getVolume())
//    }
//
//    @Test
//    fun `test video volume off`() {
//        videoPlayerViewModel.isVolumeOn = false
//        assertEquals(0.0f, videoPlayerViewModel.getVolume())
//    }
}