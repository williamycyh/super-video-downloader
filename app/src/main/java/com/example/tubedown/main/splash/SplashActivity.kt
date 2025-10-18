package com.example.tubedown.main.splash

//import com.allVideoDownloaderXmaster.OpenForTesting
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.example.BuildConfig
import com.example.R
import com.example.databinding.ActivitySplashBinding
import com.example.tubedown.main.base.BaseActivity
import com.example.tubedown.main.home.MainActivity
import com.example.tubedown.rereads.MyCommon
import com.example.tubedown.rereads.Utils
import com.example.tubedown.rereads.Utils.appCon
import com.example.util.SharedPrefHelper
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
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
        fetchConfig()
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

    var isFirstSplashShown = true
    private fun checkFirstLaunch() {
        isFirstSplashShown = sharedPrefHelper.getIsFirstSplashShown()

        if (!isFirstSplashShown) {
            // 首次显示闪屏，显示隐私协议和开始按钮
            showFirstLaunchUI()
        } else {
            // 非首次显示闪屏，显示简洁界面
            showNormalSplash()
            // 注意：自动跳转现在在 fetchConfig 完成后进行
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

        // 注意：自动跳转现在在 fetchConfig 完成后进行，不再在这里自动跳转
    }

    private fun openPrivacyPolicy() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)))
        startActivity(intent)
    }

    private fun startMainActivity() {
        // 标记已完成首次闪屏显示
        sharedPrefHelper.setIsFirstSplashShown(true)
        
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }

    private fun proceedToMainActivity() {

        if (!isFirstSplashShown) {
            // 首次启动，等待用户点击开始按钮，不自动跳转
            return
        } else {
            // 非首次启动
            startMainActivity()
        }
    }

    private fun fetchConfig() {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(1)
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        firebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 配置拉取成功，更新配置值
                    appCon.f_type = firebaseRemoteConfig.getLong("a_ty")
                    appCon.block_urls = firebaseRemoteConfig.getString("block_urls")

                    val v_version = firebaseRemoteConfig.getLong("av_version")
                    appCon.v_version = v_version.toInt()
                    if (appCon.f_type == 0L && !Utils.canShowFullAd(this)) {
                        appCon.f_type = 1
                    }

                    appCon.ph_num = firebaseRemoteConfig.getLong("am_pho")

                    appCon.dialog_msg = firebaseRemoteConfig.getString("adialog_msg")
                    appCon.dialog_pkg = firebaseRemoteConfig.getString("adialog_pkg")
                    appCon.dialog_type = firebaseRemoteConfig.getLong("adialog_type")
                    appCon.app_version = firebaseRemoteConfig.getLong("aapp_version")
                    appCon.force_use = firebaseRemoteConfig.getLong("aforce_use")
                } else {

                }
                
                // 无论配置拉取成功还是失败，都进行页面跳转
                proceedToMainActivity()
            }
    }

}