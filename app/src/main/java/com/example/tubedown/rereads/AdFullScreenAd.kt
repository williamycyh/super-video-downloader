package com.example.tubedown.rereads

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd
import com.example.BuildConfig

class AdFullScreenAd : AdCenter() {
    val TAG = "FullScreenAd"
    var activity: Context? = null

    public fun setFullScreenLoadResult(result: FullScreenLoadResult) {
        mFullScreenLoadResult = result
    }

    var mFullScreenLoadResult: FullScreenLoadResult? = null

    interface FullScreenLoadResult {
        fun onAdLoaded()
        fun onAdDisplayed()
        fun onAdHidden()
        fun onAdClicked()
        fun onAdLoadFailed()
        fun onAdDisplayFailed()
    }

    override fun initQueue() {
        if (BuildConfig.DEBUG) {
            return
        }

        queue.offer(Ad_Type_Mopub)
//        queue.offer(Ad_Type_IRONSOURCE)
    }

    var admobclicked = false

    var mopubId = ""
    var admobId = ""
    var fbId = ""
    fun loadFullScreen(activity: Context, mopubId: String) {
        if (!isAdShow()) {
            return
        }
        initQueue()
        this.mopubId = mopubId
        this.activity = activity
//        this.admobId = admobId
//        this.fbId = fbId
        loadByOrder()
    }

    fun loadByOrder() {
        var type = queue.poll()

        if (type == Ad_Type_Mopub && !TextUtils.isEmpty(mopubId)) {
            Log.d(TAG, "loadByOrder Ad_Type_Mopub")
            loadMopubDelay()
        } else if (type == Ad_Type_IRONSOURCE) {
            Log.d(TAG, "loadByOrder Ad_Type_IRONSOURCE")
//            loadIronAd()
        }
    }

    fun showAd(): Boolean {
        if (!Utils.canShowFullAd(activity)) {
            return false
        }
        if (mInterstitial?.isReady == true) {
            mInterstitial?.showAd()
            mInterstitial = null
            loadByOrder()
            return true
        }
        return false
    }

    fun isReady(): Boolean {
        if (mInterstitial?.isReady == true
//                || IronSource.isInterstitialReady()
//                || mAdmobInterstitialAd?.isLoaded == true
//                || isFbReady()
        ) {
            return true
        }
        return false
    }

    ///////////////////////mopub///////////////////
    fun loadMopubDelay() {
        if (MoPubInited) {
            loadMopub()
        } else {
            android.os.Handler().postDelayed({
                loadMopubDelay()
            }, 100)
        }
    }

    var failCount = 0;
    var maxLinstener = object : MaxAdListener {
        override fun onAdLoaded(ad: MaxAd) {
            Log.d(TAG, "onAdLoaded:")
            mFullScreenLoadResult?.onAdLoaded()
        }

        override fun onAdDisplayed(ad: MaxAd) {
            mFullScreenLoadResult?.onAdDisplayed()
        }

        override fun onAdHidden(ad: MaxAd) {
            mFullScreenLoadResult?.onAdHidden()
            Log.d(TAG, "onAdHidden:")
        }

        override fun onAdClicked(ad: MaxAd) {
            Log.d(TAG, "onAdClicked:")
            mFullScreenLoadResult?.onAdClicked()
        }

        override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
            if (queue.size <= 0 && failCount < 3) {
                queue.offer(Ad_Type_Mopub)
                failCount = failCount + 1;
            }
            loadByOrder()
            Log.d(TAG, "onAdLoadFailed:" + error?.message)
            mFullScreenLoadResult?.onAdLoadFailed()
        }

        override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
            Log.d(TAG, "onAdDisplayFailed:")
            mFullScreenLoadResult?.onAdDisplayFailed()
        }

    }
    var mInterstitial: MaxInterstitialAd? = null
    fun loadMopub() {
        mInterstitial = MaxInterstitialAd(mopubId, activity)
        mInterstitial?.setListener(maxLinstener)

        // Load the first ad
        mInterstitial?.loadAd()
    }

    fun isMopubReady() {
        mInterstitial?.isReady
    }

    fun showMopub() {
        mInterstitial?.showAd()
    }
}