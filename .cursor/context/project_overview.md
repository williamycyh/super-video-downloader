# Super Video Downloader - 项目概览

## 项目简介
这是一个Android视频下载应用，支持从多个视频平台下载视频内容。

## 技术架构变更

### 原始架构
- **视频下载库**: `io.github.junkfood02.Bubedl-android:library`
- **支持平台**: 基于Python yt-dlp的Android封装库

### 当前架构
- **视频下载库**: 自定义的 `bt-dlp-java` 工程 (重命名自 yt-dlp-java)
- **源码基础**: 基于Python `yt_dlp` 源码改造
- **源码位置**: `/Users/egbert/Desktop/myproject/yt-dlp/javacode`
- **包名**: `com.btdlp` (重命名自 com.ytdlp)

## 项目结构

### 主要目录
```
super-video-downloader/
├── app/                                    # Android应用主模块
│   ├── src/main/java/com/example/         # 应用源代码
│   └── build.gradle                       # 应用构建配置
├── yt-dlp-java/                           # 自定义视频下载库 (Gradle模块名)
│   └── src/main/java/com/btdlp/          # Java版本的Bube下载实现 (重命名)
└── yt-dlp/                                # Python源码参考
    └── yt_dlp/                            # Python版本的yt-dlp源码
```

### 关键文件
- `app/build.gradle` - 应用构建配置，包含yt-dlp-java依赖
- `settings.gradle` - 模块配置，定义yt-dlp-java模块路径
- `yt-dlp-java/src/main/java/com/btdlp/` - Java版本的Bube视频提取器实现 (重命名)

## 支持的视频平台

### 已实现的提取器
- **Dailymotion** - `AdvancedDailymotionExtractor.java`
- **Facebook** - `FinalFacebookExtractor.java`
- **Instagram** - `InstagramExtractor.java`
- **TikTok** - `AdvancedTikTokExtractor.java`
- **Twitter** - `AdvancedTwitterExtractor.java`
- **Vimeo** - `AdvancedVimeoExtractor.java`
- **Bube** - 内置Bube提取器
- **其他成人平台** - Pornhub, XHamster, XVideos, XNXX

## 核心功能模块

### 1. 视频信息提取
- **主类**: `BtdJava.java` (重命名自 YtDlpJava.java)
- **核心引擎**: `BubeDL.java` (重命名自 BubeDL.java)
- **请求处理**: `BubeDLRequest.java` (重命名自 BubeDLRequest.java)
- **响应处理**: `BubeDLResponse.java` (重命名自 BubeDLResponse.java)
- **提取器注册**: `ExtractorRegistry.java`
- **视频信息**: `VideoInfo.java`
- **视频格式**: `VideoFormat.java`

### 2. 下载管理
- **下载器**: `BubeDlDownloaderWorker.java`
- **进度管理**: 支持实时下载进度显示
- **多线程下载**: 支持并发下载
- **FFmpeg支持**: 通过 `AndroidFfmpegHlsDownloader.java` 支持HLS流下载

### 3. UI界面
- **主界面**: `MainActivity.kt`
- **浏览器**: `WebTabFragment.kt`
- **下载弹窗**: `DetectedVideosTabFragment.kt`
- **下载列表**: `ProgressFragment.kt`

## 重要修复记录

### 1. Dailymotion支持修复
- **问题**: Dailymotion视频无法解析和下载
- **原因**: Java版本的Dailymotion提取器没有实现真实的API调用
- **修复**: 按照Python版本逻辑重写了`AdvancedDailymotionExtractor.java`
- **关键改进**:
  - 实现了真实的HTTP请求到Dailymotion元数据API
  - 添加了完整的JSON解析逻辑
  - 支持多种视频质量格式提取

### 2. 布局文件修复
- **问题**: 提交cbbadfb17301067e66ad9a9bb5e15771766008fc导致布局元素位置错乱
- **影响**: 下载弹窗无法正常显示
- **状态**: 待修复，需要恢复所有布局文件的元素位置

## 构建配置

### Gradle依赖
```gradle
// 自定义Bube下载Java库 (重命名自yt-dlp-java)
implementation project(':yt-dlp-java')
// FFmpeg支持
implementation 'com.arthenica:ffmpeg-kit-full:6.0-2'
```

### 模块路径配置
```gradle
// settings.gradle
include ':yt-dlp-java'
project(':yt-dlp-java').projectDir = new File('/Users/egbert/Desktop/myproject/yt-dlp/javacode')
```

## 开发注意事项

### 1. 视频提取器开发
- 参考Python版本的`yt_dlp/extractor/`目录下的实现
- 实现`InfoExtractor`接口
- 在`ExtractorRegistry.java`中注册新的提取器

### 2. API调用
- 使用`HttpURLConnection`进行网络请求
- 设置正确的请求头模拟浏览器
- 处理HTTP错误和超时

### 3. 日志调试
- 使用`Logger`类记录调试信息
- 关键日志标签: `Bube_DL_DEBUG_TAG`
- 可通过`adb logcat`查看实时日志

## 测试方法

### 1. 构建测试
```bash
./gradlew :app:assembleDebug --no-daemon
```

### 2. 安装测试
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. 日志监控
```bash
adb logcat | grep -E "(VideoService|VideoDetectionTabViewModel|AdvancedDailymotionExtractor)"
```

## 重命名记录

### 2024年10月重命名
- **包名**: `com.ytdlp` → `com.btdlp`
- **主类**: `YtDlpJava` → `BtdJava`
- **核心类**: `BubeDL` → `BubeDL`
- **请求类**: `BubeDLRequest` → `BubeDLRequest`
- **响应类**: `BubeDLResponse` → `BubeDLResponse`
- **选项类**: `BubeDLOptions` → `BubeDLOptions`
- **主入口**: `YtDlpMain` → `BtdMain`
- **示例类**: `YtDlpJavaExample` → `BtdJavaExample`

### 重命名原因
- 避免与Bube品牌冲突
- 使用更通用的"Bube"品牌名
- 保持功能完整性的同时重新品牌化

## 待解决问题

1. **布局文件修复** - 恢复提交cbbadfb导致的布局元素位置错乱
2. **其他平台测试** - 验证其他视频平台的下载功能
3. **错误处理优化** - 改进网络错误和解析错误的处理
4. **性能优化** - 优化大文件下载和内存使用
5. **Android应用引用更新** - 更新Android应用中对重命名类的引用

## 相关文档
- Python yt-dlp官方文档: https://github.com/yt-dlp/yt-dlp
- Android开发文档: https://developer.android.com/
- 项目构建日志: `build/reports/problems/problems-report.html`
