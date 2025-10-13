package com.ytdlp.downloader.hls;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.downloader.BaseDownloader;
import com.ytdlp.utils.Logger;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * HLS (HTTP Live Streaming) 下载器
 */
public class HlsDownloader extends BaseDownloader {
    
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_CONCURRENT_DOWNLOADS = 4;
    
    private ExecutorService executorService;
    private final List<Future<Void>> downloadTasks;
    
    public HlsDownloader() {
        super();
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        this.downloadTasks = new ArrayList<>();
        
        // 确保logger被初始化
        if (this.logger == null) {
            this.logger = new Logger(true, true, true);
        }
    }
    
    public String getDownloaderName() {
        return "hls";
    }
    
    public String getDownloaderDescription() {
        return "HLS (HTTP Live Streaming) downloader";
    }
    
    public boolean suitable(VideoFormat format) {
        if (format == null || format.getUrl() == null) {
            return false;
        }
        
        String url = format.getUrl().toLowerCase();
        String protocol = format.getProtocol();
        
        return "hls".equals(protocol) || url.contains(".m3u8") || url.contains("m3u8");
    }
    
    @Override
    protected boolean performDownload(VideoInfo videoInfo, VideoFormat format, File outputFile) throws Exception {
        String url = format.getUrl();
        logger.info("=== HLS Downloader performDownload 开始 ===");
        logger.info("VideoInfo: " + videoInfo.getTitle());
        logger.info("Format ID: " + format.getFormatId());
        logger.info("Format Protocol: " + format.getProtocol());
        logger.info("Format URL: " + url);
        logger.info("Output File: " + outputFile.getAbsolutePath());
        
        try {
            // 下载播放列表
            logger.info("开始下载播放列表...");
            HlsPlaylist playlist = downloadPlaylist(url, format);
            if (playlist == null || playlist.getSegments().isEmpty()) {
                throw new IOException("No segments found in playlist");
            }
            
            logger.info("播放列表下载完成，找到 " + playlist.getSegments().size() + " 个片段");
            
            // 下载所有片段
            logger.info("开始下载所有片段...");
            boolean success = downloadSegments(playlist, format, outputFile);
            
            if (success) {
                logger.info("HLS下载完成: " + outputFile.getAbsolutePath());
                logger.info("输出文件大小: " + outputFile.length() + " bytes");
            } else {
                logger.error("HLS下载失败");
            }
            
            logger.info("=== HLS Downloader performDownload 结束 ===");
            return success;
            
        } catch (Exception e) {
            logger.error("HLS下载过程中发生异常: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            shutdownExecutor();
        }
    }
    
    private HlsPlaylist downloadPlaylist(String url, VideoFormat format) throws Exception {
        URL urlObj = new URL(url);
        HttpURLConnection connection = createConnection(urlObj, format);
        
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode);
            }
            
            // 首先检查是否是主播放列表
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                boolean isMasterPlaylist = false;
                int lineCount = 0;
                
                // 检查前10行是否包含EXT-X-STREAM-INF
                while ((line = reader.readLine()) != null && lineCount < 10) {
                    if (line.contains("EXT-X-STREAM-INF")) {
                        isMasterPlaylist = true;
                        break;
                    }
                    lineCount++;
                }
                
                if (isMasterPlaylist) {
                    // 这是主播放列表，需要选择最佳变体
                    logger.info("检测到主播放列表，选择最佳变体");
                    connection.disconnect();
                    return downloadVariantPlaylist(url, format);
                }
                
                // 重置流到开始位置
                reader.close();
                connection.disconnect();
                
                // 重新创建连接
                connection = createConnection(urlObj, format);
                try (BufferedReader newReader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    return parsePlaylist(newReader, url);
                }
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    private HlsPlaylist parsePlaylist(BufferedReader reader, String baseUrl) throws Exception {
        HlsPlaylist playlist = new HlsPlaylist();
        String line;
        HlsSegment currentSegment = null;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.startsWith("#EXT-X-VERSION:")) {
                playlist.setVersion(parseVersion(line));
            } else if (line.startsWith("#EXT-X-TARGETDURATION:")) {
                playlist.setTargetDuration(parseTargetDuration(line));
            } else if (line.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
                playlist.setMediaSequence(parseMediaSequence(line));
            } else if (line.startsWith("#EXTINF:")) {
                // 创建一个新的片段并设置时长
                currentSegment = new HlsSegment();
                currentSegment.setDuration(parseSegmentDuration(line));
            } else if (line.startsWith("#EXT-X-ENDLIST")) {
                playlist.setEndList(true);
            } else if (!line.startsWith("#") && !line.isEmpty()) {
                // 这是一个片段URL
                if (currentSegment != null) {
                    currentSegment.setUrl(resolveUrl(line, baseUrl));
                    playlist.addSegment(currentSegment);
                    currentSegment = null; // 重置当前片段
                }
            }
        }
        
        return playlist;
    }
    
    private HlsPlaylist downloadVariantPlaylist(String url, VideoFormat format) throws Exception {
        // 下载变体播放列表，选择最佳质量
        URL urlObj = new URL(url);
        HttpURLConnection connection = createConnection(urlObj, format);
        
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                String bestUrl = null;
                int bestBandwidth = 0;
                
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    if (line.startsWith("#EXT-X-STREAM-INF:")) {
                        // 解析带宽信息
                        String bandwidthStr = extractAttribute(line, "BANDWIDTH");
                        if (bandwidthStr != null) {
                            int bandwidth = Integer.parseInt(bandwidthStr);
                            if (bandwidth > bestBandwidth) {
                                bestBandwidth = bandwidth;
                                String nextLine = reader.readLine();
                                if (nextLine != null && !nextLine.startsWith("#")) {
                                    bestUrl = resolveUrl(nextLine.trim(), url);
                                }
                            }
                        }
                    }
                }
                
                if (bestUrl != null) {
                    return downloadPlaylist(bestUrl, format);
                }
            }
            
        } finally {
            connection.disconnect();
        }
        
        return null;
    }
    
    private boolean downloadSegments(HlsPlaylist playlist, VideoFormat format, File outputFile) throws Exception {
        List<HlsSegment> segments = playlist.getSegments();
        Path tempDir = Files.createTempDirectory("hls_download");
        
        try {
            // 并发下载所有片段
            for (int i = 0; i < segments.size(); i++) {
                final HlsSegment segment = segments.get(i);
                // 根据片段URL的扩展名确定文件扩展名
                String extension = getSegmentExtension(segment.getUrl());
                final Path segmentFile = tempDir.resolve(String.format("segment_%06d.%s", i, extension));
                final int segmentIndex = i;
                
                Future<Void> task = executorService.submit(() -> {
                    downloadSegment(segment, format, segmentFile, segmentIndex);
                    return null;
                });
                
                downloadTasks.add(task);
            }
            
            // 等待所有下载完成
            for (Future<Void> task : downloadTasks) {
                try {
                    task.get();
                } catch (ExecutionException e) {
                    logger.error("Segment download failed: " + e.getCause().getMessage());
                    return false;
                }
            }
            
            // 合并片段
            return mergeSegments(playlist, outputFile, tempDir);
            
        } finally {
            // 清理临时文件
            try {
                Files.walk(tempDir)
                     .sorted(Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(File::delete);
            } catch (Exception e) {
                logger.warning("Failed to clean temp directory: " + e.getMessage());
            }
        }
    }
    
    private void downloadSegment(HlsSegment segment, VideoFormat format, Path segmentFile, int index) throws Exception {
        logger.info("下载片段 {}: {}", index, segment.getUrl());
        
        URL urlObj = new URL(segment.getUrl());
        HttpURLConnection connection = createConnection(urlObj, format);
        
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode + " for segment " + index);
            }
            
            long contentLength = connection.getContentLengthLong();
            logger.info("片段 {} 响应成功，内容长度: {} bytes", index, contentLength);
            
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(segmentFile.toFile())) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (shouldCancel()) {
                        throw new InterruptedException("Download cancelled");
                    }
                    
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                logger.debug("Downloaded segment " + index + ": " + totalBytes + " bytes");
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    private boolean mergeSegments(HlsPlaylist playlist, File outputFile, Path tempDir) throws Exception {
        logger.info("开始合并 {} 个片段到文件: {}", playlist.getSegments().size(), outputFile.getAbsolutePath());
        
        // 检查第一个片段来确定格式
        Path firstSegment = findFirstSegment(tempDir);
        if (firstSegment == null) {
            logger.error("第一个片段不存在");
            return false;
        }
        
        // 检查是否为fMP4格式
        boolean isFmp4 = isFmp4Format(firstSegment);
        logger.info("检测到格式: {}", isFmp4 ? "fMP4" : "TS");
        
        if (isFmp4) {
            return mergeFmp4Segments(playlist, outputFile, tempDir);
        } else {
            return mergeTsSegments(playlist, outputFile, tempDir);
        }
    }
    
    private Path findFirstSegment(Path tempDir) throws Exception {
        return findSegmentFile(tempDir, 0);
    }
    
    private Path findSegmentFile(Path tempDir, int index) throws Exception {
        // 查找指定索引的片段文件（可能是不同的扩展名）
        String[] extensions = {"ts", "m4s", "mp4"};
        for (String ext : extensions) {
            Path segmentFile = tempDir.resolve(String.format("segment_%06d.%s", index, ext));
            if (Files.exists(segmentFile)) {
                return segmentFile;
            }
        }
        return null;
    }
    
    private boolean isFmp4Format(Path segmentFile) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(segmentFile.toFile())) {
            byte[] header = new byte[8];
            if (inputStream.read(header) >= 8) {
                // 检查是否以MP4原子开头
                int size = ((header[0] & 0xFF) << 24) | ((header[1] & 0xFF) << 16) | 
                          ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
                String type = new String(header, 4, 4);
                
                logger.info("片段头部: size={}, type={}", size, type);
                
                // 常见的MP4原子类型
                return type.equals("ftyp") || type.equals("styp") || type.equals("moof") || 
                       type.equals("mdat") || type.equals("sidx");
            }
        }
        return false;
    }
    
    private boolean mergeTsSegments(HlsPlaylist playlist, File outputFile, Path tempDir) throws Exception {
        logger.info("使用TS格式合并");
        
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            for (int i = 0; i < playlist.getSegments().size(); i++) {
                Path segmentFile = findSegmentFile(tempDir, i);
                if (segmentFile == null || !Files.exists(segmentFile)) {
                    logger.warning("TS片段文件不存在: segment_{:06d}", i);
                    continue;
                }
                
                logger.info("合并TS片段 {}: {} (大小: {} bytes)", i, segmentFile, Files.size(segmentFile));
                
                try (FileInputStream inputStream = new FileInputStream(segmentFile.toFile())) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytesRead;
                        long totalBytesRead = 0;
                        
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                        }
                        
                        logger.info("TS片段 {} 合并完成，读取了 {} bytes", i, totalBytesRead);
                    }
            }
            
            logger.info("所有TS片段合并完成，输出文件大小: {} bytes", outputFile.length());
            return true;
        } catch (Exception e) {
            logger.error("合并TS片段失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private boolean mergeFmp4Segments(HlsPlaylist playlist, File outputFile, Path tempDir) throws Exception {
        logger.info("使用fMP4格式合并");
        
        // 对于fMP4，我们需要创建一个有效的MP4容器
        // 这里我们使用一个简化的方法：将第一个片段作为基础，然后追加其他片段的数据部分
        
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            // 写入第一个片段（通常包含初始化信息）
            Path firstSegment = findSegmentFile(tempDir, 0);
            if (firstSegment != null && Files.exists(firstSegment)) {
                logger.info("写入第一个fMP4片段: {} (大小: {} bytes)", firstSegment, Files.size(firstSegment));
                copyFile(firstSegment.toFile(), outputStream);
            }
            
            // 追加其他片段
            for (int i = 1; i < playlist.getSegments().size(); i++) {
                Path segmentFile = findSegmentFile(tempDir, i);
                if (segmentFile == null) {
                    logger.warning("fMP4片段文件不存在: segment_{:06d}", i);
                    continue;
                }
                
                if (Files.exists(segmentFile)) {
                    logger.info("追加fMP4片段 {}: {} (大小: {} bytes)", i, segmentFile, Files.size(segmentFile));
                    copyFile(segmentFile.toFile(), outputStream);
                } else {
                    logger.warning("fMP4片段文件不存在: {}", segmentFile);
                }
            }
            
            logger.info("所有fMP4片段合并完成，输出文件大小: {} bytes", outputFile.length());
            return true;
        } catch (Exception e) {
            logger.error("合并fMP4片段失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void copyFile(File source, FileOutputStream destination) throws Exception {
        try (FileInputStream inputStream = new FileInputStream(source)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                destination.write(buffer, 0, bytesRead);
            }
        }
    }
    
    private HttpURLConnection createConnection(URL url, VideoFormat format) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);
        connection.setInstanceFollowRedirects(true);
        
        // 设置请求头
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        connection.setRequestProperty("Connection", "keep-alive");
        
        // 添加全局HTTP头部
        if (params != null) {
            String httpHeaders = params.getHttpHeaders();
            if (httpHeaders != null && !httpHeaders.isEmpty()) {
                String[] headerLines = httpHeaders.split("\n");
                for (String headerLine : headerLines) {
                    String[] parts = headerLine.split(":", 2);
                    if (parts.length == 2) {
                        connection.setRequestProperty(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }
        
        // 添加格式特定的头部
        if (format.getHttpHeaders() != null) {
            for (Map.Entry<String, String> entry : format.getHttpHeaders().entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        
        return connection;
    }
    
    private String resolveUrl(String url, String baseUrl) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        
        try {
            URL base = new URL(baseUrl);
            return new URL(base, url).toString();
        } catch (MalformedURLException e) {
            return url;
        }
    }
    
    private String getSegmentExtension(String segmentUrl) {
        if (segmentUrl == null) {
            return "ts"; // 默认扩展名
        }
        
        // 检查URL中的扩展名
        if (segmentUrl.contains(".m4s")) {
            return "m4s";
        } else if (segmentUrl.contains(".ts")) {
            return "ts";
        } else if (segmentUrl.contains(".mp4")) {
            return "mp4";
        } else {
            return "ts"; // 默认扩展名
        }
    }
    
    private int parseVersion(String line) {
        try {
            return Integer.parseInt(line.substring("#EXT-X-VERSION:".length()).trim());
        } catch (Exception e) {
            return 3; // 默认版本
        }
    }
    
    private int parseTargetDuration(String line) {
        try {
            return Integer.parseInt(line.substring("#EXT-X-TARGETDURATION:".length()).trim());
        } catch (Exception e) {
            return 10; // 默认时长
        }
    }
    
    private long parseMediaSequence(String line) {
        try {
            return Long.parseLong(line.substring("#EXT-X-MEDIA-SEQUENCE:".length()).trim());
        } catch (Exception e) {
            return 0;
        }
    }
    
    private double parseSegmentDuration(String line) {
        try {
            String durationStr = line.substring("#EXTINF:".length()).split(",")[0].trim();
            return Double.parseDouble(durationStr);
        } catch (Exception e) {
            return 10.0; // 默认时长
        }
    }
    
    private String extractAttribute(String line, String attribute) {
        String pattern = attribute + "=";
        int index = line.indexOf(pattern);
        if (index >= 0) {
            int start = index + pattern.length();
            int end = line.indexOf(",", start);
            if (end < 0) {
                end = line.length();
            }
            return line.substring(start, end).trim();
        }
        return null;
    }
    
    private void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    
    /**
     * HLS播放列表类
     */
    public static class HlsPlaylist {
        private int version = 3;
        private int targetDuration = 10;
        private long mediaSequence = 0;
        private boolean endList = false;
        private List<HlsSegment> segments = new ArrayList<>();
        
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
        
        public int getTargetDuration() { return targetDuration; }
        public void setTargetDuration(int targetDuration) { this.targetDuration = targetDuration; }
        
        public long getMediaSequence() { return mediaSequence; }
        public void setMediaSequence(long mediaSequence) { this.mediaSequence = mediaSequence; }
        
        public boolean isEndList() { return endList; }
        public void setEndList(boolean endList) { this.endList = endList; }
        
        public List<HlsSegment> getSegments() { return segments; }
        public void setSegments(List<HlsSegment> segments) { this.segments = segments; }
        
        public void addSegment(HlsSegment segment) {
            this.segments.add(segment);
        }
    }
    
    /**
     * HLS片段类
     */
    public static class HlsSegment {
        private String url;
        private double duration;
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public double getDuration() { return duration; }
        public void setDuration(double duration) { this.duration = duration; }
    }
}
