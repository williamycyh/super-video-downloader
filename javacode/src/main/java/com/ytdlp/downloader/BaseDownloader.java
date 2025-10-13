package com.ytdlp.downloader;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.options.YoutubeDLOptions;
import com.ytdlp.utils.Logger;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 下载器基类
 */
public abstract class BaseDownloader implements FileDownloader {
    
    protected YoutubeDLOptions params;
    protected Logger logger;
    protected ProgressCallback progressCallback;
    protected DownloadStats stats;
    protected final AtomicBoolean downloading = new AtomicBoolean(false);
    protected final AtomicBoolean cancelled = new AtomicBoolean(false);
    
    public BaseDownloader() {
        this.stats = new DownloadStats();
    }
    
    public void initialize(YoutubeDLOptions params, Logger logger) {
        this.params = params;
        this.logger = logger;
        
        // 确保logger不为null
        if (this.logger == null) {
            this.logger = new Logger(true, true, true);
        }
    }
    
    public void setProgressCallback(ProgressCallback progressCallback) {
        this.progressCallback = progressCallback;
    }
    
    public DownloadStats getDownloadStats() {
        return stats;
    }
    
    public void cancel() {
        cancelled.set(true);
    }
    
    public boolean isDownloading() {
        return downloading.get();
    }
    
    @Override
    public boolean download(VideoInfo videoInfo, VideoFormat format, String outputPath) {
        if (downloading.get()) {
            logger.warning("Download already in progress");
            return false;
        }
        
        downloading.set(true);
        cancelled.set(false);
        stats.reset();
        
        try {
            File outputFile = new File(outputPath);
            
            // 验证参数
            if (!validateParameters(videoInfo, format, outputFile)) {
                return false;
            }
            
            // 创建输出目录
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // 执行下载
            boolean success = performDownload(videoInfo, format, outputFile);
            
            if (success && !cancelled.get()) {
                logger.info("Download completed: " + outputFile.getAbsolutePath());
                stats.setCompleted(true);
            } else if (cancelled.get()) {
                logger.info("Download cancelled");
                stats.setCancelled(true);
            } else {
                logger.error("Download failed");
                stats.setFailed(true);
            }
            
            return success && !cancelled.get();
            
        } catch (Exception e) {
            logger.error("Download error: " + e.getMessage());
            stats.setFailed(true);
            return false;
        } finally {
            downloading.set(false);
        }
    }
    
    protected abstract boolean performDownload(VideoInfo videoInfo, VideoFormat format, File outputFile) throws Exception;
    
    protected void updateProgress(long bytesDownloaded, long totalBytes, long speed) {
        stats.updateProgress(bytesDownloaded, totalBytes, speed);
        
        if (progressCallback != null) {
            try {
                int percentage = totalBytes > 0 ? (int) ((bytesDownloaded * 100) / totalBytes) : 0;
                progressCallback.onProgress(percentage, bytesDownloaded, totalBytes);
            } catch (Exception e) {
                logger.error("Progress callback error: " + e.getMessage());
            }
        }
    }
    
    protected boolean shouldCancel() {
        return cancelled.get();
    }
    
    protected boolean retryDownload(String url, int maxRetries) {
        int retryCount = getRetryCount();
        if (retryCount >= maxRetries) {
            return false;
        }
        
        try {
            Thread.sleep(1000 * (retryCount + 1)); // 递增延迟
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    protected Map<String, String> getHttpHeaders(VideoFormat format) {
        Map<String, String> headers = new HashMap<>();
        
        // 添加用户代理
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        // 添加格式特定的头部
        if (format.getHttpHeaders() != null) {
            headers.putAll(format.getHttpHeaders());
        }
        
        // 添加引用页面
        headers.put("Referer", "https://www.google.com/");
        
        return headers;
    }
    
    protected boolean validateParameters(VideoInfo videoInfo, VideoFormat format, File outputFile) {
        if (videoInfo == null) {
            logger.error("VideoInfo is null");
            return false;
        }
        
        if (format == null) {
            logger.error("VideoFormat is null");
            return false;
        }
        
        if (format.getUrl() == null || format.getUrl().isEmpty()) {
            logger.error("Video URL is null or empty");
            return false;
        }
        
        if (outputFile == null) {
            logger.error("Output file is null");
            return false;
        }
        
        return true;
    }
    
    protected int getRetryCount() {
        return stats.getRetryCount();
    }
    
    protected long getRateLimitBytesPerSecond() {
        String rateLimit = params != null ? params.getString("ratelimit") : null;
        if (rateLimit != null) {
            return parseRateLimit(rateLimit);
        }
        return 0; // 无限制
    }
    
    private long parseRateLimit(String rateLimit) {
        if (rateLimit == null || rateLimit.isEmpty()) {
            return 0;
        }
        
        try {
            String lower = rateLimit.toLowerCase();
            if (lower.endsWith("k")) {
                return Long.parseLong(lower.substring(0, lower.length() - 1)) * 1024;
            } else if (lower.endsWith("m")) {
                return Long.parseLong(lower.substring(0, lower.length() - 1)) * 1024 * 1024;
            } else if (lower.endsWith("g")) {
                return Long.parseLong(lower.substring(0, lower.length() - 1)) * 1024 * 1024 * 1024;
            } else {
                return Long.parseLong(lower);
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid rate limit format: " + rateLimit);
            return 0;
        }
    }
}
