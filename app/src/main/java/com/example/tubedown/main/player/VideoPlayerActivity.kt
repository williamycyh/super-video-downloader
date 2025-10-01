package com.example.tubedown.main.player

import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.example.R
import com.example.tubedown.main.base.BaseActivity
import com.example.util.ext.addFragment

class VideoPlayerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_player)

        intent.extras?.let { addFragment(R.id.content_frame, it, ::VideoPlayerFragment) }
    }
}