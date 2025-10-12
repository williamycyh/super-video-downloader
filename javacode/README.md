# yt-dlp Java版本

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-8+-blue.svg)](https://www.oracle.com/java/)
[![Android](https://img.shields.io/badge/Android-5.0+-green.svg)](https://developer.android.com/)

一个纯Java实现的视频下载库，基于Python版本的yt-dlp，支持10个主流视频平台。专为Android应用设计，无外部依赖，提供完整的视频信息提取和下载功能。

## 🌟 特性

- ✅ **纯Java实现** - 无外部依赖，完美适配Android
- ✅ **10个平台支持** - Facebook, Instagram, TikTok, Pornhub, XHamster, XNXX, XVideos, Twitter, Dailymotion, Vimeo
- ✅ **OAuth认证** - 支持Dailymotion等平台的认证机制
- ✅ **HLS支持** - 完整的HTTP Live Streaming协议支持
- ✅ **多格式支持** - MP4, HLS, DASH等多种视频格式
- ✅ **Android优化** - 专为Android应用优化的API设计

## 🚀 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>com.ytdlp</groupId>
    <artifactId>yt-dlp-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle依赖

```gradle
implementation 'com.ytdlp:yt-dlp-java:1.0.0'
```

### 基本使用

```java
import com.ytdlp.core.VideoInfo;
import com.ytdlp.extractor.facebook.AdvancedFacebookExtractor;
import com.ytdlp.downloader.FileDownloader;

// 创建提取器
AdvancedFacebookExtractor extractor = new AdvancedFacebookExtractor();

// 提取视频信息
VideoInfo videoInfo = extractor.extract("https://www.facebook.com/watch/?v=123456789");

// 下载视频
FileDownloader downloader = new FileDownloader();
VideoFormat bestFormat = selectBestFormat(videoInfo.getFormats());
boolean success = downloader.download(bestFormat.getUrl(), "output.mp4");
```

## 📱 Android集成

### 1. 添加权限

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 2. 异步下载

```java
public class VideoDownloadTask extends AsyncTask<String, Void, Boolean> {
    @Override
    protected Boolean doInBackground(String... urls) {
        try {
            String videoUrl = urls[0];
            InfoExtractor extractor = createExtractor(videoUrl);
            VideoInfo videoInfo = extractor.extract(videoUrl);
            
            VideoFormat bestFormat = selectBestFormat(videoInfo.getFormats());
            FileDownloader downloader = new FileDownloader();
            return downloader.download(bestFormat.getUrl(), outputPath);
        } catch (Exception e) {
            return false;
        }
    }
}
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

## 📚 API文档

### 核心类

- **VideoInfo** - 视频信息容器
- **VideoFormat** - 视频格式信息
- **InfoExtractor** - 视频信息提取器基类
- **FileDownloader** - 文件下载器
- **HLSDownloader** - HLS视频下载器

### 平台提取器

- **AdvancedFacebookExtractor** - Facebook视频提取
- **AdvancedInstagramExtractor** - Instagram视频提取
- **AdvancedTikTokExtractor** - TikTok视频提取
- **AdvancedPornhubExtractor** - Pornhub视频提取
- **AdvancedXHamsterExtractor** - XHamster视频提取
- **AdvancedXNXXExtractor** - XNXX视频提取
- **AdvancedXVideosExtractor** - XVideos视频提取
- **AdvancedTwitterExtractor** - Twitter视频提取
- **AdvancedDailymotionExtractor** - Dailymotion视频提取
- **AdvancedVimeoExtractor** - Vimeo视频提取

## 🔧 高级功能

### OAuth认证

```java
import com.ytdlp.utils.DailymotionAuth;

DailymotionAuth auth = new DailymotionAuth();
String token = auth.getAccessToken();
Map<String, String> headers = auth.getAuthHeaders();
```

### HLS下载

```java
import com.ytdlp.downloader.HLSDownloader;

boolean success = HLSDownloader.downloadHLS(hlsUrl, "output.mp4");
```

### 进度回调

```java
FileDownloader downloader = new FileDownloader();
downloader.download(url, outputPath, new ProgressCallback() {
    @Override
    public void onProgress(int percentage) {
        // 更新进度条
    }
    
    @Override
    public void onComplete(String filePath) {
        // 下载完成
    }
    
    @Override
    public void onError(String error) {
        // 下载失败
    }
});
```

## 🏗️ 构建

### 使用Maven

```bash
mvn clean package
```

### 使用Gradle

```bash
./gradlew build
```

### 创建Fat JAR

```bash
mvn clean package -P fat-jar
```

## 🧪 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=FacebookExtractorTest
```

## 📖 文档

- [完整API文档](API_DOCUMENTATION.md)
- [Android集成示例](ANDROID_INTEGRATION_EXAMPLE.md)
- [项目概述](.cursor/context/project_overview.md)

## 🤝 贡献

欢迎提交Issue和Pull Request！

1. Fork项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开Pull Request

## 📄 许可证

本项目基于MIT许可证开源 - 查看 [LICENSE](LICENSE) 文件了解详情。

## ⚠️ 免责声明

- 请遵守各视频平台的使用条款和法律法规
- 仅用于个人学习和研究目的
- 不得用于商业用途或侵犯版权
- 使用时请确保有合法的下载权限

## 📞 支持

如有问题，请查看：
- [GitHub Issues](https://github.com/your-repo/issues)
- [Wiki文档](https://github.com/your-repo/wiki)
- [示例代码](https://github.com/your-repo/examples)

## 🙏 致谢

- 感谢 [yt-dlp](https://github.com/yt-dlp/yt-dlp) 项目提供的灵感和参考
- 感谢所有贡献者的支持

---

**注意**: 这是一个教育项目，请合理使用，遵守相关法律法规。