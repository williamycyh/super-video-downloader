package com.ytdlp;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.core.YoutubeDL;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.extractor.ExtractorRegistry;
import com.ytdlp.utils.Logger;

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
        options.put("quiet", "false");
        options.put("verbose", "true");
        options.put("format", "best");
        options.put("output", "%(title)s.%(ext)s");
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
            logger.info("设置HTTP头部: {}", headerStr.toString());
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
     * 主入口方法 - 下载视频（带输出路径）
     * @param url 视频URL
     * @param outputPath 输出路径
     * @return 下载结果
     */
    public DownloadResult download(String url, String outputPath) {
        try {
            logger.info("=== 开始视频下载 ===");
            logger.info("URL: {}", url);
            
            // 1. 自动选择提取器
            InfoExtractor extractor = createExtractor(url);
            if (extractor == null) {
                String error = "不支持的视频平台: " + getPlatformName(url);
                logger.error(error);
                return new DownloadResult(false, null, error, null);
            }
            
            logger.info("选择的提取器: {}", extractor.getIE_NAME());
            
            // 2. 提取视频信息
            VideoInfo videoInfo = extractor.extract(url);
            if (videoInfo == null || videoInfo.getFormats().isEmpty()) {
                String error = "无法提取视频信息或未找到可用格式";
                logger.error(error);
                return new DownloadResult(false, null, error, null);
            }
            
            logger.info("提取成功 - 标题: {}, 格式数量: {}", 
                videoInfo.getTitle(), videoInfo.getFormats().size());
            
            // 3. 格式选择
            List<VideoFormat> selectedFormats = selectFormats(videoInfo.getFormats());
            if (selectedFormats.isEmpty()) {
                String error = "未找到符合要求的视频格式";
                logger.error(error);
                return new DownloadResult(false, null, error, videoInfo);
            }
            
            logger.info("选择了 {} 个格式", selectedFormats.size());
            
            // 4. 生成输出文件名
            if (outputPath == null) {
                outputPath = generateOutputPath(videoInfo, selectedFormats.get(0));
            }
            
            // 5. 下载视频
            boolean success = downloadFormats(selectedFormats, outputPath);
            
            if (success) {
                logger.info("下载完成: {}", outputPath);
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
            logger.info("URL: {}", url);
            
            InfoExtractor extractor = createExtractor(url);
            if (extractor == null) {
                logger.error("不支持的视频平台: {}", getPlatformName(url));
                return null;
            }
            
            VideoInfo videoInfo = extractor.extract(url);
            if (videoInfo != null) {
                logger.info("信息提取成功 - 标题: {}, 格式数量: {}", 
                    videoInfo.getTitle(), videoInfo.getFormats().size());
            }
            
            return videoInfo;
            
        } catch (Exception e) {
            logger.error("信息提取失败: {}", e.getMessage());
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
            logger.error("创建提取器失败: {}", e.getMessage());
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
        String fileName = template
            .replace("%(title)s", sanitizeFileName(videoInfo.getTitle()))
            .replace("%(ext)s", format.getExt())
            .replace("%(id)s", videoInfo.getId());
            
        return fileName;
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
        // 使用YoutubeDL核心进行下载
        YoutubeDL youtubeDL = YoutubeDL.getInstance();
        
        // 设置HTTP头部信息
        String httpHeaders = options.get("http_headers");
        if (httpHeaders != null && !httpHeaders.isEmpty()) {
            youtubeDL.setHttpHeaders(httpHeaders);
        }
        
        for (VideoFormat format : formats) {
            try {
                logger.info("开始下载格式: " + format.getFormatId() + " (" + format.getExt() + ")");
                logger.info("调用youtubeDL.downloadFormat，格式协议: " + format.getProtocol());
                logger.info("输出路径: " + outputPath);
                
                // 直接使用YoutubeDL的downloadFormat方法，而不是重新提取信息
                boolean success = youtubeDL.downloadFormat(format, outputPath, "video");
                
                logger.info("downloadFormat调用完成，结果: " + success);
                
                if (success) {
                    // 通知进度回调
                    notifyProgressCallbacks(100, -1, -1);
                    notifyCompleteCallbacks(outputPath);
                    return true;
                }
                
            } catch (Exception e) {
                logger.error("下载格式失败: {}", e.getMessage());
                e.printStackTrace();
                notifyErrorCallbacks("下载失败: " + e.getMessage());
            }
        }
        
        return false;
    }
    
    /**
     * 通知进度回调
     */
    private void notifyProgressCallbacks(int percentage, long bytesDownloaded, long totalBytes) {
        for (ProgressCallback callback : progressCallbacks) {
            try {
                callback.onProgress(percentage, bytesDownloaded, totalBytes);
            } catch (Exception e) {
                logger.error("进度回调执行失败: {}", e.getMessage());
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
                logger.error("完成回调执行失败: {}", e.getMessage());
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
                logger.error("错误回调执行失败: {}", e.getMessage());
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
