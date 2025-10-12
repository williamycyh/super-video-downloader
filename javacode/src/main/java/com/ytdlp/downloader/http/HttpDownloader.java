package com.ytdlp.downloader.http;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.downloader.BaseDownloader;
import com.ytdlp.utils.Logger;

import java.io.*;
import java.net.*;
import java.util.Map;

/**
 * HTTP下载器
 */
public class HttpDownloader extends BaseDownloader {
    
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECT_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 60000;
    
    public HttpDownloader() {
        super();
    }
    
    public String getDownloaderName() {
        return "http";
    }
    
    public String getDownloaderDescription() {
        return "HTTP video downloader";
    }
    
    public boolean suitable(VideoFormat format) {
        if (format == null || format.getUrl() == null) {
            return false;
        }
        
        String url = format.getUrl().toLowerCase();
        String protocol = format.getProtocol();
        
        // 支持HTTP和HTTPS协议
        return "http".equals(protocol) || "https".equals(protocol) || 
               url.startsWith("http://") || url.startsWith("https://");
    }
    
    @Override
    protected boolean performDownload(VideoInfo videoInfo, VideoFormat format, File outputFile) throws Exception {
        String url = format.getUrl();
        logger.info("Starting HTTP download: " + url);
        
        // 检查URL是否可访问
        if (!isUrlAccessible(url, format)) {
            throw new IOException("URL is not accessible: " + url);
        }
        
        // 获取文件大小
        long fileSize = getFileSize(url, format);
        if (fileSize > 0) {
            stats.setTotalBytes(fileSize);
        }
        
        // 执行下载
        boolean success = downloadFile(url, outputFile, format);
        
        if (success) {
            logger.info("HTTP download completed: " + outputFile.getAbsolutePath());
        } else {
            logger.error("HTTP download failed: " + url);
        }
        
        return success;
    }
    
    private boolean retryDownload(String url, int maxRetries, DownloadOperation operation) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                stats.setRetryCount(stats.getRetryCount() + 1);
                
                if (attempt < maxRetries - 1) {
                    long delay = 1000 * (attempt + 1); // 递增延迟
                    logger.info("Retrying download in " + delay + "ms (attempt " + (attempt + 2) + "/" + maxRetries + ")");
                    Thread.sleep(delay);
                }
            }
        }
        
        throw lastException;
    }
    
    private boolean downloadFile(String url, File outputFile, VideoFormat format) throws Exception {
        return retryDownload(url, 3, () -> {
            URL urlObj = new URL(url);
            HttpURLConnection connection = createConnection(urlObj, format);
            
            try {
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 获取内容长度
                    long contentLength = connection.getContentLengthLong();
                    if (contentLength > 0 && stats.getTotalBytes() == 0) {
                        stats.setTotalBytes(contentLength);
                    }
                    
                    // 下载文件
                    try (InputStream inputStream = connection.getInputStream();
                         FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                        
                        byte[] buffer = new byte[BUFFER_SIZE];
                        long totalBytesRead = 0;
                        long lastUpdateTime = System.currentTimeMillis();
                        
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            if (shouldCancel()) {
                                logger.info("Download cancelled by user");
                                return false;
                            }
                            
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            
                            // 更新进度
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastUpdateTime >= 1000) { // 每秒更新一次
                                long speed = totalBytesRead * 1000 / (currentTime - (stats.getDownloadTimeMs() > 0 ? stats.getDownloadTimeMs() : currentTime));
                                updateProgress(totalBytesRead, stats.getTotalBytes(), speed);
                                lastUpdateTime = currentTime;
                            }
                            
                            // 应用速率限制
                            applyRateLimit(bytesRead);
                        }
                        
                        // 最终进度更新
                        updateProgress(totalBytesRead, stats.getTotalBytes(), 0);
                        
                        return true;
                    }
                } else {
                    throw new IOException("HTTP error: " + responseCode + " " + connection.getResponseMessage());
                }
                
            } finally {
                connection.disconnect();
            }
        });
    }
    
    private HttpURLConnection createConnection(URL url, VideoFormat format) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        connection.setInstanceFollowRedirects(true);
        
        // 设置请求头
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setRequestProperty("Range", "bytes=0-");
        
        // 添加格式特定的头部
        if (format.getHttpHeaders() != null) {
            for (Map.Entry<String, String> entry : format.getHttpHeaders().entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        
        // 设置引用页面
        if (format.getUrl() != null) {
            try {
                URL formatUrl = new URL(format.getUrl());
                connection.setRequestProperty("Referer", formatUrl.getProtocol() + "://" + formatUrl.getHost());
            } catch (Exception e) {
                connection.setRequestProperty("Referer", "https://www.google.com/");
            }
        }
        
        return connection;
    }
    
    private void applyRateLimit(int bytesRead) {
        long rateLimit = getRateLimitBytesPerSecond();
        if (rateLimit > 0) {
            long sleepTime = (bytesRead * 1000) / rateLimit;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    public long getFileSize(String url, VideoFormat format) throws Exception {
        URL urlObj = new URL(url);
        HttpURLConnection connection = createConnection(urlObj, format);
        
        try {
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return connection.getContentLengthLong();
            } else {
                logger.warning("Could not get file size, HTTP error: " + responseCode);
                return -1;
            }
        } finally {
            connection.disconnect();
        }
    }
    
    public boolean isUrlAccessible(String url, VideoFormat format) {
        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = createConnection(urlObj, format);
            
            try {
                connection.setRequestMethod("HEAD");
                int responseCode = connection.getResponseCode();
                return responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL;
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            logger.warning("URL accessibility check failed: " + e.getMessage());
            return false;
        }
    }
    
    private boolean lambda$performDownload$0(String url, File outputFile, VideoFormat format) throws Exception {
        return downloadFile(url, outputFile, format);
    }
    
    /**
     * 下载操作接口
     */
    @FunctionalInterface
    private interface DownloadOperation {
        boolean execute() throws Exception;
    }
    
    /**
     * 下载操作数据类
     */
    public static class DownloadOperationData {
        private final String url;
        private final File outputFile;
        private final VideoFormat format;
        
        public DownloadOperationData(String url, File outputFile, VideoFormat format) {
            this.url = url;
            this.outputFile = outputFile;
            this.format = format;
        }
        
        public String getUrl() {
            return url;
        }
        
        public File getOutputFile() {
            return outputFile;
        }
        
        public VideoFormat getFormat() {
            return format;
        }
    }
}
