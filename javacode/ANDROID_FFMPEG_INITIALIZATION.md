# Android FFmpeg初始化指南

## 📱 Android环境中的FFmpeg初始化

### 1. **FFmpegKit依赖配置**

在您的Android项目的`build.gradle`中添加FFmpegKit依赖：

```gradle
dependencies {
    // FFmpegKit for Android
    implementation 'com.arthenica:ffmpeg-kit-full:6.0-2'
}
```

### 2. **Android权限配置**

在`AndroidManifest.xml`中添加必要的权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

### 3. **Application类初始化**

在您的Application类中初始化FFmpegKit：

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化FFmpegKit
        initializeFFmpegKit();
    }
    
    private void initializeFFmpegKit() {
        try {
            // FFmpegKit在Android中会自动初始化
            // 但我们可以设置一些配置
            FFmpegKitConfig.setLogLevel(Level.AV_LOG_INFO);
            FFmpegKitConfig.enableStatisticsCallback(new StatisticsCallback() {
                @Override
                public void apply(Statistics statistics) {
                    // 处理统计信息
                    Log.d("FFmpegKit", "Time: " + statistics.getTime() + 
                          ", Size: " + statistics.getSize());
                }
            });
            
            Log.i("FFmpegKit", "FFmpegKit初始化完成");
            
        } catch (Exception e) {
            Log.e("FFmpegKit", "FFmpegKit初始化失败: " + e.getMessage());
        }
    }
}
```

### 4. **YtDlpJava使用方式**

在Android中使用YtDlpJava：

```java
public class VideoDownloadManager {
    
    public void downloadVideo(String url, String outputPath) {
        try {
            // 创建YtDlpJava实例
            YtDlpJava ytdlpJava = new YtDlpJava();
            
            // 提取视频信息
            VideoInfo videoInfo = ytdlpJava.extractInfo(url);
            if (videoInfo == null) {
                Log.e("VideoDownload", "无法提取视频信息");
                return;
            }
            
            // 选择最佳格式
            VideoFormat bestFormat = selectBestFormat(videoInfo.getFormats());
            if (bestFormat == null) {
                Log.e("VideoDownload", "没有找到合适的格式");
                return;
            }
            
            // 下载视频
            DownloadResult result = ytdlpJava.downloadFormat(bestFormat, outputPath);
            
            if (result.isSuccess()) {
                Log.i("VideoDownload", "下载成功: " + result.getFilePath());
                // 处理下载成功
            } else {
                Log.e("VideoDownload", "下载失败: " + result.getErrorMessage());
                // 处理下载失败
            }
            
        } catch (Exception e) {
            Log.e("VideoDownload", "下载过程中发生错误: " + e.getMessage());
        }
    }
    
    private VideoFormat selectBestFormat(List<VideoFormat> formats) {
        // 优先选择HLS格式
        for (VideoFormat format : formats) {
            if ("hls".equals(format.getProtocol()) || format.getUrl().contains(".m3u8")) {
                return format;
            }
        }
        
        // 如果没有HLS格式，选择第一个
        return formats.isEmpty() ? null : formats.get(0);
    }
}
```

### 5. **Activity中的使用示例**

```java
public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 检查权限
        checkPermissions();
        
        // 开始下载
        downloadVideo();
    }
    
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }
    
    private void downloadVideo() {
        new Thread(() -> {
            VideoDownloadManager manager = new VideoDownloadManager();
            String outputPath = getExternalFilesDir(null) + "/downloaded_video.mp4";
            manager.downloadVideo("https://www.dailymotion.com/video/x9s1tzq", outputPath);
        }).start();
    }
}
```

### 6. **FFmpegKit配置选项**

```java
// 设置日志级别
FFmpegKitConfig.setLogLevel(Level.AV_LOG_DEBUG);

// 启用统计回调
FFmpegKitConfig.enableStatisticsCallback(new StatisticsCallback() {
    @Override
    public void apply(Statistics statistics) {
        // 更新进度
        updateProgress(statistics.getTime(), statistics.getSize());
    }
});

// 设置会话回调
FFmpegKitConfig.setSessionCallback(new SessionCallback() {
    @Override
    public void apply(Session session) {
        // 处理会话状态
        if (ReturnCode.isSuccess(session.getReturnCode())) {
            Log.i("FFmpegKit", "命令执行成功");
        } else {
            Log.e("FFmpegKit", "命令执行失败: " + session.getFailStackTrace());
        }
    }
});
```

### 7. **错误处理和回退机制**

```java
public class RobustVideoDownloader {
    
    public void downloadWithFallback(String url, String outputPath) {
        try {
            YtDlpJava ytdlpJava = new YtDlpJava();
            VideoInfo videoInfo = ytdlpJava.extractInfo(url);
            
            if (videoInfo == null) {
                throw new Exception("无法提取视频信息");
            }
            
            // 尝试FFmpeg方案
            VideoFormat hlsFormat = findHlsFormat(videoInfo.getFormats());
            if (hlsFormat != null) {
                try {
                    DownloadResult result = ytdlpJava.downloadFormat(hlsFormat, outputPath);
                    if (result.isSuccess()) {
                        Log.i("Download", "FFmpeg方案成功");
                        return;
                    }
                } catch (Exception e) {
                    Log.w("Download", "FFmpeg方案失败，尝试纯Java方案: " + e.getMessage());
                }
            }
            
            // 回退到纯Java方案
            DownloadResult result = ytdlpJava.download(url, outputPath + ".java");
            if (result.isSuccess()) {
                Log.i("Download", "纯Java方案成功");
            } else {
                throw new Exception("所有下载方案都失败了");
            }
            
        } catch (Exception e) {
            Log.e("Download", "下载失败: " + e.getMessage());
        }
    }
}
```

### 8. **ProGuard配置**

如果使用ProGuard，需要在`proguard-rules.pro`中添加：

```proguard
# FFmpegKit
-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**

# YtDlpJava
-keep class com.ytdlp.** { *; }
-dontwarn com.ytdlp.**
```

## 🎯 总结

Android版本的FFmpeg初始化包括：

1. **依赖配置**: 添加FFmpegKit依赖
2. **权限设置**: 配置必要的Android权限
3. **Application初始化**: 在Application中初始化FFmpegKit
4. **自动检测**: YtDlpJava会自动检测Android环境并使用FFmpegKit
5. **回退机制**: 如果FFmpegKit不可用，自动回退到纯Java方案

这样配置后，您的Android应用就可以使用FFmpeg进行高效的视频处理了！🚀
