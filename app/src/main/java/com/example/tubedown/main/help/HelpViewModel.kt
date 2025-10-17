package com.example.tubedown.main.help

import androidx.lifecycle.ViewModel
import com.example.tubedown.main.base.BaseViewModel
import javax.inject.Inject

class HelpViewModel @Inject constructor() : BaseViewModel() {
    
    override fun start() {
        // 帮助页面启动时的逻辑
        // 可以在这里添加统计帮助页面访问次数等
    }
    
    override fun stop() {
        // 帮助页面停止时的逻辑
    }
}
