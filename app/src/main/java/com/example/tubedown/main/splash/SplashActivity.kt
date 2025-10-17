package com.example.tubedown.main.splash

//import com.allVideoDownloaderXmaster.OpenForTesting
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.R
import com.example.databinding.ActivitySplashBinding
import com.example.tubedown.main.base.BaseActivity
import com.example.tubedown.main.home.MainActivity
import com.example.util.SharedPrefHelper
import javax.inject.Inject

//@OpenForTesting
@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var sharedPrefHelper: SharedPrefHelper

    private lateinit var splashViewModel: SplashViewModel

    private lateinit var dataBinding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏显示
        setupFullScreen()

        splashViewModel = ViewModelProvider(this, viewModelFactory)[SplashViewModel::class.java]

        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
        dataBinding.viewModel = splashViewModel

        setupViews()
        checkFirstLaunch()
    }

    private fun setupFullScreen() {
        // 只设置状态栏颜色，保持导航栏默认颜色
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = getColor(R.color.colorPrimaryDark)
            // 移除导航栏颜色设置，保持系统默认
        }
        
        // 隐藏ActionBar
        supportActionBar?.hide()
        
        // 只设置状态栏内容颜色（浅色内容），导航栏保持默认
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun setupViews() {
        // 设置隐私协议点击事件
        dataBinding.privacyLink.setOnClickListener {
            openPrivacyPolicy()
        }

        // 设置开始按钮点击事件
        dataBinding.startButton.setOnClickListener {
            startMainActivity()
        }
    }

    private fun checkFirstLaunch() {
        val isFirstLaunch = sharedPrefHelper.getIsFirstStart()
        
        if (isFirstLaunch) {
            // 首次启动，显示隐私协议和开始按钮
            showFirstLaunchUI()
        } else {
            // 非首次启动，显示简洁界面并自动跳转
            showNormalSplash()
        }
    }

    private fun showFirstLaunchUI() {
        // 显示首次启动UI元素
        dataBinding.disclaimerText.visibility = View.VISIBLE
        dataBinding.privacyLink.visibility = View.VISIBLE
        dataBinding.startButton.visibility = View.VISIBLE
    }

    private fun showNormalSplash() {
        // 隐藏首次启动UI元素
        dataBinding.disclaimerText.visibility = View.GONE
        dataBinding.privacyLink.visibility = View.GONE
        dataBinding.startButton.visibility = View.GONE

        // 1秒后自动跳转
        Handler(Looper.getMainLooper()).postDelayed({
            startMainActivity()
        }, 1000)
    }

    private fun openPrivacyPolicy() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)))
        startActivity(intent)
    }

    private fun startMainActivity() {
        // 标记已完成首次启动
        sharedPrefHelper.setIsFirstStart(false)
        
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }
}