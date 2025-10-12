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
        logger.info("Starting HLS download: " + url);
        
        try {
            // 下载播放列表
            HlsPlaylist playlist = downloadPlaylist(url, format);
            if (playlist == null || playlist.getSegments().isEmpty()) {
                throw new IOException("No segments found in playlist");
            }
            
            logger.info("Found " + playlist.getSegments().size() + " segments");
            
            // 下载所有片段
            boolean success = downloadSegments(playlist, format, outputFile);
            
            if (success) {
                logger.info("HLS download completed: " + outputFile.getAbsolutePath());
            } else {
                logger.error("HLS download failed");
            }
            
            return success;
            
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
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                return parsePlaylist(reader, url);
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
                if (currentSegment != null) {
                    currentSegment.setDuration(parseSegmentDuration(line));
                }
            } else if (line.startsWith("#EXT-X-ENDLIST")) {
                playlist.setEndList(true);
            } else if (!line.startsWith("#") && !line.isEmpty()) {
                // 这是一个片段URL
                currentSegment = new HlsSegment();
                currentSegment.setUrl(resolveUrl(line, baseUrl));
                playlist.addSegment(currentSegment);
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
                final Path segmentFile = tempDir.resolve(String.format("segment_%06d.ts", i));
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
            return mergeSegments(playlist, outputFile);
            
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
        URL urlObj = new URL(segment.getUrl());
        HttpURLConnection connection = createConnection(urlObj, format);
        
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode + " for segment " + index);
            }
            
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
    
    private boolean mergeSegments(HlsPlaylist playlist, File outputFile) throws Exception {
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            for (int i = 0; i < playlist.getSegments().size(); i++) {
                Path segmentFile = Paths.get(System.getProperty("java.io.tmpdir"), 
                                          "hls_download", String.format("segment_%06d.ts", i));
                
                if (Files.exists(segmentFile)) {
                    try (FileInputStream inputStream = new FileInputStream(segmentFile.toFile())) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to merge segments: " + e.getMessage());
            return false;
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
