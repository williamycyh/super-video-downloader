package com.example.tubedown.rereads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.tubedown.rereads.Utils;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
//import com.inmobi.ads.InMobiInterstitial;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;


public class MyCommon {


    public static AdFullScreenAd adFullScreenAd = new AdFullScreenAd();

    static Context sActivity;
    static int sAdType = 0;
//    static InMobiInterstitial sInMobiInterstitial;
    //    static MBNewInterstitialHandler sMbInterstitalVideoHandler;
    static boolean sfullscreenShowRightWay = false;
    static AdFullScreenAd.FullScreenLoadResult fullScreenAdInterface = new AdFullScreenAd.FullScreenLoadResult() {
        @Override
        public void onAdDisplayFailed() {
        }

        @Override
        public void onAdLoadFailed() {
        }

        @Override
        public void onAdClicked() {
        }

        @Override
        public void onAdHidden() {
            loadFullScreen(sActivity);//load next
        }

        @Override
        public void onAdDisplayed() {
        }

        @Override
        public void onAdLoaded() {
            if(sfullscreenShowRightWay){
                showFullScreen(sActivity);
                sfullscreenShowRightWay = false;
            }
        }
    };

    public static void loadFullScreenAndShow(Context activity){
        if(!Utils.canShowFullAd(activity)){
            return;
        }
        sfullscreenShowRightWay = true;
        sActivity = activity;
        adFullScreenAd.setFullScreenLoadResult(fullScreenAdInterface);
        adFullScreenAd.loadFullScreen(activity, AdUnit.ADUNIT_FULLSCREEN);
    }

    public static void loadFullScreen(Context activity){
        if(!Utils.canShowFullAd(activity)){
            return;
        }
        sActivity = activity;
        adFullScreenAd.setFullScreenLoadResult(fullScreenAdInterface);
        adFullScreenAd.loadFullScreen(activity, AdUnit.ADUNIT_FULLSCREEN);
    }

    public static boolean showFullScreen(Context activity){
        if(!Utils.canShowFullAd(activity)){
            return false;
        }
        Toast.makeText(activity, "Loading...Take a break while watching an AD.",Toast.LENGTH_LONG).show();
        return adFullScreenAd.showAd();
    }

    /////////////////////////////
//    public static void loadFullScreenAction(Activity activity){
//        if(sInMobiInterstitial == null || ){
//            loadFullScreen(activity);
//        }
//    }
//
//    public static boolean showFullScreenAction(Activity activity){
//        return showFullScreen(activity);
//    }
    ///////////////////////////

    /////////////////////////////

    public static boolean showFullScreenDetail(Activity activity){
        return showFullScreen(activity);
    }
    ///////////////////////////

    AdNativeAdCenter minNativeCenter = new AdNativeAdCenter();

    public void loadBigNative(Activity activity, FrameLayout containner){
        if(activity.isFinishing() || containner == null){
            return;
        }
        if(!Utils.canShowFullAd(activity)){
            return;
        }

        minNativeCenter.setNativeLoadResult(new AdNativeAdCenter.NativeLoadResult() {
            @Override
            public void onResult(boolean success) {
                if(!success){
//                    loadBigBanner(activity, containner);
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            containner.setVisibility(View.GONE);
                        }
                    });
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            containner.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
        minNativeCenter.loadNativeAds(activity, containner, AdUnit.ADUNIT_NATIVE,0L,false);
    }

    public void loadMinNative(Activity activity, FrameLayout containner){
        if(activity.isFinishing()){
            return;
        }
        String mopubId = AdUnit.ADUNIT_MIN_NATIVE;

        minNativeCenter.setNativeLoadResult(new AdNativeAdCenter.NativeLoadResult() {
            @Override
            public void onResult(boolean success) {
                if(!success){
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            containner.setVisibility(View.GONE);
                        }
                    });
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            containner.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }
        });
        minNativeCenter.loadNativeAds(activity, containner, mopubId, 0L, true);

    }

//    AdBannerAdCenter adBigBannerAdCenter = new AdBannerAdCenter();
//    public void loadBigBanner(Activity activity, FrameLayout containner){
//        if(activity.isFinishing() || containner == null){
//            return;
//        }
//
//        adBigBannerAdCenter.loadBannerAds(activity, containner, true);
//    }
//
//
//    AdBannerAdCenter adBannerAdCenter = new AdBannerAdCenter();
//    public void loadBanner(Activity activity, FrameLayout containner){
//        if(activity.isFinishing() || containner == null){
//            return;
//        }
//
//        adBannerAdCenter.loadBannerAds(activity, containner, false);
//    }


    public static void googleRate(Activity activity){

        ReviewManager manager = ReviewManagerFactory.create(activity);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // We can get the ReviewInfo object
                ReviewInfo reviewInfo = task.getResult();
                Task<Void> flow = manager.launchReviewFlow(activity, reviewInfo);
                flow.addOnCompleteListener(flowtask -> {
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even whether the review dialog was shown. Thus, no
                    // matter the result, we continue our app flow.
                    if(flowtask != null){
                        if(flowtask.isSuccessful()){
                            Toast.makeText(activity, "Success", Toast.LENGTH_LONG).show();
                        } else {
                            Exception exception = flowtask.getException();
                            if(exception != null){
                                Toast.makeText(activity, exception.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
            } else {
                // There was some problem, log or handle the error code.
                Exception exception =  task.getException();
                if(exception != null){
                    Toast.makeText(activity, exception.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        });
    }



//    public static boolean canShowFullAd(Activity activity){
//        if(!isShowByDev(activity)){
//            return false;
//        }
//        if(appCon.v_version == BuildConfig.VERSION_CODE){
//            return false;
//        }
//        return true;
//    }


//    public static boolean isInstallMiApps(Context context){
//        boolean player = checkAppInstalled("com.miui.player", context);
//        boolean fileex = checkAppInstalled("com.mi.android.globalFileexplorer", context);
//        Log.d("commons","player " + player +" fileex " +fileex);
//        if(player || fileex){
//            return true;
//        }
//        return false;
//    }

//    public static boolean isTimeZoneUK() {
//        String timeZone = TimeZone.getDefault().getID();
//        Log.d("commons","timezone:"+timeZone);
//        boolean contains = false;
//        if(!TextUtils.isEmpty(timeZone)){
//            contains = timeZone.toLowerCase().contains("london");
//            Log.d("commons","timezone:"+contains);
//        }
//        return contains;
//    }

    public static boolean isUKCountry(Context context){
        String country = getSimContryISO(context);//has sim
        Log.d("commons","isUKCountry country:"+country);
        if(!TextUtils.isEmpty(country)){
            if(country.toLowerCase().contains("gb")
                    || country.toLowerCase().contains("uk")){
                return true;
            }
        }
        String timeZone = TimeZone.getDefault().getID();
        Log.d("commons","timezone:"+timeZone);
        if(!TextUtils.isEmpty(timeZone)){
            if(timeZone.toLowerCase().contains("london")){
                Log.d("commons","timezone london");
                return true;
            }
        }

        String localCountry = Locale.getDefault().getCountry();
        Log.d("commons","isUKCountry localCountry:"+localCountry);
        if(!TextUtils.isEmpty(localCountry)){
            if(localCountry.toLowerCase().contains("gb")
                    || localCountry.toLowerCase().contains("uk")){
                return true;
            }
        }

        return false;
    }

    public static boolean isEnLanguage(){
        String language = Locale.getDefault().getLanguage();
        Log.d("commons","getLanguage language:"+language);//en
        if(!TextUtils.isEmpty(language)){
            if(language.toLowerCase().contains("en")){
                return true;
            } else {
                return false;
            }
        }
        return true;
    }


    public static boolean isShowByDev(Context context){
        if(!pinAc(context)){
            ASharePreferenceUtils.putInt (context, "show_reson", 2);
            Log.d("commons","show_pinAc");
            return false;
        }
        if(1 == isDevMode(context)){
            Log.d("commons","show_DevMode");
            ASharePreferenceUtils.putInt (context, "show_reson", 3);
            return false;
        }
        return true;
    }

    public static boolean checkPhotoNum(long num, Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int numCount = getAllShownImagesPath(context);
            if(numCount > num){
                return true;
            } else {
                return false;
            }
        }
        File photoDir[] = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).listFiles();

        if(photoDir == null){
            return false;
        }
        for(File dir : photoDir){
            if(dir.getName().contains("Camera") ||
                    dir.getName().contains("Cam")){
                if(dir.listFiles() != null){
                    int numCount = dir.listFiles().length;
                    Log.d("commons","numCount:"+numCount + " " + num);
                    if(numCount > num){
                        return true;
                    }
                }
            }

        }
        return false;
    }

    /**
     * Getting All Images Path
     *
     * @param activity
     * @return ArrayList with images Path
     */
    public static int getAllShownImagesPath(Context activity) {
        int count = 0;
        ArrayList<String> listOfAllImages = new ArrayList<String>();
        try{
            Uri uri;
            Cursor cursor;
            int column_index_data, column_index_folder_name;

            String absolutePathOfImage = null;
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            String[] projection = { MediaStore.MediaColumns.DATA,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME };

            cursor = activity.getContentResolver().query(uri, projection, null,
                    null, null);

            column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            column_index_folder_name = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            while (cursor.moveToNext()) {
                absolutePathOfImage = cursor.getString(column_index_data);

                if(absolutePathOfImage.contains("/Cam")){
                    count = count + 1;
                }
//            listOfAllImages.add(absolutePathOfImage);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return count;
    }

    public static boolean isShowByPhoto(Context context){
//        int photoNum = getPhotoNum();
        long photos = 45;//App.appCon.ph_num;
        if(photos <= 0){
            photos = 45;
        }
        boolean photoEnough = checkPhotoNum(photos, context);
        if(!photoEnough){
            ASharePreferenceUtils.putInt (context, "show_reson", 1);
            Log.d("commons","show_photoNum false");
            return false;
        }

        return true;
    }



    public static boolean hasSocialMedia(Context context){
        return checkAppInstalled("com.facebook.katana", context)
                || checkAppInstalled("com.whatsapp", context)
                || checkAppInstalled("com.facebook.orca", context)
                || checkAppInstalled("jp.naver.line.android", context)
                || checkAppInstalled("com.kakao.talk", context)
                || checkAppInstalled("com.tencent.mm", context)
                || checkAppInstalled("com.skype.raider", context);
    }

    public static boolean checkAppInstalled(String pkgName, Context context) {
        if (pkgName== null || pkgName.isEmpty()) {
            return false;
        }
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            e.printStackTrace();
        }
        if(packageInfo == null) {
            return false;
        } else {
            return true;//true为安装了，false为未安装
        }
    }

    public static int getPhotoNum(){
        Object localObject1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        int i;
        if (((File)localObject1).listFiles() != null) {
            i = ((File)localObject1).listFiles().length;
        } else {
            i = 0;
        }
        return i;
    }

    public static boolean pinAc(Context context){
        Log.d("commons","pinAc：start");
        Intent localObject1 = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        int j = ((Intent)localObject1).getIntExtra("status", -1);
        String pin_status;
        String pin_ac;
        if ((j != 2) && (j != 5))
        {
            pin_status = "0";
            pin_ac = "";
        }
        else
        {
            j = ((Intent)localObject1).getIntExtra("plugged", -1);
            if (j == 2) {
                pin_ac = "usb";
            } else if (j == 1) {
                pin_ac = "ac";
            } else {
                pin_ac = "";
            }
            pin_status = "1";
        }
        Log.d("commons","pinAc："+pin_ac +"  " + pin_status);
        if("1".equalsIgnoreCase(pin_status) && "usb".equalsIgnoreCase(pin_ac)){//usb 充电
            return false;
        }
        Log.d("commons","pinAc：end");
        return true;
    }

    public static int isDevMode(Context paramContext)
    {
        return Settings.Secure.getInt(paramContext.getContentResolver(), "development_settings_enabled", 0);
    }

    public static String getCountry(Context context){

        String country = getSimContryISO(context);//has sim
        return country;
//        if(!TextUtils.isEmpty(country)){
//            return country;
//        }

//        Locale locale;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            locale = getResources().getConfiguration().getLocales().get(0);
//        } else {
//            locale = getResources().getConfiguration().locale;
//        }
//        return locale.getCountry();
    }

    public static String getSimContryISO(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if(tm == null){
            return "";
        }
        String countryIso = tm.getSimCountryIso();
        if (!TextUtils.isEmpty(countryIso)) {
            countryIso = countryIso.toLowerCase(Locale.US);
            return countryIso;
        }
        return "";
    }



}
