package com.example.tubedown.rereads

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.applovin.mediation.MaxSegment
import com.applovin.mediation.MaxSegmentCollection
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.example.BuildConfig
import com.google.android.gms.common.util.CollectionUtils
import java.io.UnsupportedEncodingException
import java.util.LinkedList
import java.util.Queue


abstract class AdCenter {
    var queue: Queue<Int> = LinkedList<Int>()

    companion object {
        val TAG = "AdCenter"

        val isShowAds = true

        fun isAdShow(): Boolean {
//            if (MyApp.getInstance().packageName.contains("pro", true)){
//                return false
//            }
            if (BuildConfig.DEBUG) {
                return false
            }

            return true
        }


        val Ad_Type_Admob = 1
        val Ad_Type_Facebook = 2
        val Ad_Type_Mopub = 3
        val Ad_Type_IRONSOURCE = 4
        val Ad_Type_Inmobi = 5

        var MoPubInited = false
        var InmobiInited = false;

//        fun initAds(context: Context){
//            if(!isAdShow()){
//                return
//            }
////            AudienceNetworkAds.initialize(context)
//        }


        fun initMobpuAd(activity: Context) {
            if (!isAdShow()) {
                return
            }


////            var ironSourceSettings: HashMap<String, String> = HashMap()
////            ironSourceSettings.put("​applicationKey​", AdUnit.IRONSRC_APP_KEY)
//
//            var inMobiSettings: HashMap<String, String> = HashMap()
//            inMobiSettings.put("accountid", BaseCommon.decodeToString("YmE5YjFhMjRhMjVhNDQxMThjZGRkOWQwNzQ3ZmNhM2U=")) //"ba9b1a24a25a44118cddd9d0747fca3e"
//
//            //Mintegral
////            // Declare your Mintegral app ID and app key
////            val mintegralConfigs: MutableMap<String, String> = HashMap()
////            mintegralConfigs["appId"] =  AdUnit.MINTEGRAL_APP_ID
////            mintegralConfigs["appKey"] = BaseCommon.decodeToString("MGY3OWI3NmQxY2E0NDFlNzk5YmRhYmI3NjUzZDQ1ODA=")//"0f79b76d1ca441e799bdabb7653d4580"
//
//
//            val sdkConfiguration = SdkConfiguration.Builder(AdUnit.ADUNIT_banner)
////                    .withMediatedNetworkConfiguration(IronSourceAdapterConfiguration::class.java.name, ironSourceSettings)
//                    .withMediatedNetworkConfiguration(InMobiAdapterConfiguration::class.java.name, inMobiSettings)
////                    .withMediatedNetworkConfiguration(MintegralAdapterConfiguration::class.java.name, mintegralConfigs)
////                    .withLogLevel(MoPubLog.LogLevel.DEBUG)
//                    .build()
//
//            MoPub.initializeSdk(activity, sdkConfiguration, object : SdkInitializationListener {
//                override fun onInitializationFinished() {
//                    MoPubInited = true
//                }
//
//            })

            // Create the initialization configuration
            var decodeKey =
                MydecodeToString("aXh5XzRIXzBUc0ljZXh1S0R0dHFIV0tOYVpxMkZVZ0RXRjl3UnVsbnF3NVQ0Mkk4TjZtM0hjXzFuZ1MyaV9hR2ZzWmJfT2dDcTNQdkE4c1FCVHFsTkNKN0VU")
            val initConfig = AppLovinSdkInitializationConfiguration.builder(decodeKey)
                .setMediationProvider(AppLovinMediationProvider.MAX)
                .setSegmentCollection(
                    MaxSegmentCollection.builder()
                        .addSegment(MaxSegment(849, CollectionUtils.listOf(1, 3)))
                        .build()
                )
                .build()


            val settings = AppLovinSdk.getInstance(activity).settings
            settings.termsAndPrivacyPolicyFlowSettings.isEnabled = false
            settings.termsAndPrivacyPolicyFlowSettings.privacyPolicyUri =
                Uri.parse("https://sites.google.com/view/tnvddown/home")


            AppLovinSdk.getInstance(activity).initialize(initConfig) { sdkConfig ->
                // Start loading ads
                MoPubInited = true
                android.util.Log.d("AdCenter", "MoPubInited")
//                AppLovinSdk.getInstance(activity).showMediationDebugger()
                MyCommon.loadFullScreen(activity)
            }
        }

//        fun initInmobi(activity: Context){
//            if(!isAdShow()){
//                return
//            }
//            // OPTIONAL: Prepare InMobi GDPR Consent
//            var consentObject = JSONObject()
//            try {
//                consentObject.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, true);
//
//                // If you obtain your consent outside and before MoPub initialization sequence, and have it available at this time, you can include it here.
//                consentObject.put("gdpr", "0"); // "0" or "1"
////                consentObject.put(InMobiSdk.IM_GDPR_CONSENT_IAB, “ << consent in IAB format >> ”);
//            } catch (e: Exception) {
//                e.printStackTrace();
//            }
//
//            InMobiSdk.init(activity,
//                MydecodeToString("aXh5X2JhOWIxYTI0YTI1YTQ0MTE4Y2RkZDlkMDc0N2ZjYTNl"), consentObject, object : com.inmobi.sdk.SdkInitializationListener {
//
//                override fun onInitializationComplete(p0: java.lang.Error?) {
//                    if (null != p0) {
//                        Log.e(TAG, "InMobi Init failed -" + p0.message)
//                    } else {
//                        Log.d(TAG, "InMobi Init Successful")
//                        InmobiInited = true
//                    }
//                    initMobpuAd(activity)
//                }
//            })
//        }

        fun MydecodeToString(str: String): String? {
            try {
                val mystr =
                    String(Base64.decode(str.toByteArray(charset("UTF-8")), Base64.DEFAULT))
                if (mystr.length > 4) {
                    return mystr.substring(4)
                }
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            return ""
        }

        open fun decodeToString(str: String): String {
            try {
                return String(Base64.decode(str.toByteArray(charset("UTF-8")), Base64.DEFAULT))
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            return ""
        }
    }

    abstract fun initQueue()


}