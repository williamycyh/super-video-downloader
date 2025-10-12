# YtDlpJava 使用指南

## 📖 概述

YtDlpJava 是 Python 版本 yt-dlp 的纯 Java 实现，提供了与 Python 版本相似的 API 接口，支持 10 个主流视频平台。

## 🚀 快速开始

### 基本使用

```java
import com.ytdlp.YtDlpJava;

// 创建下载器实例
YtDlpJava ytdlp = new YtDlpJava();

// 下载视频
YtDlpJava.DownloadResult result = ytdlp.download("https://www.facebook.com/watch/?v=123456789");

if (result.isSuccess()) {
    System.out.println("下载成功: " + result.getFilePath());
} else {
    System.out.println("下载失败: " + result.getErrorMessage());
}
```

### 命令行使用

```bash
# 基本下载
java -cp . com.ytdlp.YtDlpMain https://www.facebook.com/watch/?v=123456789

# 指定格式和输出路径
java -cp . com.ytdlp.YtDlpMain -f 720p -o "video_%(title)s.%(ext)s" https://www.instagram.com/p/ABC123/

# 列出可用格式
java -cp . com.ytdlp.YtDlpMain --list-formats https://www.tiktok.com/@user/video/123456789

# 模拟下载（不实际下载文件）
java -cp . com.ytdlp.YtDlpMain --simulate https://www.pornhub.com/view_video.php?viewkey=123456789
```

## 📚 API 详解

### YtDlpJava 类

#### 构造函数

```java
// 使用默认选项
YtDlpJava ytdlp = new YtDlpJava();

// 使用自定义选项
Map<String, String> options = new HashMap<>();
options.put("format", "720p");
options.put("output", "downloads/%(title)s.%(ext)s");
YtDlpJava ytdlp = new YtDlpJava(options);
```

#### 主要方法

##### 1. download(String url)
下载视频到默认位置
```java
YtDlpJava.DownloadResult result = ytdlp.download(url);
```

##### 2. download(String url, String outputPath)
下载视频到指定位置
```java
YtDlpJava.DownloadResult result = ytdlp.download(url, "output.mp4");
```

##### 3. extractInfo(String url)
提取视频信息（不下载）
```java
VideoInfo videoInfo = ytdlp.extractInfo(url);
```

##### 4. listFormats(String url)
列出可用格式
```java
List<VideoFormat> formats = ytdlp.listFormats(url);
```

##### 5. setOption(String key, String value)
设置选项
```java
ytdlp.setOption("format", "best");
ytdlp.setOption("output", "%(title)s.%(ext)s");
```

#### 进度回调

```java
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
        System.err.println("下载失败: " + error);
    }
});
```

### 选项配置

#### 常用选项

| 选项 | 说明 | 默认值 |
|------|------|--------|
| `format` | 视频格式选择 | `best` |
| `output` | 输出文件名模板 | `%(title)s.%(ext)s` |
| `quiet` | 静默模式 | `false` |
| `verbose` | 详细输出 | `true` |

#### 格式选择

```java
ytdlp.setOption("format", "best");     // 最佳质量
ytdlp.setOption("format", "worst");    // 最低质量
ytdlp.setOption("format", "720p");     // 720p质量
ytdlp.setOption("format", "1080p");    // 1080p质量
```

#### 输出模板

```java
ytdlp.setOption("output", "%(title)s.%(ext)s");           // 标题.扩展名
ytdlp.setOption("output", "%(uploader)s_%(title)s.%(ext)s"); // 上传者_标题.扩展名
ytdlp.setOption("output", "video_%(id)s.%(ext)s");        // video_视频ID.扩展名
```

## 🎯 支持的平台

| 平台 | 状态 | 示例URL |
|------|------|---------|
| **Facebook** | ✅ 完全支持 | `https://www.facebook.com/watch/?v=123456789` |
| **Instagram** | ✅ 完全支持 | `https://www.instagram.com/p/ABC123/` |
| **TikTok** | ✅ 完全支持 | `https://www.tiktok.com/@user/video/123456789` |
| **Pornhub** | ✅ 完全支持 | `https://www.pornhub.com/view_video.php?viewkey=123456789` |
| **XHamster** | ✅ 完全支持 | `https://xhamster.com/videos/video-123456789` |
| **XNXX** | ✅ 完全支持 | `https://www.xnxx.com/video-abc123/title` |
| **XVideos** | ✅ 完全支持 | `https://www.xvideos.com/video123456/title` |
| **Twitter** | ✅ 完全支持 | `https://twitter.com/user/status/123456789` |
| **Dailymotion** | ✅ 完全支持 | `https://www.dailymotion.com/video/x123456` |
| **Vimeo** | ⚠️ 部分支持 | `https://vimeo.com/123456789` |

## 📱 Android 集成

### 基本集成

```java
public class MainActivity extends AppCompatActivity {
    private YtDlpJava ytdlp;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化下载器
        ytdlp = new YtDlpJava();
        ytdlp.setOption("output", "/storage/emulated/0/Download/%(title)s.%(ext)s");
        
        // 设置进度回调
        ytdlp.addProgressCallback(new YtDlpJava.ProgressCallback() {
            @Override
            public void onProgress(int percentage, long bytesDownloaded, long totalBytes) {
                runOnUiThread(() -> {
                    // 更新进度条
                    progressBar.setProgress(percentage);
                });
            }
            
            @Override
            public void onComplete(String filePath) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "下载完成", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "下载失败: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void downloadVideo(String url) {
        new Thread(() -> {
            YtDlpJava.DownloadResult result = ytdlp.download(url);
            // 处理结果...
        }).start();
    }
}
```

### 权限配置

在 `AndroidManifest.xml` 中添加：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## 🔧 高级用法

### 批量下载

```java
List<String> urls = Arrays.asList(
    "https://www.facebook.com/watch/?v=123456789",
    "https://www.instagram.com/p/ABC123/",
    "https://www.tiktok.com/@user/video/123456789"
);

YtDlpJava ytdlp = new YtDlpJava();
ytdlp.setOption("ignore-errors", "true"); // 忽略错误继续下载

for (String url : urls) {
    YtDlpJava.DownloadResult result = ytdlp.download(url);
    if (result.isSuccess()) {
        System.out.println("下载成功: " + result.getFilePath());
    } else {
        System.err.println("下载失败: " + result.getErrorMessage());
    }
}
```

### 自定义格式选择

```java
// 先列出格式
List<VideoFormat> formats = ytdlp.listFormats(url);

// 显示格式供用户选择
for (int i = 0; i < formats.size(); i++) {
    VideoFormat format = formats.get(i);
    System.out.printf("%d. %s - %sp - %s%n", 
        i, format.getFormatId(), format.getQuality(), format.getExt());
}

// 用户选择格式后下载
Scanner scanner = new Scanner(System.in);
int choice = scanner.nextInt();
VideoFormat selectedFormat = formats.get(choice);

// 使用选定的格式下载
ytdlp.setOption("format", selectedFormat.getFormatId());
YtDlpJava.DownloadResult result = ytdlp.download(url);
```

### 错误处理

```java
try {
    YtDlpJava.DownloadResult result = ytdlp.download(url);
    
    if (result.isSuccess()) {
        System.out.println("下载成功: " + result.getFilePath());
    } else {
        System.err.println("下载失败: " + result.getErrorMessage());
        
        // 根据错误类型进行不同处理
        if (result.getErrorMessage().contains("不支持的平台")) {
            System.err.println("请检查URL是否正确");
        } else if (result.getErrorMessage().contains("无法提取")) {
            System.err.println("视频可能已被删除或需要登录");
        }
    }
} catch (Exception e) {
    System.err.println("程序异常: " + e.getMessage());
    e.printStackTrace();
}
```

## 🛠️ 故障排除

### 常见问题

1. **"不支持的平台"错误**
   - 检查URL是否以 `http://` 或 `https://` 开头
   - 确认URL来自支持的平台

2. **"无法提取视频信息"错误**
   - 视频可能已被删除或设为私有
   - 某些平台需要登录才能下载
   - 检查网络连接

3. **下载失败**
   - 检查输出路径是否有写入权限
   - 确认磁盘空间充足
   - 检查网络连接稳定性

4. **格式选择问题**
   - 使用 `listFormats()` 查看可用格式
   - 某些格式可能需要特定的下载器（如HLS）

### 调试模式

```java
YtDlpJava ytdlp = new YtDlpJava();
ytdlp.setOption("verbose", "true"); // 启用详细日志
```

## 📄 示例代码

完整的使用示例请参考：
- `YtDlpJavaExample.java` - 基本使用示例
- `AndroidYtDlpExample.java` - Android集成示例
- `YtDlpJavaTest.java` - 功能测试示例

## 🤝 贡献

欢迎提交 Issue 和 Pull Request 来改进这个项目。

## 📄 许可证

本项目基于 MIT 许可证开源。
