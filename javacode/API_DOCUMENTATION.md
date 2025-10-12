# yt-dlp Java版本 - 超简单接入指南

## 🎯 3行代码搞定视频下载！

```java
import com.ytdlp.YtDlpJava;

YtDlpJava ytdlp = new YtDlpJava();
YtDlpJava.DownloadResult result = ytdlp.download("你的视频URL");
System.out.println(result.isSuccess() ? "下载成功!" : "下载失败");
```

## 🚀 支持的平台（10个）

✅ **Facebook, Instagram, TikTok, Pornhub, XHamster, XNXX, XVideos, Twitter, Dailymotion, Vimeo**

## 📱 Android接入（超简单）

### 1. 复制源码
把 `src/main/java/com/ytdlp/` 整个文件夹复制到你的Android项目

### 2. 添加权限
在 `AndroidManifest.xml` 添加：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 3. 开始使用
```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 下载视频
        new Thread(() -> {
            YtDlpJava ytdlp = new YtDlpJava();
            YtDlpJava.DownloadResult result = ytdlp.download("视频URL");
            
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    Toast.makeText(this, "下载成功!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "下载失败", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}
```

## 🎮 更多用法

### 选择视频质量
```java
YtDlpJava ytdlp = new YtDlpJava();
ytdlp.setOption("format", "720p");  // 选择720p
ytdlp.setOption("format", "best");  // 选择最佳质量
YtDlpJava.DownloadResult result = ytdlp.download("视频URL");
```

### 自定义文件名
```java
YtDlpJava ytdlp = new YtDlpJava();
ytdlp.setOption("output", "我的视频_%(title)s.%(ext)s");
YtDlpJava.DownloadResult result = ytdlp.download("视频URL");
```

### 显示下载进度
```java
YtDlpJava ytdlp = new YtDlpJava();
ytdlp.addProgressCallback(new YtDlpJava.ProgressCallback() {
    @Override
    public void onProgress(int percentage, long bytesDownloaded, long totalBytes) {
        System.out.println("下载进度: " + percentage + "%");
    }
    
    @Override
    public void onComplete(String filePath) {
        System.out.println("下载完成: " + filePath);
    }
    
    @Override
    public void onError(String error) {
        System.out.println("下载失败: " + error);
    }
});
YtDlpJava.DownloadResult result = ytdlp.download("视频URL");
```
## 💡 实用技巧

### 批量下载
```java
String[] urls = {"URL1", "URL2", "URL3"};
for (String url : urls) {
    YtDlpJava ytdlp = new YtDlpJava();
    YtDlpJava.DownloadResult result = ytdlp.download(url);
    System.out.println(url + ": " + (result.isSuccess() ? "成功" : "失败"));
}
```

### 只获取视频信息（不下载）
```java
YtDlpJava ytdlp = new YtDlpJava();
VideoInfo info = ytdlp.extractInfo("视频URL");
System.out.println("标题: " + info.getTitle());
System.out.println("时长: " + info.getDuration() + "秒");
```

### 列出所有可用格式
```java
YtDlpJava ytdlp = new YtDlpJava();
List<VideoFormat> formats = ytdlp.listFormats("视频URL");
for (VideoFormat format : formats) {
    System.out.println(format.getFormatId() + " - " + format.getQuality() + "p");
}
```

## 🛠️ 命令行使用

```bash
# 下载视频
java -cp . com.ytdlp.YtDlpMain https://www.facebook.com/watch/?v=123456789

# 选择质量
java -cp . com.ytdlp.YtDlpMain -f 720p https://www.instagram.com/p/ABC123/

# 自定义文件名
java -cp . com.ytdlp.YtDlpMain -o "我的视频_%(title)s.%(ext)s" https://www.tiktok.com/@user/video/123456789

# 列出格式
java -cp . com.ytdlp.YtDlpMain --list-formats https://www.pornhub.com/view_video.php?viewkey=123456789
```

## ❓ 常见问题

**Q: 支持哪些视频网站？**  
A: Facebook, Instagram, TikTok, Pornhub, XHamster, XNXX, XVideos, Twitter, Dailymotion, Vimeo

**Q: Android需要什么权限？**  
A: 只需要网络权限和存储权限

**Q: 下载的视频保存在哪里？**  
A: 默认保存在当前目录，可以通过 `setOption("output", "路径")` 自定义

**Q: 如何选择视频质量？**  
A: 使用 `setOption("format", "720p")` 或 `setOption("format", "best")`

**Q: 下载失败怎么办？**  
A: 检查网络连接，确认视频URL有效，某些视频可能需要登录

## 🎉 就这么简单！

3行代码就能下载10个平台的视频，无需复杂配置，直接复制粘贴就能用！

---

## 📞 需要帮助？

- 查看 `USAGE_GUIDE.md` 获取详细说明
- 查看 `YtDlpJavaExample.java` 获取完整示例
- 查看 `AndroidYtDlpExample.java` 获取Android集成示例

**就这么简单！开始使用吧！** 🚀
