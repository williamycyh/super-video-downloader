# Android集成 - 超简单！

## 🎯 3步搞定Android集成

### 1. 复制源码
把 `javacode/src/main/java/com/ytdlp/` 整个文件夹复制到你的Android项目的 `src/main/java/` 目录下

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
        
        // 下载视频 - 就这么简单！
        new Thread(() -> {
            YtDlpJava ytdlp = new YtDlpJava();
            YtDlpJava.DownloadResult result = ytdlp.download("你的视频URL");
            
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

## 🎮 更多功能

### 显示下载进度
```java
YtDlpJava ytdlp = new YtDlpJava();
ytdlp.addProgressCallback(new YtDlpJava.ProgressCallback() {
    @Override
    public void onProgress(int percentage, long bytesDownloaded, long totalBytes) {
        runOnUiThread(() -> {
            progressBar.setProgress(percentage);
            statusText.setText("下载进度: " + percentage + "%");
        });
    }
    
    @Override
    public void onComplete(String filePath) {
        runOnUiThread(() -> {
            Toast.makeText(this, "下载完成: " + filePath, Toast.LENGTH_SHORT).show();
        });
    }
    
    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "下载失败: " + error, Toast.LENGTH_SHORT).show();
        });
    }
});
YtDlpJava.DownloadResult result = ytdlp.download("视频URL");
```

### 选择视频质量
```java
YtDlpJava ytdlp = new YtDlpJava();
ytdlp.setOption("format", "720p");  // 选择720p
ytdlp.setOption("output", "/storage/emulated/0/Download/%(title)s.%(ext)s");
YtDlpJava.DownloadResult result = ytdlp.download("视频URL");
```

### 批量下载
```java
String[] urls = {"URL1", "URL2", "URL3"};
for (String url : urls) {
    new Thread(() -> {
        YtDlpJava ytdlp = new YtDlpJava();
        YtDlpJava.DownloadResult result = ytdlp.download(url);
        // 处理结果...
    }).start();
}
```

### 获取视频信息（不下载）
```java
YtDlpJava ytdlp = new YtDlpJava();
VideoInfo info = ytdlp.extractInfo("视频URL");
System.out.println("标题: " + info.getTitle());
System.out.println("时长: " + info.getDuration() + "秒");
```

## 🎉 就这么简单！

**3步就能在Android中集成视频下载功能！**

1. 复制源码 ✅
2. 添加权限 ✅  
3. 开始使用 ✅

支持10个主流视频平台，无需复杂配置！

---

## 📞 需要更多帮助？

- 查看 `API_DOCUMENTATION.md` 获取完整API说明
- 查看 `YtDlpJavaExample.java` 获取更多示例
- 查看 `USAGE_GUIDE.md` 获取详细使用指南

**开始使用吧！** 🚀