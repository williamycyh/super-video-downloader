package com.example.tubedown.rereads

import android.app.Activity
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder
import com.example.BuildConfig
import com.example.R


class AdNativeAdCenter: AdCenter() {
    val TAG = "NativeAdCenter"

    var ADMOB_AD_UNIT_ID = ""//ca-app-pub-4642862715300910/1070445143
    var FB_PLACEMENT_ID = ""
    var MOPUB_ID = ""
    var INMOBI = 0L
    var container:ViewGroup? = null
    var context: Activity? = null

    init {
    }

    override
    fun initQueue(){
        if(BuildConfig.DEBUG){
            return
        }
        //优先级
//        if(isShowAdmob(AdmobManager.ADMOB_TYPE_NATIVE)){
//            queue.offer(Ad_Type_Admob)
//        }
//        if(FbManager.isShowFb(FbManager.FB_TYPE_NATIVE)){
//            queue.offer(Ad_Type_Facebook)
//        }
        queue.offer(Ad_Type_Mopub)
    }

//    var natvieAdInterface: NatvieAdInterface? = null
//
//    interface NatvieAdInterface{
//        fun onNativeAdLoaded(adType: Int, mopubAd:com.mopub.nativeads.NativeAd?)
//        fun onNativeLoadFail(adType: Int, errorCode: Int)
//    }

    fun setNativeLoadResult(result: NativeLoadResult){
        mNativeLoadResult = result
    }
    var mNativeLoadResult: NativeLoadResult? = null
    interface NativeLoadResult{
       fun onResult(success: Boolean)
    }

    var mIsSmall = false;
    fun loadNativeAds(context: Activity, container: ViewGroup,
                      mopubId: String, inmobi:Long, isSmall: Boolean){
        if(!isAdShow()){
            Log.d(TAG, "loadNativeAds no show")
            return
        }
        initQueue()
        this.container = container
        this.container?.isClickable = true
        this.mIsSmall = isSmall;

        MOPUB_ID = mopubId
        this.INMOBI = inmobi
        this.context = context

        try{
            loadByOrder(context)
        }catch (e: IllegalArgumentException){
        }

    }

    fun loadByOrder(context:Activity){
        var type = queue.poll()

        if(type == null){
            mNativeLoadResult?.onResult(false)
            return
        }
//        if(type == Ad_Type_Admob && !isShowAdmob(AdmobManager.ADMOB_TYPE_NATIVE)){
//            loadByOrder(context)
//            return
//        }

//        if (type == Ad_Type_Admob){
//            Log.d(TAG, "loadByOrder Ad_Type_Admob")
//            if(TextUtils.isEmpty(ADMOB_AD_UNIT_ID)){
//                loadByOrder(context)
//            } else {
//                loadAdmob(context)
//            }
//        } else if(type == Ad_Type_Facebook){
//            Log.d(TAG, "loadByOrder Ad_Type_Facebook")
//            if(TextUtils.isEmpty(FB_PLACEMENT_ID)){
//                loadByOrder(context)
//            } else {
//                loadFB(context)
//            }
//        } else
            if(type == Ad_Type_Mopub){
            Log.d(TAG, "loadByOrder Ad_Type_Mopub")
            if(TextUtils.isEmpty(MOPUB_ID)){
                loadByOrder(context)
            } else {
                loadMopubDelay(context)
            }
        }
    }

    fun loadMopubDelay(context: Activity){
        if(MoPubInited){
            loadMopub(context)
        } else {
            Handler().postDelayed({
                loadMopubDelay(context)
            },100)
        }
    }


    fun disableAdmobClick(view: View?, clickable: Boolean){
        setClickable(view, clickable)
    }

    fun disableFbClick(view: View?, clickable: Boolean){
        setClickable(view, clickable)
    }

    fun setClickable(view: View?, clickable: Boolean) {
        if (view != null) {
            view.isClickable = clickable
            if (view is ViewGroup) {
                for (i in 0 until view.getChildCount()) {
                    setClickable(view.getChildAt(i), clickable)
                }
            }
        }
    }

    private lateinit var nativeAdLoader: MaxNativeAdLoader
    private var nativeAd: MaxAd? = null
    fun loadMopub(context: Activity) {

        val nativeAdListener = object : MaxNativeAdListener() {
            override fun onNativeAdLoaded(view: MaxNativeAdView?, ad: MaxAd) {
                Log.d(TAG, "onNativeAdLoaded:" + ad)
                // Cleanup any pre-existing native ad to prevent memory leaks.
                if (nativeAd != null) {
                    nativeAdLoader.destroy(nativeAd)
                }

                // Save ad for cleanup.
                if (ad != null) {
                    nativeAd = ad

                    // Add ad view to view.
                    if (view?.parent != null) {
                        (view?.parent as ViewGroup).removeView(view) // <- fix
                    }

                    container?.removeAllViews()
                    container?.addView(view)

                    mNativeLoadResult?.onResult(true)
                }
            }

            override fun onNativeAdLoadFailed(adUnitId: String, maxError: MaxError) {
                mNativeLoadResult?.onResult(false)
                Log.d(TAG, "onNativeAdLoadFailed:" + maxError)
            }

            override fun onNativeAdClicked(ad: MaxAd) {
                Log.d(TAG, "onNativeAdClicked:")
            }
        }

        nativeAdLoader = MaxNativeAdLoader(MOPUB_ID, context)
        nativeAdLoader.setNativeAdListener(nativeAdListener)

        if (mIsSmall) {
            nativeAdLoader.loadAd(createNativeAdViewForMin())
        } else {
//            nativeAdLoader.loadAd()
            nativeAdLoader.loadAd(createNativeAdViewForBig())
        }
    }

    private fun createNativeAdViewForMin(): MaxNativeAdView
    {
        val binder: MaxNativeAdViewBinder =
            MaxNativeAdViewBinder.Builder(R.layout.d_native_min_layout)
                .setTitleTextViewId(R.id.d_nativetitletextview)
                .setAdvertiserTextViewId(R.id.d_nativeadvertisertextview)
                .setIconImageViewId(R.id.d_min_native_icon)
                .setOptionsContentViewGroupId(R.id.d_optionsview)
                .setBodyTextViewId(R.id.d_nativebodytextview)
                .setCallToActionButtonId(R.id.d_nativeadaction)
                .build()
        return MaxNativeAdView(binder, context)
    }

    private fun createNativeAdViewForBig(): MaxNativeAdView{

        var builderExtend = ApproveNativeBuilder(R.layout.d_native_big_layout)
        builderExtend.setTitleTextViewId(R.id.d__title_text_view)
        builderExtend.setAdvertiserTextViewId(R.id.d_advertiser_text_view)
        builderExtend.setBodyTextViewId(R.id.d_body_text_view)
        builderExtend.setIconImageViewId(R.id.d_icon_image_view)
        builderExtend.setIconContentView(R.id.d_icon_view)
        builderExtend.setOptionsContentViewGroupId(R.id.options_view)
        builderExtend.setOptionsContentFrameLayout(R.id.options_view)
        builderExtend.setMediaContentViewGroupId(R.id.d_media_content_view)
        builderExtend.setMediaContentFrameLayout(R.id.d_media_content_view)
        builderExtend.setCallToActionButtonId(R.id.d_cta_button)
        val binder: MaxNativeAdViewBinder = builderExtend.build()

        return MaxNativeAdView(binder, context)
    }
}