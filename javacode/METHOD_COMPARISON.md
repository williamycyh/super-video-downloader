# execute() vs download() 方法对比分析

## 🎯 方法概述

### `execute()` 方法
```java
public BubeDLResponse execute(BubeDLRequest request, String processId, ProgressCallback callback)
```

### `download()` 方法
```java
public DownloadResult download(String url, String outputPath)
```

## 📋 详细对比

### 1. **设计目的**

#### `execute()` 方法
- **目的**: 兼容Python版本的`dl-android`库
- **设计**: 模仿Python yt-dlp的完整工作流程
- **输入**: `BubeDLRequest`对象（包含完整的命令参数）
- **输出**: `BubeDLResponse`对象（包含完整的执行结果）

#### `download()` 方法
- **目的**: 简化的Java原生下载接口
- **设计**: 直接、简单的下载功能
- **输入**: URL字符串 + 输出路径
- **输出**: `DownloadResult`对象（简化的结果）

### 2. **参数对比**

#### `execute()` 参数
```java
// 复杂的参数结构
BubeDLRequest request    // 包含所有选项和URL
String processId           // 进程ID
ProgressCallback callback  // 进度回调
```

#### `download()` 参数
```java
// 简单的参数结构
String url         // 视频URL
String outputPath  // 输出路径
```

### 3. **功能对比**

#### `execute()` 功能
```java
// 1. 解析复杂的请求对象
List<String> command = request.buildCommand();

// 2. 处理HTTP头部
if (options.getHttpHeaders() != null) {
    Map<String, String> headers = parseHttpHeaders(options.getHttpHeaders());
    setHttpHeaders(headers);
}

// 3. 根据选项决定行为
boolean isInfoRequest = request.hasOption("--dump-json");

// 4. 支持格式选择
if (options.getFormat() != null) {
    VideoFormat selectedFormat = selectFormatBySpec(videoInfo.getFormats(), options.getFormat());
    DownloadResult result = downloadFormat(selectedFormat, outputPath);
}

// 5. 返回详细的响应对象
return new BubeDLResponse(command, exitCode, elapsedTime, output, error);
```

#### `download()` 功能
```java
// 1. 自动选择提取器
InfoExtractor extractor = createExtractor(url);

// 2. 提取视频信息
VideoInfo videoInfo = extractor.extract(url);

// 3. 自动格式选择
List<VideoFormat> selectedFormats = selectFormats(videoInfo.getFormats());

// 4. 自动生成文件名
if (outputPath == null) {
    outputPath = generateOutputPath(videoInfo, selectedFormats.get(0));
}

// 5. 下载视频
boolean success = downloadFormats(selectedFormats, outputPath);

// 6. 返回简化的结果对象
return new DownloadResult(success, outputPath, error, videoInfo);
```

### 4. **返回值对比**

#### `execute()` 返回值
```java
public class BubeDLResponse {
    private List<String> command;     // 执行的命令
    private int exitCode;             // 退出码
    private long elapsedTime;         // 执行时间
    private String output;            // 输出内容
    private String error;             // 错误信息
}
```

#### `download()` 返回值
```java
public class DownloadResult {
    private boolean success;          // 是否成功
    private String filePath;          // 文件路径
    private String errorMessage;      // 错误信息
    private VideoInfo videoInfo;      // 视频信息
}
```

### 5. **使用场景对比**

#### `execute()` 使用场景
```java
// 复杂场景：需要精确控制所有参数
BubeDLRequest request = new BubeDLRequest();
request.setUrls(Arrays.asList(url));
request.setOption("--format", "best[height<=720]");
request.setOption("--output", "/path/to/output.%(ext)s");
request.setOption("--http-headers", "User-Agent: MyApp/1.0");

BubeDLResponse response = ytdlpJava.execute(request, "process1", callback);

if (response.getExitCode() == 0) {
    System.out.println("成功: " + response.getOutput());
} else {
    System.err.println("失败: " + response.getError());
}
```

#### `download()` 使用场景
```java
// 简单场景：快速下载
DownloadResult result = ytdlpJava.download(url, outputPath);

if (result.isSuccess()) {
    System.out.println("下载成功: " + result.getFilePath());
} else {
    System.err.println("下载失败: " + result.getErrorMessage());
}
```

### 6. **内部实现对比**

#### `execute()` 内部实现
```java
// 1. 解析请求对象
List<String> command = request.buildCommand();
DownloadOptions options = request.getOptions();

// 2. 应用配置
if (options.getHttpHeaders() != null) {
    Map<String, String> headers = parseHttpHeaders(options.getHttpHeaders());
    setHttpHeaders(headers);
}

// 3. 根据选项执行不同操作
if (isInfoRequest) {
    // 仅提取信息
    VideoInfo videoInfo = extractInfo(url);
    String jsonOutput = convertVideoInfoToJson(videoInfo);
    return new BubeDLResponse(command, 0, elapsedTime, jsonOutput, "");
} else {
    // 下载视频
    if (options.getFormat() != null) {
        // 指定格式下载
        DownloadResult result = downloadFormat(selectedFormat, outputPath);
    } else {
        // 自动格式下载
        DownloadResult result = download(url, outputPath);
    }
}
```

#### `download()` 内部实现
```java
// 1. 自动选择提取器
InfoExtractor extractor = createExtractor(url);

// 2. 提取视频信息
VideoInfo videoInfo = extractor.extract(url);

// 3. 自动格式选择
List<VideoFormat> selectedFormats = selectFormats(videoInfo.getFormats());

// 4. 自动生成输出路径
if (outputPath == null) {
    outputPath = generateOutputPath(videoInfo, selectedFormats.get(0));
}

// 5. 下载视频
boolean success = downloadFormats(selectedFormats, outputPath);
```

### 7. **性能对比**

#### `execute()` 性能特点
- **启动开销**: 较高（需要解析复杂请求对象）
- **内存使用**: 较高（保存完整的命令和响应信息）
- **灵活性**: 极高（支持所有Python yt-dlp选项）
- **易用性**: 较低（需要了解复杂的参数结构）

#### `download()` 性能特点
- **启动开销**: 较低（直接处理URL和路径）
- **内存使用**: 较低（只保存必要信息）
- **灵活性**: 中等（支持基本的下载需求）
- **易用性**: 极高（简单的参数接口）

## 🎯 使用建议

### 选择 `execute()` 的情况
1. **需要与Python yt-dlp完全兼容**
2. **需要精确控制下载参数**
3. **需要自定义格式选择**
4. **需要处理复杂的HTTP头部**
5. **需要获取详细的执行信息**

### 选择 `download()` 的情况
1. **快速原型开发**
2. **简单的下载需求**
3. **不需要复杂的参数控制**
4. **优先考虑代码简洁性**
5. **Android应用集成**

## 🔄 方法关系

```java
// execute() 内部会调用 download()
public BubeDLResponse execute(BubeDLRequest request, ...) {
    // ...
    if (options.getFormat() != null) {
        // 使用 downloadFormat()
        DownloadResult result = downloadFormat(selectedFormat, outputPath);
    } else {
        // 调用 download() 方法
        DownloadResult result = download(url, outputPath);
    }
    // ...
}
```

## 📝 总结

| 特性 | `execute()` | `download()` |
|------|-------------|--------------|
| **复杂度** | 高 | 低 |
| **灵活性** | 极高 | 中等 |
| **易用性** | 低 | 高 |
| **性能** | 中等 | 高 |
| **兼容性** | Python完全兼容 | Java原生 |
| **适用场景** | 复杂需求 | 简单需求 |

**建议**: 
- **Android应用**: 优先使用 `download()`
- **复杂需求**: 使用 `execute()`
- **Python兼容**: 使用 `execute()`
