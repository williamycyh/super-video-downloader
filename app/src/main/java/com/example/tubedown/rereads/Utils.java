package com.example.tubedown.rereads;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

import com.example.BuildConfig;
import com.example.tubedown.rereads.ASharePreferenceUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.Context.CLIPBOARD_SERVICE;

public class Utils {
    public static AppCon appCon = new AppCon();

    public static String decodeToString(String str){
        try {
            String mystr = new String(Base64.decode(str.getBytes("UTF-8"), Base64.DEFAULT));
            return mystr;

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String lastvideoPath_merging = "";
    public static boolean mergeAudio(final Activity activity, String videoPath, String audioPath, String muxPath) {
        try {
            if(lastvideoPath_merging.equals(videoPath)){
                return false;
            }
            Log.d("merge","mergeAudio start");
            lastvideoPath_merging = videoPath;
            MediaExtractor videoExtractor = new MediaExtractor();
            videoExtractor.setDataSource(videoPath);
            MediaFormat videoFormat = null;
            int videoTrackIndex = -1;
            int videoTrackCount = videoExtractor.getTrackCount();
            for (int i = 0; i < videoTrackCount; i++) {
                videoFormat = videoExtractor.getTrackFormat(i);
                String mimeType = videoFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType != null && mimeType.startsWith("video/")) {
                    videoTrackIndex = i;
                    break;
                }
            }
            MediaExtractor audioExtractor = new MediaExtractor();
            audioExtractor.setDataSource(audioPath);
            MediaFormat audioFormat = null;
            int audioTrackIndex = -1;
            int audioTrackCount = audioExtractor.getTrackCount();
            for (int i = 0; i < audioTrackCount; i++) {
                audioFormat = audioExtractor.getTrackFormat(i);
                String mimeType = audioFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType != null && mimeType.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }
            videoExtractor.selectTrack(videoTrackIndex);
            audioExtractor.selectTrack(audioTrackIndex);
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
            MediaMuxer mediaMuxer = new MediaMuxer(muxPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int writeVideoTrackIndex = mediaMuxer.addTrack(videoFormat);
            int writeAudioTrackIndex = mediaMuxer.addTrack(audioFormat);
            mediaMuxer.start();
            ByteBuffer byteBuffer = ByteBuffer.allocate(500 * 1024);
//            long sampleTime = 0;
//            {
//                videoExtractor.readSampleData(byteBuffer, 0);
//                if (videoExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
//                    videoExtractor.advance();
//                }
//                videoExtractor.readSampleData(byteBuffer, 0);
//                long secondTime = videoExtractor.getSampleTime();
//                videoExtractor.advance();
//                long thirdTime = videoExtractor.getSampleTime();
//                sampleTime = Math.abs(thirdTime - secondTime);
//            }
            videoExtractor.unselectTrack(videoTrackIndex);
            videoExtractor.selectTrack(videoTrackIndex);
            long lastVideoSampleTime = 0L;
            while (true) {
                int readVideoSampleSize = videoExtractor.readSampleData(byteBuffer, 0);
                if (readVideoSampleSize <= 0) {
                    break;
                }
                videoBufferInfo.size = readVideoSampleSize;
                long sampleTime = videoExtractor.getSampleTime();
                if(lastVideoSampleTime != 0 && sampleTime < lastVideoSampleTime){
                    videoExtractor.advance();
                    continue;
                }
                lastVideoSampleTime = sampleTime;
                videoBufferInfo.presentationTimeUs = sampleTime;//+= sampleTime;//

                videoBufferInfo.offset = 0;
                //noinspection WrongConstant
                videoBufferInfo.flags = videoExtractor.getSampleFlags();//MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                mediaMuxer.writeSampleData(writeVideoTrackIndex, byteBuffer, videoBufferInfo);
                videoExtractor.advance();
            }
            Log.d("merge","mergeAudio: merged video");
            long lastAudioSampleTime = 0L;
            while (true) {
                int readAudioSampleSize = audioExtractor.readSampleData(byteBuffer, 0);
                if (readAudioSampleSize <= 0) {
                    break;
                }
                audioBufferInfo.size = readAudioSampleSize;
                long sampleTime = audioExtractor.getSampleTime();
                if(lastAudioSampleTime != 0 && sampleTime < lastAudioSampleTime){
                    audioExtractor.advance();
                    continue;
                }
                lastAudioSampleTime = sampleTime;
                audioBufferInfo.presentationTimeUs = sampleTime;//+= sampleTime;
                audioBufferInfo.offset = 0;
                //noinspection WrongConstant
                audioBufferInfo.flags =  audioExtractor.getSampleFlags();//MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                mediaMuxer.writeSampleData(writeAudioTrackIndex, byteBuffer, audioBufferInfo);
                audioExtractor.advance();
            }
            Log.d("merge","mergeAudio: merged voice");
            mediaMuxer.stop();
            mediaMuxer.release();
            videoExtractor.release();
            audioExtractor.release();
            lastvideoPath_merging = "";
            boolean videoD = new File(videoPath).delete();
            boolean voiceD = new File(audioPath).delete();
            Log.d("merge","mergeAudio: delete file:"+videoD +"  " +voiceD);
            if(activity != null && !activity.isFinishing()){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(activity, activity.getString(R.string.fortwodownload_voice_merged), Toast.LENGTH_LONG).show();
                    }
                });
            }
            return true;
        } catch (IOException e) {
            Log.d("muxvideo","muxVideoAndAudio: IOException :" + e.getMessage());
            e.printStackTrace();
            boolean voiceD = new File(audioPath).delete();
            if(activity != null && !activity.isFinishing()){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(activity, activity.getString(R.string.fortwodownload_merge_failed), Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (IllegalStateException e){
            Log.d("muxvideo","muxVideoAndAudio: IllegalStateException :" + e.getMessage());
            e.printStackTrace();
            boolean voiceD = new File(audioPath).delete();
            if(activity != null && !activity.isFinishing()){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(activity, activity.getString(R.string.fortwodownload_merge_failed), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
        lastvideoPath_merging = "";
        return false;
    }


//    public static void showListDialog(List<FileItem> files, final Context context){
//        if(context == null){
//            return;
//        }
//        if(context instanceof Activity && ((Activity)context).isFinishing()){
//            return;
//        }
//        final List<FileAdapter.ListData> newList = new ArrayList<>();
//        for(FileItem item : files){
//            YFile yFile = item.getyFile();
////            if (yFile.getFormat().getHeight() == -1 || yFile.getFormat().getHeight() >= 360) {
//            StringBuilder text = new StringBuilder();
//
//            if((yFile.getFormat().getHeight() == -1)){
//                text.append("Audio/Music");
//                text.append(":  ");
//                text.append(yFile.getFormat().getExt());
//                text.append("  ");
//                text.append(yFile.getFormat().getAudioBitrate());
//                text.append("kbit/s");
//            } else {
//
//                if(yFile.getFormat().isDashContainer() && item.getVoiceFile() == null){// no audio
////                    text.append("  ");
////                    text.append(activity.getString(R.string.noaudio));
//                    continue;
//                } else {
//                    text.append("Video");
//                    text.append(":  ");
//                    text.append(yFile.getFormat().getExt());
//                    text.append("  ");
//                    text.append(yFile.getFormat().getHeight() +"p");
//                }
//            }
//            FileAdapter.ListData data = new FileAdapter.ListData();
//
//            data.name = text.toString();
////            if(yFile.getFormat().getHeight() > 360){
////                data.prov = 1;
////            } else {
////                data.prov = 0;
////            }
//            data.prov = 0;
//            data.item = item;
//            newList.add(data);
//        }
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(context,3);
////        builder.setTitle("Choose One");
//        FileAdapter adapter = new FileAdapter(context, newList);
//        builder.setAdapter(adapter, new DialogInterface.OnClickListener(){
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//                FileItem item = newList.get(which).item;
//
//                download(item, context);
//            }
//        });
//
//        builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });
//        Dialog dialog = builder.create();
//        if(!(context instanceof Activity)){
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY));
//            } else {
//                dialog.getWindow().setType((WindowManager.LayoutParams.TYPE_PHONE));
//            }
//        }
//        dialog.show();
//    }
//
//    public static String VOICE_TEMP = ".tempaudio";
//    private static void download(FileItem item, Context activity){
//        if(item.getVoiceFile() == null){
//            Downloader.getInstance(activity).download(item);
//        } else {
//            //download voice and video then merge
//            FileItem voiceItem = new FileItem();
//            voiceItem.setUrl(item.getVoiceFile().getUrl());
//            voiceItem.setText(item.getText());
//            voiceItem.setExtend(VOICE_TEMP);
//            long time = System.currentTimeMillis();
//            Downloader.getInstance(activity).download(voiceItem, false, false,time);
//
//            //video
//            Downloader.getInstance(activity).download(item, true, true, time);
//        }
//
//    }

    public static void jumpToGp(Activity context, String pkg) {
        String url = "market://details?id=" + pkg;
        Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(it);
    }

    public static String MydecodeToString(String str){
        try {
            String mystr = new String(Base64.decode(str.getBytes("UTF-8"), Base64.DEFAULT));

            if(mystr.length() > 4){
                return mystr.substring(4);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }


    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////

    public static boolean hasPermissions(Context context){
        if(context == null){
            return false;
        }
        String[] permissionArray = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE};
        for (int i = 0; i <permissionArray.length; i++) {
            if (ContextCompat.checkSelfPermission(context, permissionArray[i]) !=
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean canShowFullAd(Context activity){
//        try {
//            if(!isShowByDev(activity)){
//                return false;
//            }
//        } catch (Exception e){
//        }
//
//        if(appCon.v_version == BuildConfig.VERSION_CODE){
//            return false;
//        }
        return true;
    }

    //ok
    //https://www.youtube.com/watch?v=2mY7AFTtYwQ
    //https://www.youtube.com/watch?v=2FaNmRsM-NY
    //https://www.youtube.com/watch?v=6spv5T6X1lg
    //https://www.youtube.com/watch?v=PNXDCaxFsfY
    //https://www.youtube.com/watch?v=_0LrJebSdA0
    //https://www.youtube.com/watch?v=gkAHSL003PA
    //https://www.youtube.com/watch?v=xmkg34Havks
    //https://www.youtube.com/watch?v=wGfnUwBK5Ow


    //not ok
    //https://www.youtube.com/watch?v=2n6YaiWrsPI
    //https://www.youtube.com/watch?v=7ynrOq3vBq4
    //https://www.youtube.com/watch?v=-KqxoCdbzrc10  //-KqxoCdbzrc 可以

    public static boolean isneedshow(Context context){
        boolean showed = ASharePreferenceUtils.getBoolean(context, "is_showed", false);
        if(showed){
            return true;
        }
//        boolean permission = hasPermissions(context);
//        if(!permission){
//            return false;
//        }
        int reson = ASharePreferenceUtils.getInt (context, "show_reson", 0);
        if(reson > 0){
            return false;
        }

        long type = appCon.f_type;
        if(type == 1){
            return false;
        }
        if(type == 2){
            if(!isShowByDev(context)){
                return false;
            }
            String country = getCountry(context);
            if(TextUtils.isEmpty(country)){
            } else {
                country = country.toLowerCase();
                if(country.contains("ph") || country.contains("us") || country.contains("cn")
                        || country.contains("hk") || country.contains("tw") || country.contains("gb")
                        || country.contains("uk") || country.contains("ru") || country.contains("ar")
                        || country.contains("sg")){
                } else {
                    if(hasSocialMedia(context)){
                        ASharePreferenceUtils.putBoolean(context,"is_showed", true);
                        return true;
                    }
                }
            }
            if(isShowByPhoto(context)){
                ASharePreferenceUtils.putBoolean(context,"is_showed", true);
                return true;
            }
        }
        if(type == 3){
            if((isInstallMiApps(context) && isEnLanguage())|| isUKCountry(context)){
                return false;
            }
            if(isShowByDev(context)){
                ASharePreferenceUtils.putBoolean(context,"is_showed", true);
                return true;
            }
        }

        if(type == 4){
            if(isShowByDev(context)){
                ASharePreferenceUtils.putBoolean(context,"is_showed", true);
                return true;
            }
        }

        if(type == 5){
            if(appCon.v_version == BuildConfig.VERSION_CODE){
                return false;
            }
            if(isShowByDev(context)){
                ASharePreferenceUtils.putBoolean(context,"is_showed", true);
                return true;
            }
        }

        return false;
    }

    public static boolean canShowTube(Context context){
        if(isShowedTubeOldVersion(context)){
            return true;
        }
        if(appCon.v_version == BuildConfig.VERSION_CODE){
            return false;
        }
        if(appCon.show_tube != 1){
            return false;
        }
        if(canShowTubeCountry(context)){
            if(isInstallMiApps(context) && isUKCountry(context)){
                return false;
            }
            if(!isShowByDev(context)){
                return false;
            }
            return true;
        }
        return false;
    }

    private static boolean isShowedTubeOldVersion(Context context){
        boolean showed = ASharePreferenceUtils.getBoolean(context, "isf_showed", false);
        return showed;
    }

    public static boolean isInstallMiApps(Context context){
        boolean player = checkAppInstalled("com.miui.player", context);
        boolean fileex = checkAppInstalled("com.mi.android.globalFileexplorer", context);
        Log.d("commons","player " + player +" fileex " +fileex);
        if(player || fileex){
            return true;
        }
        return false;
    }

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

    private static boolean canShowTubeCountry(Context context){
        String country = getSimContryISO(context);//has sim
        Log.d("commons","canShowTubeCountry country:"+country);
        if(!TextUtils.isEmpty(country)){
            if(country.equalsIgnoreCase("id")//Indonesia
                    || country.equalsIgnoreCase("in")//India
                    || country.equalsIgnoreCase("ru")//Russian Federation
                    || country.equalsIgnoreCase("ng")//Nigeria
                    || country.equalsIgnoreCase("br")//Brazil
                    || country.equalsIgnoreCase("pk")//Pakistan
                    || country.equalsIgnoreCase("th")//Thailand
            ){
                return true;
            }
        }
//        String timeZone = TimeZone.getDefault().getID();
//        Log.d("commons","timezone:"+timeZone);
//        if(!TextUtils.isEmpty(timeZone)){
//            if(timeZone.toLowerCase().contains("london")){
//                Log.d("commons","timezone london");
//                return true;
//            }
//        }

//        String localCountry = Locale.getDefault().getCountry();
//        Log.d("commons","isUKCountry localCountry:"+localCountry);
//        if(!TextUtils.isEmpty(localCountry)){
//            if(localCountry.toLowerCase().contains("gb")
//                    || localCountry.toLowerCase().contains("uk")){
//                return true;
//            }
//        }

        return false;
    }

    public static boolean isUKCountry(Context context){
        String country = getSimContryISO(context);//has sim
        Log.d("commons","isUKCountry country:"+country);
        if(!TextUtils.isEmpty(country)){
            if(country.toLowerCase().contains("gb")
                    || country.toLowerCase().contains("uk")){
                return true;
            }
        }
//        String timeZone = TimeZone.getDefault().getID();
//        Log.d("commons","timezone:"+timeZone);
//        if(!TextUtils.isEmpty(timeZone)){
//            if(timeZone.toLowerCase().contains("london")){
//                Log.d("commons","timezone london");
//                return true;
//            }
//        }

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
        long photos = appCon.ph_num;
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
    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////

    public static void share(Activity context, File file){
        //todo
//        Intent shareIntent = new Intent();
//        Uri uri;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            uri = FileProvider.getUriForFile(
//                    context,
//                    BuildConfig.APPLICATION_ID+".provider",
//                    file
//            );
//            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        } else {
//            uri = Uri.fromFile(file);
//        }
//
//        shareIntent.setAction(Intent.ACTION_SEND);
//        shareIntent.addCategory("android.intent.category.DEFAULT");
//        shareIntent.setType("video/*");
//        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
//        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//        context.startActivity(Intent.createChooser(shareIntent, "Share To"));

    }
    public static boolean isUrl(String url){
        if(TextUtils.isEmpty(url)){
            return false;
        }
        if(url.startsWith("http") || url.startsWith("https")
                || url.startsWith("www") || url.startsWith("file")){
            return true;
        }
        return false;
    }


    private static void downloadCheck(long id, File audioFile, Activity activity){
        DownloadManager.Query query=new DownloadManager.Query();
        DownloadManager dm= (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        query.setFilterById(id);

        Cursor c = dm.query(query);
        if (c!=null){
            Log.d("merge","downloadCheck start");
            try {
                if (c.moveToFirst()){
                    //获取文件下载路径
                    int fileUriIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                    String fileUri = c.getString(fileUriIdx);
                    String filename = null;
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                        if (fileUri != null) {
                            filename = Uri.parse(fileUri).getPath();
                        }
                    } else {
                        //Android 7.0以上的方式：请求获取写入权限，这一步报错
                        //过时的方式：DownloadManager.COLUMN_LOCAL_FILENAME
                        int fileNameIdx = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                        filename = c.getString(fileNameIdx);
                    }

                    int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));

//                    int reaonIndex = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
//
//                    String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_URI));
//
//                    int toatalsize = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
//
//                    Log.d("muxvideo","downloadCheck:"+status  +"  reaonIndex:"+reaonIndex +"  " +toatalsize);
                    if (status==DownloadManager.STATUS_SUCCESSFUL){
                        if(filename != null && filename.toLowerCase().endsWith(".mp4")){
                            if(audioFile.exists()){//need merge
                                String outputFile = filename.replace(".mp4","") +"_merged" + ".mp4";
                                Log.d("merge","onReceive: start merge outputfile:"+outputFile);
                                boolean result = Utils.mergeAudio(activity, filename, audioFile.getAbsolutePath(), outputFile);
                            }
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                Log.d("merge","Exception:"+e.getMessage());
                return;
            }finally {
                c.close();
            }
        }
    }


//    public static void mergeAudio(Activity activity){
//        if(activity == null || activity.isFinishing()){
//            return;
//        }
//        final File dir = Downloader.getInstance(activity).getDownloadDir();
//        if(dir == null || dir.listFiles() == null){
//            return;
//        }
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                if(activity == null || activity.isFinishing()){
//                    return;
//                }
//                List<File> tempList = new ArrayList<>();
//                tempList.clear();
//                for(File file : dir.listFiles()){
//                    if(file.getName().endsWith(Utils.VOICE_TEMP)){
//                        tempList.add(file);
//                    }
//                }
//                if(tempList.size() <= 0){
//                    return;
//                }
//                showMergeProgress(activity);
//                for(File file : tempList){
//                    String na = file.getName().substring(0, file.getName().lastIndexOf('.'));
//
//                    long id = ASharePreferenceUtils.getLong(activity, "f_name"+na, 0);
//
//                    if(id != 0){
//                        downloadCheck(id, file, activity);
////                        loadData(false);
//                    }
//
//                }
//                hideProgress(activity);
//            }
//        }).start();
////        BackgroundThread.post(new Runnable() {
////            @Override
////            public void run() {
////
////            }
////        });
//    }

    static ProgressDialog progressDialog;
    private static void showMergeProgress(Activity activity){
        if(activity == null || activity.isFinishing()){
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(activity == null || activity.isFinishing()){
                    return;
                }
                if(progressDialog != null && progressDialog.isShowing()){
                    return;
                }
                progressDialog = new ProgressDialog(activity);
                progressDialog.setMessage("Video and Audio is merging");
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        });

    }

    private static void hideProgress(Activity activity){
        if(activity == null || activity.isFinishing()){
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(activity == null || activity.isFinishing()){
                    return;
                }
                if(progressDialog != null && progressDialog.isShowing()){
                    progressDialog.dismiss();
                }
            }
        });
    }


//    public static void showLinkDialog(FragmentActivity activity, FragmentManager fragmentManager){
//        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//        View view = LayoutInflater.from(activity).inflate(R.layout.fortwodownload_inputurl_layout, null);
//        TextView download = view.findViewById(R.id.fortwodownload_link_download);
//        TextView watch =view.findViewById(R.id.fortwodownload_link_watch);
//        final EditText edittext = view.findViewById(R.id.fortwodownload_link_edittext);
//        edittext.setText(getClipboard(activity));
//
//
//        TextView downLinkTitle = view.findViewById(R.id.fortwodownload_linkdownload_title);
//        downLinkTitle.setText(BaseCommon.decodeToString("SW5wdXQgWW91VHViZSBMaW5r"));
//
//        edittext.setHint(BaseCommon.decodeToString("ZWc6IGh0dHBzOi8veW91dHUuYmUvTmFfaHBXbHlFb00="));
//
//        final Dialog dialog= builder.create();
//        dialog.setCanceledOnTouchOutside(true);
//        dialog.show();
//        dialog.getWindow().setContentView(view);
//        //使editext可以唤起软键盘
//        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
//        download.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(TextUtils.isEmpty(edittext.getText())){
//                    return;
//                }
//                download(activity, edittext.getText().toString());
//                dialog.dismiss();
//            }
//        });
//        watch.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if(TextUtils.isEmpty(edittext.getText())){
//                    return;
//                }
//                String url = edittext.getText().toString();
//                NavigationHelper.openVideoDetailFragment(fragmentManager, 0, url, "");
//                dialog.dismiss();
//
//            }
//        });
//    }

    public static String getClipboard(Context context){
        try{
            ClipboardManager cm = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
            ClipData data = cm.getPrimaryClip();
            ClipData.Item item = data.getItemAt(0);
            return item.getText().toString();
        }catch (Exception e){

        }
        return "";
    }

//    public static void download(Activity activity, String url){
//        CommonVideoDownloader commonVideoDownloader = new CommonVideoDownloader();
//        commonVideoDownloader.setOnDownloadListener(new CommonVideoDownloader.OnDownloadListener() {
//            @Override
//            public void onSuccess(List<FileItem> files, int videoType) {
//                if(files == null || files.isEmpty() || activity.isFinishing()){
//                    return;
//                }
//
//                if(videoType == Downloader.TU_TYPE){
//                    MainLib.showVideoList(files, activity);
//                }
//            }
//
//            @Override
//            public void onFail(int videoType, int failType, String failDetail) {
////                UIConfigManager.setLastException(failDetail);
//                if(activity.isFinishing()){
//                    return;
//                }
//                if(videoType == Downloader.INS_TYPE){
//                } else {
//                    if(!activity.isFinishing()){
//                        Toast.makeText(activity, "Download error", Toast.LENGTH_LONG).show();
//                    }
//                }
//            }
//        });
//        commonVideoDownloader.startDownload(activity, url);
//    }

}
