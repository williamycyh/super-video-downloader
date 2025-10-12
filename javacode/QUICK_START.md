# 🚀 快速开始 - 3行代码搞定！

## Java项目中使用

```java
import com.ytdlp.YtDlpJava;

YtDlpJava ytdlp = new YtDlpJava();
YtDlpJava.DownloadResult result = ytdlp.download("你的视频URL");
System.out.println(result.isSuccess() ? "下载成功!" : "下载失败");
```

## Android项目中使用

### 1. 复制源码
把 `src/main/java/com/ytdlp/` 复制到你的Android项目

### 2. 添加权限
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 3. 开始使用
```java
new Thread(() -> {
    YtDlpJava ytdlp = new YtDlpJava();
    YtDlpJava.DownloadResult result = ytdlp.download("你的视频URL");
    
    runOnUiThread(() -> {
        Toast.makeText(this, result.isSuccess() ? "下载成功!" : "下载失败", Toast.LENGTH_SHORT).show();
    });
}).start();
```

## 命令行使用

```bash
java -cp . com.ytdlp.YtDlpMain https://www.facebook.com/watch/?v=123456789
```

## 支持的平台

✅ Facebook, Instagram, TikTok, Pornhub, XHamster, XNXX, XVideos, Twitter, Dailymotion, Vimeo

## 就这么简单！

3行代码就能下载10个平台的视频，无需复杂配置！🎉
