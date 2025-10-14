# Android FFmpeg下载失败修复方案

## 🔍 问题分析

从Android日志中可以看到以下问题：

### 1. **FFmpegKit执行失败**
```
FFmpegKit执行失败，返回码: null
FFmpegKit错误输出: null
```

### 2. **输出路径问题**
```
输出路径: /M3U8_Stream.ts
```
这不是正确的Android文件路径。

## 🔧 修复方案

### 1. **修复FFmpegKit异步执行问题**

**问题**: 使用`FFmpegKit.executeAsync()`但没有等待执行完成
**解决方案**: 改用同步执行`FFmpegKit.execute()`

```java
// 修复前（异步，有问题）
Session session = FFmpegKit.executeAsync(String.join(" ", command), callback1, null, callback2);
ReturnCode returnCode = session.getReturnCode(); // 立即获取，可能为null

// 修复后（同步）
Session session = FFmpegKit.execute(String.join(" ", command));
ReturnCode returnCode = session.getReturnCode(); // 等待完成后获取
```

### 2. **修复输出路径生成**

**问题**: HLS格式生成`.ts`扩展名，但FFmpeg输出MP4
**解决方案**: HLS格式使用`.mp4`扩展名

```java
// 修复前
if ("hls".equals(format.getProtocol()) || "m3u8".equals(format.getProtocol())) {
    ext = "ts";  // 错误：FFmpeg输出MP4
}

// 修复后
if ("hls".equals(format.getProtocol()) || "m3u8".equals(format.getProtocol())) {
    ext = "mp4";  // 正确：FFmpeg转换为MP4
}
```

### 3. **增强错误日志**

```java
if (success) {
    logger.info("FFmpegKit执行成功");
} else {
    logger.error("FFmpegKit执行失败，返回码: %s", returnCode);
    logger.error("FFmpegKit错误输出: %s", session.getFailStackTrace());
    logger.error("FFmpegKit输出: %s", session.getOutput());  // 新增
}
```

### 4. **文件验证**

```java
if (success) {
    // 验证输出文件
    File outputFile = new File(outputPath);
    if (outputFile.exists() && outputFile.length() > 0) {
        logger.info("FFmpegKit下载完成: %s (大小: %d bytes)", outputPath, outputFile.length());
        return true;
    } else {
        logger.error("FFmpegKit下载完成但文件不存在或为空: %s", outputPath);
        return false;
    }
}
```

## 📱 Android环境中的完整修复

### 1. **更新AndroidFfmpegHlsDownloader.java**

```java
// 使用同步执行
Session session = FFmpegKit.execute(String.join(" ", command));

// 等待执行完成
ReturnCode returnCode = session.getReturnCode();
boolean success = ReturnCode.isSuccess(returnCode);

// 增强错误日志
if (!success) {
    logger.error("FFmpegKit执行失败，返回码: %s", returnCode);
    logger.error("FFmpegKit错误输出: %s", session.getFailStackTrace());
    logger.error("FFmpegKit输出: %s", session.getOutput());
}

// 验证输出文件
if (success) {
    File outputFile = new File(outputPath);
    if (outputFile.exists() && outputFile.length() > 0) {
        logger.info("FFmpegKit下载完成: %s (大小: %d bytes)", outputPath, outputFile.length());
        return true;
    } else {
        logger.error("FFmpegKit下载完成但文件不存在或为空: %s", outputPath);
        return false;
    }
}
```

### 2. **更新YtDlpJava.java**

```java
// HLS格式使用MP4扩展名
if ("hls".equals(format.getProtocol()) || "m3u8".equals(format.getProtocol())) {
    ext = "mp4";  // FFmpeg会转换为MP4格式
}
```

## 🧪 测试验证

### 预期日志输出
```
使用FFmpegKit执行命令...
FFmpegKit命令: -y -protocol_whitelist file,http,https,tcp,tls -i [URL] -c copy -f mp4 -bsf:a aac_adtstoasc -bsf:v h264_mp4toannexb [OUTPUT_PATH]
FFmpegKit执行成功
FFmpegKit下载完成: /storage/emulated/0/Android/data/.../video.mp4 (大小: 1234567 bytes)
```

### 预期结果
- ✅ FFmpegKit执行成功
- ✅ 生成正确的MP4文件
- ✅ 文件大小大于0
- ✅ 文件可播放

## 🎯 关键修复点

1. **同步执行**: 使用`FFmpegKit.execute()`而不是`executeAsync()`
2. **正确扩展名**: HLS格式输出MP4而不是TS
3. **增强日志**: 添加详细的错误信息
4. **文件验证**: 确保输出文件存在且有效

## 📝 总结

主要问题是FFmpegKit的异步执行导致返回码为null。修复后应该能够正常下载Dailymotion的HLS视频并转换为MP4格式。

**修复后的流程**:
1. 检测到HLS格式 → 使用FFmpeg下载器
2. 同步执行FFmpeg命令 → 等待完成
3. 验证返回码 → 检查输出文件
4. 返回下载结果 → 生成可播放的MP4文件
