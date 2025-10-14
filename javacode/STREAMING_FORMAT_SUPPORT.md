# 流媒体格式支持扩展

## 🎯 概述

参考Android工程中的M3U8判断逻辑，我们扩展了Java版本的流媒体格式支持，现在可以使用FFmpeg下载多种流媒体格式。

## 📋 支持的流媒体格式

### 1. **协议类型**
```java
// 支持的协议
"hls"        // HTTP Live Streaming
"m3u8"       // M3U8播放列表
"dash"       // Dynamic Adaptive Streaming
"mpd"        // Media Presentation Description
"mms"        // Microsoft Media Server
"rtmp"       // Real-Time Messaging Protocol
"rtsp"       // Real-Time Streaming Protocol
"websocket"  // WebSocket流
```

### 2. **文件扩展名**
```java
// 支持的扩展名
".m3u8"      // HLS播放列表
".mpd"       // DASH清单文件
".txt"       // 文本清单文件
```

### 3. **URL关键词**
```java
// 支持的URL关键词
"manifest"   // 清单文件
"playlist"   // 播放列表
"stream"     // 流媒体
"live"       // 直播流
```

## 🔧 实现细节

### 1. **流媒体格式检测**

#### Android工程逻辑（参考）
```kotlin
val isM3u8
    get() = originalUrl.contains(".m3u8") || 
            originalUrl.contains(".mpd") || 
            formats.formats.any { url ->
                url.url.toString().contains(".m3u8") || 
                url.url.toString().contains(".mpd") || 
                originalUrl.contains(".txt")
            }
```

#### Java版本实现
```java
private boolean isStreamingFormat(VideoFormat format) {
    String protocol = format.getProtocol();
    String url = format.getUrl();
    
    // 检查协议
    if ("hls".equals(protocol) || "m3u8".equals(protocol) || 
        "dash".equals(protocol) || "mpd".equals(protocol) ||
        "mms".equals(protocol) || "rtmp".equals(protocol) ||
        "rtsp".equals(protocol) || "websocket".equals(protocol)) {
        return true;
    }
    
    // 检查URL中的流媒体标识
    if (url != null) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains(".m3u8") || 
            lowerUrl.contains(".mpd") || 
            lowerUrl.contains(".txt") ||
            lowerUrl.contains("manifest") ||
            lowerUrl.contains("playlist") ||
            lowerUrl.contains("stream") ||
            lowerUrl.contains("live")) {
            return true;
        }
    }
    
    return false;
}
```

### 2. **FFmpeg命令构建**

#### 根据流媒体类型设置不同参数
```java
private List<String> buildFfmpegCommand(String streamUrl, String outputPath) {
    List<String> command = new ArrayList<>();
    
    // FFmpeg可执行文件
    if (!isAndroidEnvironment()) {
        command.add("ffmpeg");
    }
    
    command.add("-y");  // 覆盖输出文件
    
    // 根据流媒体类型设置参数
    String streamType = detectStreamType(streamUrl);
    
    switch (streamType.toLowerCase()) {
        case "hls":
        case "m3u8":
            // HLS流参数
            command.add("-protocol_whitelist");
            command.add("file,http,https,tcp,tls");
            break;
            
        case "dash":
        case "mpd":
            // DASH流参数
            command.add("-f");
            command.add("dash");
            break;
            
        case "rtmp":
            // RTMP流参数
            command.add("-f");
            command.add("flv");
            break;
            
        case "rtsp":
            // RTSP流参数
            command.add("-rtsp_transport");
            command.add("tcp");
            break;
            
        case "mms":
            // MMS流参数
            command.add("-f");
            command.add("asf");
            break;
    }
    
    // 通用参数
    command.add("-i");
    command.add(streamUrl);
    command.add("-c");
    command.add("copy");
    command.add("-f");
    command.add("mp4");
    command.add("-bsf:a");
    command.add("aac_adtstoasc");
    command.add("-bsf:v");
    command.add("h264_mp4toannexb");
    command.add(outputPath);
    
    return command;
}
```

### 3. **流媒体类型检测**
```java
private String detectStreamType(String url) {
    if (url == null) return "unknown";
    
    String lowerUrl = url.toLowerCase();
    
    if (lowerUrl.contains(".m3u8") || lowerUrl.contains("hls")) {
        return "hls";
    } else if (lowerUrl.contains(".mpd") || lowerUrl.contains("dash")) {
        return "dash";
    } else if (lowerUrl.contains("rtmp://")) {
        return "rtmp";
    } else if (lowerUrl.contains("rtsp://")) {
        return "rtsp";
    } else if (lowerUrl.contains("mms://")) {
        return "mms";
    } else if (lowerUrl.contains(".txt") || lowerUrl.contains("manifest")) {
        return "manifest";
    } else {
        return "auto";
    }
}
```

## 📱 Android使用示例

### 1. **基本使用**
```java
YtDlpJava ytdlpJava = new YtDlpJava();
VideoInfo videoInfo = ytdlpJava.extractInfo(url);

for (VideoFormat format : videoInfo.getFormats()) {
    // 自动检测流媒体格式
    DownloadResult result = ytdlpJava.downloadFormat(format, outputPath);
    
    if (result.isSuccess()) {
        Log.i("Download", "下载成功: " + result.getFilePath());
    }
}
```

### 2. **手动指定格式**
```java
// 查找流媒体格式
VideoFormat streamingFormat = null;
for (VideoFormat format : videoInfo.getFormats()) {
    if (format.getUrl().contains(".m3u8") || 
        format.getUrl().contains(".mpd")) {
        streamingFormat = format;
        break;
    }
}

if (streamingFormat != null) {
    // 使用FFmpeg下载流媒体
    DownloadResult result = ytdlpJava.downloadFormat(streamingFormat, outputPath);
}
```

## 🧪 测试结果

### 测试用例
```java
// 测试不同的流媒体格式
String[] testUrls = {
    "https://example.com/video.m3u8",     // ✅ 流媒体
    "https://example.com/video.mpd",      // ✅ 流媒体
    "https://example.com/manifest.txt",   // ✅ 流媒体
    "https://example.com/playlist.m3u8",  // ✅ 流媒体
    "https://example.com/stream.mpd",     // ✅ 流媒体
    "https://example.com/live.m3u8",      // ✅ 流媒体
    "https://example.com/video.mp4",      // ❌ 普通文件
    "https://example.com/video.avi"       // ❌ 普通文件
};
```

### 实际测试结果
```
🎯 测试URL: https://www.dailymotion.com/video/x9s1tzq
🔍 分析格式: hls-auto
   协议: hls
   扩展: mp4
   URL: https://www.dailymotion.com/cdn/manifest/video/x9s1tzq.m3u8?sec=...
   流媒体格式: ✅ 是
   📱 将使用FFmpeg下载器
```

## 🎯 优势对比

### 扩展前
```java
// 只支持基本的HLS格式
if ("hls".equals(protocol) || "m3u8".equals(protocol) || format.getUrl().contains("m3u8")) {
    // 使用FFmpeg
}
```

### 扩展后
```java
// 支持多种流媒体格式
if (isStreamingFormat(format)) {
    // 使用FFmpeg，自动检测流媒体类型并设置相应参数
}
```

## 🚀 支持的完整格式列表

```java
public List<String> getSupportedFormats() {
    List<String> formats = new ArrayList<>();
    
    // HLS格式
    formats.add("hls");
    formats.add("m3u8");
    
    // DASH格式
    formats.add("dash");
    formats.add("mpd");
    
    // 流媒体协议
    formats.add("rtmp");
    formats.add("rtsp");
    formats.add("mms");
    
    // 清单文件
    formats.add("manifest");
    formats.add("playlist");
    
    // 文本格式
    formats.add("txt");
    
    return formats;
}
```

## 📝 总结

### ✅ 新增功能
1. **扩展流媒体格式检测**: 支持M3U8、MPD、TXT等多种格式
2. **智能FFmpeg参数**: 根据流媒体类型自动设置FFmpeg参数
3. **协议支持**: 支持HLS、DASH、RTMP、RTSP、MMS等协议
4. **Android兼容**: 完全兼容Android工程的判断逻辑

### 🎯 使用建议
1. **自动检测**: 系统会自动检测流媒体格式并使用FFmpeg
2. **回退机制**: 如果FFmpeg不可用，自动回退到纯Java方案
3. **Android优化**: 在Android环境中使用FFmpegKit获得最佳性能

**现在Java版本完全支持Android工程中的流媒体格式判断逻辑！** 🚀
