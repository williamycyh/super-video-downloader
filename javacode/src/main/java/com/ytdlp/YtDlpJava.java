package com.ytdlp;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.core.YoutubeDL;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.extractor.ExtractorRegistry;
import com.ytdlp.utils.Logger;
import com.ytdlp.options.DownloadOptions;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Java版本的yt-dlp主入口类
 * 参考Python版本的设计，提供统一的视频下载接口
 */
public class YtDlpJava {
    
    private Logger logger;
    private Map<String, String> options;
    private List<ProgressCallback> progressCallbacks;
    private ExtractorRegistry extractorRegistry;
    
    /**
     * 进度回调接口
     */
    public interface ProgressCallback {
        void onProgress(int percentage, long bytesDownloaded, long totalBytes);
        void onComplete(String filePath);
        void onError(String error);
    }
    
    /**
     * 下载结果类
     */
    public static class DownloadResult {
        private boolean success;
        private String filePath;
        private String errorMessage;
        private VideoInfo videoInfo;
        
        public DownloadResult(boolean success, String filePath, String errorMessage, VideoInfo videoInfo) {
            this.success = success;
            this.filePath = filePath;
            this.errorMessage = errorMessage;
            this.videoInfo = videoInfo;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getFilePath() { return filePath; }
        public String getErrorMessage() { return errorMessage; }
        public VideoInfo getVideoInfo() { return videoInfo; }
    }
    
    /**
     * 构造函数
     */
    public YtDlpJava() {
        this.logger = new Logger(true, false, false);
        this.options = new HashMap<>();
        this.progressCallbacks = new ArrayList<>();
        this.extractorRegistry = new ExtractorRegistry();
        
        // 设置默认选项
        setDefaultOptions();
    }
    
    /**
     * 构造函数（带选项）
     */
    public YtDlpJava(Map<String, String> options) {
        this();
        this.options.putAll(options);
    }
    
    /**
     * 设置默认选项
     */
    private void setDefaultOptions() {
        // 只保留必要的默认选项，避免与Android应用传递的参数冲突
        options.put("quiet", "false");
        options.put("verbose", "true");
        options.put("no_warnings", "false");
    }
    
    /**
     * 设置选项
     */
    public void setOption(String key, String value) {
        options.put(key, value);
    }
    
    /**
     * 获取选项
     */
    public String getOption(String key) {
        return options.get(key);
    }
    
    /**
     * 添加进度回调
     */
    public void addProgressCallback(ProgressCallback callback) {
        progressCallbacks.add(callback);
    }
    
    /**
     * 设置HTTP头部
     */
    public void setHttpHeaders(Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            // 将headers存储到options中，供下载器使用
            StringBuilder headerStr = new StringBuilder();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (headerStr.length() > 0) {
                    headerStr.append("\n");
                }
                headerStr.append(entry.getKey()).append(": ").append(entry.getValue());
            }
            options.put("http_headers", headerStr.toString());
            logger.info("设置HTTP头部: %s", headerStr.toString());
        }
    }
    
    /**
     * 移除进度回调
     */
    public void removeProgressCallback(ProgressCallback callback) {
        progressCallbacks.remove(callback);
    }
    
    /**
     * 主入口方法 - 下载视频
     * @param url 视频URL
     * @return 下载结果
     */
    public DownloadResult download(String url) {
        return download(url, null);
    }
    
    /**
     * 兼容Python版本的execute方法
     * @param request 请求对象
     * @param processId 进程ID（用于取消）
     * @param callback 进度回调
     * @return 响应对象
     */
    public YoutubeDLResponse execute(YoutubeDLRequest request, String processId, ProgressCallback callback) {
        long startTime = System.currentTimeMillis();
        List<String> command = request.buildCommand();
        
        try {
            logger.info("=== 执行兼容Python版本的下载 ===");
            logger.info("命令: %s", command);
            
            // 获取URL
            List<String> urls = request.getUrls();
            if (urls.isEmpty()) {
                throw new IllegalArgumentException("No URLs provided");
            }
            String url = urls.get(0);
            
            // 获取选项
            DownloadOptions options = request.getOptions();
            
            // 应用选项到YtDlpJava实例
            if (options.getHttpHeaders() != null) {
                // 将字符串格式的HTTP头部转换为Map格式
                Map<String, String> headers = parseHttpHeaders(options.getHttpHeaders());
                setHttpHeaders(headers);
            }
            
            // 处理其他自定义选项
            if (options.getCustomOptions() != null && !options.getCustomOptions().isEmpty()) {
                Map<String, String> customOptions = options.getCustomOptions();
                logger.info("处理剩余自定义选项: %s", customOptions);
                
                // 处理剩余的自定义选项
                for (Map.Entry<String, String> entry : customOptions.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    
                    // 跳过已经通过标准字段处理的选项
                    if ("-o".equals(key) || "-f".equals(key)) {
                        continue;
                    }
                    
                    // 处理其他选项
                    if (key.startsWith("--")) {
                        // 长选项格式
                        String optionName = key.substring(2);
                        if (value != null && !value.isEmpty()) {
                            setOption(optionName, value);
                            logger.info("设置选项 %s = %s", optionName, value);
                        } else {
                            setOption(optionName, "true");
                            logger.info("设置布尔选项 %s = true", optionName);
                        }
                    } else if (key.startsWith("-")) {
                        // 短选项格式
                        String optionName = key.substring(1);
                        if (value != null && !value.isEmpty()) {
                            setOption(optionName, value);
                            logger.info("设置选项 %s = %s", optionName, value);
                        } else {
                            setOption(optionName, "true");
                            logger.info("设置布尔选项 %s = true", optionName);
                        }
                    }
                }
            }
            
            // 处理输出路径
            String outputPath = options.getOutput();
            if (outputPath == null && options.getOutputTemplate() != null) {
                outputPath = options.getOutputTemplate();
            }
            
            logger.info("最终输出路径: %s", outputPath);
            
            // 根据请求类型决定是提取信息还是下载
            boolean isInfoRequest = request.hasOption("--dump-json");
            
            if (isInfoRequest) {
                // 提取信息
                VideoInfo videoInfo = extractInfo(url);
                if (videoInfo != null) {
                    // 将VideoInfo转换为JSON字符串（简化版本）
                    String jsonOutput = convertVideoInfoToJson(videoInfo);
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    return new YoutubeDLResponse(command, 0, elapsedTime, jsonOutput, "");
                } else {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    return new YoutubeDLResponse(command, 1, elapsedTime, "", "Failed to extract video information");
                }
            } else {
                // 下载视频
                if (options.getFormat() != null) {
                    // 如果有格式指定，先提取信息然后选择格式
                    VideoInfo videoInfo = extractInfo(url);
                    if (videoInfo == null) {
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        return new YoutubeDLResponse(command, 1, elapsedTime, "", "Failed to extract video information");
                    }
                    
                    // 根据格式选择视频格式
                    VideoFormat selectedFormat = selectFormatBySpec(videoInfo.getFormats(), options.getFormat());
                    if (selectedFormat == null) {
                        long elapsedTime = System.currentTimeMillis() - startTime;
                        return new YoutubeDLResponse(command, 1, elapsedTime, "", "No matching format found: " + options.getFormat());
                    }
                    
                    // 下载指定格式
                    DownloadResult result = downloadFormat(selectedFormat, outputPath);
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    
                    if (result.isSuccess()) {
                        String output = String.format("下载完成: %s", result.getFilePath());
                        return new YoutubeDLResponse(command, 0, elapsedTime, output, "");
                    } else {
                        return new YoutubeDLResponse(command, 1, elapsedTime, "", result.getErrorMessage());
                    }
                } else {
                    // 直接下载（使用原有逻辑）
                    DownloadResult result = download(url, outputPath);
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    
                    if (result.isSuccess()) {
                        String output = String.format("下载完成: %s", result.getFilePath());
                        return new YoutubeDLResponse(command, 0, elapsedTime, output, "");
                    } else {
                        return new YoutubeDLResponse(command, 1, elapsedTime, "", result.getErrorMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            String errorMsg = "执行过程中发生错误: " + e.getMessage();
            logger.error(errorMsg, e);
            return new YoutubeDLResponse(command, 1, elapsedTime, "", errorMsg);
        }
    }
    
    /**
     * 根据格式规格选择视频格式
     */
    private VideoFormat selectFormatBySpec(List<VideoFormat> formats, String formatSpec) {
        if (formats == null || formats.isEmpty()) {
            return null;
        }
        
        // 简化的格式选择逻辑
        // 实际应该实现完整的yt-dlp格式选择语法
        if ("best".equals(formatSpec)) {
            return selectBestFormat(formats);
        } else if (formatSpec.startsWith("best[")) {
            // 解析格式选择器，如 "best[height<=720]"
            return selectBestFormatWithCriteria(formats, formatSpec);
        } else {
            // 按格式ID查找
            for (VideoFormat format : formats) {
                if (formatSpec.equals(format.getFormatId())) {
                    return format;
                }
            }
        }
        
        return formats.get(0); // 默认返回第一个
    }
    
    /**
     * 选择最佳格式（带条件）
     */
    private VideoFormat selectBestFormatWithCriteria(List<VideoFormat> formats, String criteria) {
        // 简化实现，实际应该解析复杂的格式选择语法
        if (criteria.contains("height<=")) {
            // 提取高度限制
            String heightStr = criteria.substring(criteria.indexOf("height<=") + 8);
            heightStr = heightStr.substring(0, heightStr.indexOf("]"));
            int maxHeight = Integer.parseInt(heightStr);
            
            // 选择符合高度限制的最佳格式
            VideoFormat best = null;
            int bestHeight = 0;
            
            for (VideoFormat format : formats) {
                if (format.getHeight() != null && format.getHeight() <= maxHeight) {
                    if (best == null || format.getHeight() > bestHeight) {
                        best = format;
                        bestHeight = format.getHeight();
                    }
                }
            }
            
            return best != null ? best : selectBestFormat(formats);
        }
        
        return selectBestFormat(formats);
    }
    
    /**
     * 将VideoInfo转换为JSON字符串（简化版本）
     */
    private String convertVideoInfoToJson(VideoInfo videoInfo) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": \"").append(escapeJson(videoInfo.getId())).append("\",\n");
        json.append("  \"title\": \"").append(escapeJson(videoInfo.getTitle())).append("\",\n");
        json.append("  \"url\": \"").append(escapeJson(videoInfo.getUrl())).append("\",\n");
        json.append("  \"duration\": ").append(videoInfo.getDuration()).append(",\n");
        json.append("  \"uploader\": \"").append(escapeJson(videoInfo.getUploader())).append("\",\n");
        json.append("  \"formats\": [\n");
        
        List<VideoFormat> formats = videoInfo.getFormats();
        for (int i = 0; i < formats.size(); i++) {
            VideoFormat format = formats.get(i);
            json.append("    {\n");
            json.append("      \"format_id\": \"").append(escapeJson(format.getFormatId())).append("\",\n");
            json.append("      \"url\": \"").append(escapeJson(format.getUrl())).append("\",\n");
            json.append("      \"ext\": \"").append(escapeJson(format.getExt())).append("\",\n");
            json.append("      \"protocol\": \"").append(escapeJson(format.getProtocol())).append("\",\n");
            json.append("      \"width\": ").append(format.getWidth() != null ? format.getWidth() : "null").append(",\n");
            json.append("      \"height\": ").append(format.getHeight() != null ? format.getHeight() : "null").append(",\n");
            json.append("      \"tbr\": ").append(format.getTbr() != null ? format.getTbr() : "null").append("\n");
            json.append("    }");
            if (i < formats.size() - 1) json.append(",");
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * 转义JSON字符串
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
    
    /**
     * 解析HTTP头部字符串为Map
     */
    private Map<String, String> parseHttpHeaders(String headersStr) {
        Map<String, String> headers = new HashMap<>();
        if (headersStr == null || headersStr.trim().isEmpty()) {
            return headers;
        }
        
        // 支持多种格式的HTTP头部字符串
        // 格式1: "User-Agent: Custom Agent\nReferer: https://example.com"
        // 格式2: "User-Agent: Custom Agent; Referer: https://example.com"
        String[] lines = headersStr.split("[\\n;]");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                String key = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(key, value);
            }
        }
        
        return headers;
    }
    
    /**
     * 直接下载指定格式（避免重复解析URL）
     * @param format 预选的视频格式
     * @param outputPath 输出路径
     * @return 下载结果
     */
    public DownloadResult downloadFormat(VideoFormat format, String outputPath) {
        try {
            logger.info("=== 直接下载指定格式 ===");
            logger.info("格式ID: %s, 协议: %s, URL: %s", 
                format.getFormatId(), format.getProtocol(), format.getUrl());
            
            // 直接下载，无需重新解析URL
            boolean success = downloadFormats(Arrays.asList(format), outputPath);
            
            if (success) {
                logger.info("格式下载完成: %s", outputPath);
                return new DownloadResult(true, outputPath, null, null);
            } else {
                String error = "格式下载失败";
                logger.error(error);
                return new DownloadResult(false, null, error, null);
            }
            
        } catch (Exception e) {
            String error = "格式下载过程中发生错误: " + e.getMessage();
            logger.error(error);
            e.printStackTrace();
            return new DownloadResult(false, null, error, null);
        }
    }
    
    /**
     * 主入口方法 - 下载视频（带输出路径）
     * @param url 视频URL
     * @param outputPath 输出路径
     * @return 下载结果
     */
    public DownloadResult download(String url, String outputPath) {
        try {
            logger.info("=== 开始视频下载 ===");
            logger.info("URL: %s", url);
            
            // 1. 自动选择提取器
            InfoExtractor extractor = createExtractor(url);
            if (extractor == null) {
                String error = "不支持的视频平台: " + getPlatformName(url);
                logger.error(error);
                return new DownloadResult(false, null, error, null);
            }
            
            logger.info("选择的提取器: %s", extractor.getIE_NAME());
            
            // 2. 提取视频信息
            VideoInfo videoInfo = extractor.extract(url);
            if (videoInfo == null || videoInfo.getFormats().isEmpty()) {
                String error = "无法提取视频信息或未找到可用格式";
                logger.error(error);
                return new DownloadResult(false, null, error, null);
            }
            
            logger.info("提取成功 - 标题: %s, 格式数量: %s", 
                videoInfo.getTitle(), videoInfo.getFormats().size());
            
            // 3. 格式选择
            List<VideoFormat> selectedFormats = selectFormats(videoInfo.getFormats());
            if (selectedFormats.isEmpty()) {
                String error = "未找到符合要求的视频格式";
                logger.error(error);
                return new DownloadResult(false, null, error, videoInfo);
            }
            
            logger.info("选择了 %s 个格式", selectedFormats.size());
            
            // 4. 生成输出文件名
            logger.info("原始输出路径: %s", outputPath);
            if (outputPath == null) {
                outputPath = generateOutputPath(videoInfo, selectedFormats.get(0));
                logger.info("生成的输出路径: %s", outputPath);
            } else {
                logger.info("使用提供的输出路径: %s", outputPath);
            }
            
            // 5. 下载视频
            boolean success = downloadFormats(selectedFormats, outputPath);
            
            if (success) {
                logger.info("下载完成: %s", outputPath);
                return new DownloadResult(true, outputPath, null, videoInfo);
            } else {
                String error = "视频下载失败";
                logger.error(error);
                return new DownloadResult(false, null, error, videoInfo);
            }
            
        } catch (Exception e) {
            String error = "下载过程中发生错误: " + e.getMessage();
            logger.error(error);
            e.printStackTrace();
            return new DownloadResult(false, null, error, null);
        }
    }
    
    /**
     * 提取视频信息（不下载）
     * @param url 视频URL
     * @return 视频信息
     */
    public VideoInfo extractInfo(String url) {
        try {
            logger.info("=== 提取视频信息 ===");
            logger.info("URL: %s", url);
            
            InfoExtractor extractor = createExtractor(url);
            if (extractor == null) {
                logger.error("不支持的视频平台: %s", getPlatformName(url));
                return null;
            }
            
            VideoInfo videoInfo = extractor.extract(url);
            if (videoInfo != null) {
                logger.info("信息提取成功 - 标题: %s, 格式数量: %s", 
                    videoInfo.getTitle(), videoInfo.getFormats().size());
            }
            
            return videoInfo;
            
        } catch (Exception e) {
            logger.error("信息提取失败: %s", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 列出可用格式
     * @param url 视频URL
     * @return 格式列表
     */
    public List<VideoFormat> listFormats(String url) {
        VideoInfo videoInfo = extractInfo(url);
        if (videoInfo != null) {
            return videoInfo.getFormats();
        }
        return new ArrayList<>();
    }
    
    /**
     * 根据URL自动选择提取器
     */
    private InfoExtractor createExtractor(String url) {
        try {
            // 使用ExtractorRegistry来获取合适的提取器
            return extractorRegistry.getExtractor(url);
        } catch (Exception e) {
            logger.error("创建提取器失败: %s", e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取平台名称
     */
    private String getPlatformName(String url) {
        if (url.contains("facebook.com")) return "Facebook";
        if (url.contains("instagram.com")) return "Instagram";
        if (url.contains("tiktok.com")) return "TikTok";
        if (url.contains("pornhub.com")) return "Pornhub";
        if (url.contains("xhamster.com")) return "XHamster";
        if (url.contains("xnxx.com")) return "XNXX";
        if (url.contains("xvideos.com")) return "XVideos";
        if (url.contains("twitter.com") || url.contains("x.com")) return "Twitter";
        if (url.contains("dailymotion.com")) return "Dailymotion";
        if (url.contains("vimeo.com")) return "Vimeo";
        return "未知平台";
    }
    
    /**
     * 选择视频格式
     */
    private List<VideoFormat> selectFormats(List<VideoFormat> formats) {
        String formatSpec = options.get("format");
        
        if ("best".equals(formatSpec)) {
            // 选择最佳质量
            return Arrays.asList(selectBestFormat(formats));
        } else if ("worst".equals(formatSpec)) {
            // 选择最低质量
            return Arrays.asList(selectWorstFormat(formats));
        } else if (formatSpec.matches("\\d+")) {
            // 选择特定质量
            int targetQuality = Integer.parseInt(formatSpec);
            return formats.stream()
                .filter(f -> f.getQuality() != null && f.getQuality() <= targetQuality)
                .max(Comparator.comparing(VideoFormat::getQuality))
                .map(Arrays::asList)
                .orElse(Arrays.asList(selectBestFormat(formats)));
        } else {
            // 默认选择最佳质量
            return Arrays.asList(selectBestFormat(formats));
        }
    }
    
    /**
     * 选择最佳格式
     */
    private VideoFormat selectBestFormat(List<VideoFormat> formats) {
        // 优先选择MP4格式，然后按质量排序
        VideoFormat bestMp4 = formats.stream()
            .filter(f -> "mp4".equals(f.getExt()))
            .max(Comparator.comparing(f -> f.getQuality() != null ? f.getQuality() : 0))
            .orElse(null);
            
        if (bestMp4 != null) {
            return bestMp4;
        }
        
        // 如果没有MP4，选择质量最高的
        return formats.stream()
            .max(Comparator.comparing(f -> f.getQuality() != null ? f.getQuality() : 0))
            .orElse(formats.get(0));
    }
    
    /**
     * 选择最低格式
     */
    private VideoFormat selectWorstFormat(List<VideoFormat> formats) {
        return formats.stream()
            .min(Comparator.comparing(f -> f.getQuality() != null ? f.getQuality() : Integer.MAX_VALUE))
            .orElse(formats.get(0));
    }
    
    /**
     * 生成输出路径
     */
    private String generateOutputPath(VideoInfo videoInfo, VideoFormat format) {
        String template = options.get("output");
        if (template == null || template.trim().isEmpty()) {
            // 如果没有指定输出模板，生成一个简单的文件名
            String ext = format.getExt();
            if ("hls".equals(format.getProtocol()) || "m3u8".equals(format.getProtocol())) {
                ext = "mp4";  // FFmpeg会转换为MP4格式
            }
            template = sanitizeFileName(videoInfo.getTitle()) + "." + ext;
        }
        
        // 对于HLS格式，使用MP4扩展名（FFmpeg会转换为MP4）
        String ext = format.getExt();
        if ("hls".equals(format.getProtocol()) || "m3u8".equals(format.getProtocol())) {
            ext = "mp4";  // FFmpeg会转换为MP4格式
        }
        
        String fileName = template
            .replace("%(title)s", sanitizeFileName(videoInfo.getTitle()))
            .replace("%(ext)s", ext)
            .replace("%(id)s", videoInfo.getId());
            
        // 确保路径是绝对路径
        if (!new File(fileName).isAbsolute()) {
            // 在Android环境中，使用应用的临时目录
            if (isAndroidEnvironment()) {
                // 使用Android应用的缓存目录
                String cacheDir = System.getProperty("java.io.tmpdir");
                if (cacheDir == null || cacheDir.isEmpty()) {
                    cacheDir = "/data/local/tmp";
                }
                fileName = new File(cacheDir, fileName).getAbsolutePath();
            } else {
                // 如果模板不包含完整路径，使用当前工作目录
                fileName = new File(System.getProperty("user.dir"), fileName).getAbsolutePath();
            }
        }
        
        return fileName;
    }
    
    /**
     * 检查是否在Android环境中运行
     */
    private boolean isAndroidEnvironment() {
        try {
            Class.forName("android.os.Build");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 清理文件名
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "video_" + System.currentTimeMillis();
        }
        
        // 移除非法字符
        return fileName.replaceAll("[^a-zA-Z0-9\\s\\-_]", "")
                      .replaceAll("\\s+", "_")
                      .trim();
    }
    
    /**
     * 下载格式列表
     */
    private boolean downloadFormats(List<VideoFormat> formats, String outputPath) {
        // Android环境检查
        boolean isAndroid = isAndroidEnvironment();
        if (isAndroid) {
            logger.info("检测到Android环境，使用兼容模式");
        }
        
        logger.info("downloadFormats开始 - 格式数量: %d, 输出路径: %s", formats.size(), outputPath);
        
        for (VideoFormat format : formats) {
            try {
                logger.info("开始下载格式: " + format.getFormatId() + " (" + format.getExt() + ")");
                logger.info("格式协议: " + format.getProtocol());
                logger.info("输出路径: " + outputPath);
                
                // 确保输出目录存在
                File outputFile = new File(outputPath);
                File parentDir = outputFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    boolean dirCreated = parentDir.mkdirs();
                    if (!dirCreated && !parentDir.exists()) {
                        logger.error("无法创建输出目录: " + parentDir.getAbsolutePath());
                        notifyErrorCallbacks("无法创建输出目录: " + parentDir.getAbsolutePath());
                        return false;
                    }
                }
                
                // 根据协议选择下载器
                boolean success = false;
                String protocol = format.getProtocol();
                
                if (isStreamingFormat(format)) {
                    // 使用FFmpeg下载器处理流媒体格式
                    logger.info("使用FFmpeg下载器处理流媒体格式");
                    logger.info("FFmpeg下载前 - 格式ID: %s, URL: %s", format.getFormatId(), format.getUrl());
                    success = downloadWithFfmpeg(format, outputPath);
                    logger.info("FFmpeg下载后 - 结果: %s", success);
                } else {
                    // 使用原有的HTTP下载器
                    logger.info("使用HTTP下载器");
                    success = downloadWithHttp(format, outputPath);
                }
                
                logger.info("下载完成，结果: " + success);
                
                if (success) {
                    // 验证下载的文件
                    File downloadedFile = new File(outputPath);
                    if (downloadedFile.exists() && downloadedFile.length() > 0) {
                        logger.info("下载成功，文件大小: " + downloadedFile.length() + " bytes");
                        // 通知进度回调
                        notifyProgressCallbacks(100, downloadedFile.length(), downloadedFile.length());
                        notifyCompleteCallbacks(outputPath);
                        return true;
                    } else {
                        logger.error("下载完成但文件无效: " + outputPath);
                        notifyErrorCallbacks("下载完成但文件无效");
                        return false;
                    }
                }
                
            } catch (Exception e) {
                logger.error("下载格式失败: %s", e.getMessage());
                e.printStackTrace();
                notifyErrorCallbacks("下载失败: " + e.getMessage());
            }
        }
        
        return false;
    }
    
    /**
     * 使用HLS下载器下载
     */
    private boolean downloadWithPureJavaHls(VideoFormat format, String outputPath) throws Exception {
        logger.info("=== 使用HLS下载器 ===");
        
        // 使用现有的HlsDownloader（已经过测试并支持fMP4合并）
        com.ytdlp.downloader.hls.HlsDownloader hlsDownloader = 
            new com.ytdlp.downloader.hls.HlsDownloader();
        
        // 初始化下载器
        hlsDownloader.initialize(null, logger);
        
        // 创建虚拟VideoInfo对象
        VideoInfo dummyInfo = new VideoInfo();
        dummyInfo.setTitle("HLS Video");
        
        // 执行下载
        File outputFile = new File(outputPath);
        boolean success = hlsDownloader.download(dummyInfo, format, outputFile.getAbsolutePath());
        
        logger.info("HLS下载结果: " + success);
        return success;
    }
    
    /**
     * 判断是否为流媒体格式
     * 参考Android工程的判断逻辑
     */
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
    
    /**
     * 使用FFmpeg HLS下载器下载
     */
    private boolean downloadWithFfmpeg(VideoFormat format, String outputPath) throws Exception {
        logger.info("=== 使用Android FFmpeg HLS下载器 ===");
        logger.info("downloadWithFfmpeg调用 - 格式ID: %s, URL: %s, 输出路径: %s", 
            format.getFormatId(), format.getUrl(), outputPath);
        
        // 创建Android FFmpeg下载器
        com.ytdlp.downloader.hls.AndroidFfmpegHlsDownloader androidFfmpegDownloader = 
            new com.ytdlp.downloader.hls.AndroidFfmpegHlsDownloader();
        
        // 设置日志器
        androidFfmpegDownloader.setLogger(logger);
        
        // 设置进度回调
        androidFfmpegDownloader.setProgressCallback(new com.ytdlp.downloader.ProgressCallback() {
            @Override
            public void onDownloadStart(long totalBytes) {
                logger.info("FFmpeg下载开始，总字节数: %d", totalBytes);
                // 调用YtDlpJava的进度回调
                notifyProgressCallbacks(0, 0, totalBytes);
            }
            
            @Override
            public void onProgress(long bytesDownloaded, long totalBytes, long speed) {
                int percentage = totalBytes > 0 ? (int) ((bytesDownloaded * 100) / totalBytes) : 0;
                logger.info("FFmpeg下载进度: %d%% (%d/%d bytes, %d bytes/s)", 
                    percentage, bytesDownloaded, totalBytes, speed);
                // 调用YtDlpJava的进度回调
                notifyProgressCallbacks(percentage, bytesDownloaded, totalBytes);
            }
            
            @Override
            public void onDownloadComplete(long bytesDownloaded, long totalBytes) {
                logger.info("FFmpeg下载完成: %d bytes", bytesDownloaded);
                // 调用YtDlpJava的完成回调
                notifyCompleteCallbacks(outputPath);
            }
            
            @Override
            public void onDownloadError(String errorMessage, Exception exception) {
                logger.error("FFmpeg下载错误: %s", errorMessage);
                if (exception != null) {
                    logger.error("异常详情: %s", exception.getMessage());
                }
                // 调用YtDlpJava的错误回调
                notifyErrorCallbacks(errorMessage);
            }
            
            @Override
            public void onDownloadCancelled(long bytesDownloaded) {
                logger.info("FFmpeg下载取消，已下载字节数: %d", bytesDownloaded);
            }
        });
        
        // 初始化FFmpegKit（Android环境）
        androidFfmpegDownloader.initialize();
        
        // 检查FFmpeg是否可用
        if (!androidFfmpegDownloader.isAvailable()) {
            logger.error("FFmpeg不可用，回退到纯Java HLS下载器");
            return downloadWithPureJavaHls(format, outputPath);
        }
        
        // 创建虚拟VideoInfo对象
        VideoInfo dummyInfo = new VideoInfo();
        dummyInfo.setTitle("HLS Video");
        
        // 确保输出路径正确传递
        logger.info("传递给FFmpeg的输出路径: %s", outputPath);
        logger.info("输出路径长度: %d", outputPath.length());
        logger.info("输出路径是否为绝对路径: %s", new File(outputPath).isAbsolute());
        
        // 执行下载
        logger.info("开始调用AndroidFfmpegHlsDownloader.download");
        boolean success = androidFfmpegDownloader.download(dummyInfo, format, outputPath);
        logger.info("AndroidFfmpegHlsDownloader.download完成，结果: %s", success);
        
        logger.info("Android FFmpeg下载结果: " + success);
        return success;
    }
    
    /**
     * 使用HTTP下载器下载
     */
    private boolean downloadWithHttp(VideoFormat format, String outputPath) throws Exception {
        logger.info("=== 使用HTTP下载器 ===");
        
        // 使用YoutubeDL核心进行下载
        YoutubeDL youtubeDL = YoutubeDL.getInstance();
        
        // 设置HTTP头部信息
        String httpHeaders = options.get("http_headers");
        if (httpHeaders != null && !httpHeaders.isEmpty()) {
            youtubeDL.setHttpHeaders(httpHeaders);
        }
        
        // 直接使用YoutubeDL的downloadFormat方法
        return youtubeDL.downloadFormat(format, outputPath, "video");
    }
    
    /**
     * 通知进度回调
     */
    private void notifyProgressCallbacks(int percentage, long bytesDownloaded, long totalBytes) {
        for (ProgressCallback callback : progressCallbacks) {
            try {
                callback.onProgress(percentage, bytesDownloaded, totalBytes);
            } catch (Exception e) {
                logger.error("进度回调执行失败: %s", e.getMessage());
            }
        }
    }
    
    /**
     * 通知完成回调
     */
    private void notifyCompleteCallbacks(String filePath) {
        for (ProgressCallback callback : progressCallbacks) {
            try {
                callback.onComplete(filePath);
            } catch (Exception e) {
                logger.error("完成回调执行失败: %s", e.getMessage());
            }
        }
    }
    
    /**
     * 通知错误回调
     */
    private void notifyErrorCallbacks(String error) {
        for (ProgressCallback callback : progressCallbacks) {
            try {
                callback.onError(error);
            } catch (Exception e) {
                logger.error("错误回调执行失败: %s", e.getMessage());
            }
        }
    }
    
    /**
     * 获取支持的平台列表
     */
    public static List<String> getSupportedPlatforms() {
        return Arrays.asList(
            "Facebook", "Instagram", "TikTok", "Twitter", 
            "Dailymotion", "Vimeo", "Pornhub", "XHamster",
            "XVideos", "XNXX", "YouTube"
        );
    }
    
    /**
     * 检查URL是否支持
     */
    public static boolean isUrlSupported(String url) {
        return url.contains("facebook.com") || url.contains("instagram.com") ||
               url.contains("tiktok.com") || url.contains("twitter.com") ||
               url.contains("x.com") || url.contains("dailymotion.com") ||
               url.contains("vimeo.com") || url.contains("pornhub.com") ||
               url.contains("xhamster.com") || url.contains("xvideos.com") ||
               url.contains("xnxx.com") || url.contains("youtube.com") ||
               url.contains("youtu.be");
    }
}
