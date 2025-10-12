package com.ytdlp.downloader;

import com.ytdlp.utils.Logger;
import com.ytdlp.utils.EnhancedHttpClient;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.*;

/**
 * HLS (HTTP Live Streaming) 下载器
 */
public class HLSDownloader {
    
    private static final Logger logger = new Logger(true, false, false);
    private final EnhancedHttpClient httpClient;
    
    static {
        // 静态初始化
    }
    
    public HLSDownloader() {
        this.httpClient = new EnhancedHttpClient();
    }
    
    /**
     * 下载HLS流
     * @param playlistUrl HLS播放列表URL
     * @param outputPath 输出文件路径
     * @return 是否下载成功
     */
    public boolean downloadHLS(String playlistUrl, String outputPath) {
        try {
            logger.info("开始下载HLS流: " + playlistUrl);
            
            // 1. 下载播放列表
            String playlistContent = downloadPlaylist(playlistUrl);
            if (playlistContent == null || playlistContent.isEmpty()) {
                logger.error("无法下载播放列表");
                return false;
            }
            
            // 2. 解析播放列表
            List<SegmentInfo> segments = parsePlaylist(playlistContent, playlistUrl);
            if (segments.isEmpty()) {
                logger.error("播放列表中没有找到视频片段");
                return false;
            }
            
            logger.info("找到 " + segments.size() + " 个视频片段");
            
            // 3. 下载所有片段
            List<File> segmentFiles = downloadSegments(segments);
            if (segmentFiles.isEmpty()) {
                logger.error("没有成功下载任何片段");
                return false;
            }
            
            // 4. 合并片段
            boolean mergeSuccess = mergeSegments(segmentFiles, outputPath);
            
            // 5. 清理临时文件
            cleanupTempFiles(segmentFiles);
            
            if (mergeSuccess) {
                logger.info("HLS下载完成: " + outputPath);
                return true;
            } else {
                logger.error("片段合并失败");
                return false;
            }
            
        } catch (Exception e) {
            logger.error("HLS下载失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 下载播放列表
     */
    private String downloadPlaylist(String playlistUrl) {
        try {
            logger.debug("下载播放列表: " + playlistUrl);
            EnhancedHttpClient.HttpResponse response = httpClient.get(playlistUrl);
            return response.getContent();
        } catch (Exception e) {
            logger.error("下载播放列表失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析播放列表
     */
    private List<SegmentInfo> parsePlaylist(String playlistContent, String baseUrl) {
        List<SegmentInfo> segments = new ArrayList<>();
        
        // 检查是否是主播放列表
        if (playlistContent.contains("#EXT-X-STREAM-INF")) {
            return parseMasterPlaylist(playlistContent, baseUrl);
        }
        
        // 解析媒体播放列表
        String[] lines = playlistContent.split("\n");
        String basePath = getBasePath(baseUrl);
        
        for (String line : lines) {
            line = line.trim();
            
            // 跳过注释和空行
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // 如果是URL
            if (line.startsWith("http://") || line.startsWith("https://")) {
                segments.add(new SegmentInfo(line));
            } else if (!line.isEmpty()) {
                // 相对URL，需要拼接基础路径
                String fullUrl = basePath + line;
                segments.add(new SegmentInfo(fullUrl));
            }
        }
        
        return segments;
    }
    
    /**
     * 解析主播放列表
     */
    private List<SegmentInfo> parseMasterPlaylist(String playlistContent, String baseUrl) {
        List<SegmentInfo> segments = new ArrayList<>();
        
        String[] lines = playlistContent.split("\n");
        String basePath = getBasePath(baseUrl);
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                // 下一行应该是URL
                if (i + 1 < lines.length) {
                    String urlLine = lines[i + 1].trim();
                    if (!urlLine.startsWith("#") && !urlLine.isEmpty()) {
                        String fullUrl;
                        if (urlLine.startsWith("http://") || urlLine.startsWith("https://")) {
                            fullUrl = urlLine;
                        } else {
                            fullUrl = basePath + urlLine;
                        }
                        
                        // 递归解析子播放列表
                        String subPlaylistContent = downloadPlaylist(fullUrl);
                        if (subPlaylistContent != null) {
                            segments.addAll(parsePlaylist(subPlaylistContent, fullUrl));
                        }
                    }
                }
            }
        }
        
        return segments;
    }
    
    /**
     * 从EXT-X-STREAM-INF行中提取带宽信息
     */
    private int extractBandwidth(String line) {
        Pattern pattern = Pattern.compile("BANDWIDTH=(\\d+)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * 获取URL的基础路径
     */
    private String getBasePath(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash != -1) {
            return url.substring(0, lastSlash + 1);
        }
        return url + "/";
    }
    
    /**
     * 下载所有片段
     */
    private List<File> downloadSegments(List<SegmentInfo> segments) {
        List<File> downloadedFiles = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(5); // 5个并发线程
        
        try {
            List<Future<File>> futures = new ArrayList<>();
            
            for (int i = 0; i < segments.size(); i++) {
                final int index = i;
                final SegmentInfo segment = segments.get(i);
                
                Future<File> future = executor.submit(() -> {
                    try {
                        logger.debug("下载片段 " + (index + 1) + "/" + segments.size() + ": " + segment.url);
                        
                        // 创建临时文件
                        File tempFile = File.createTempFile("hls_segment_" + index, ".ts");
                        tempFile.deleteOnExit();
                        
                        // 下载片段
                        byte[] data = httpClient.getBinary(segment.url);
                        if (data != null && data.length > 0) {
                            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                                fos.write(data);
                            }
                            logger.debug("片段下载成功: " + tempFile.getName());
                            return tempFile;
                        } else {
                            logger.warning("片段下载失败，数据为空: " + segment.url);
                            tempFile.delete();
                            return null;
                        }
                        
                    } catch (Exception e) {
                        logger.error("下载片段失败: " + segment.url + " - " + e.getMessage());
                        return null;
                    }
                });
                
                futures.add(future);
            }
            
            // 收集结果
            for (Future<File> future : futures) {
                try {
                    File file = future.get(30, TimeUnit.SECONDS); // 30秒超时
                    if (file != null && file.exists()) {
                        downloadedFiles.add(file);
                    }
                } catch (TimeoutException e) {
                    logger.error("下载片段超时");
                } catch (Exception e) {
                    logger.error("获取下载结果失败: " + e.getMessage());
                }
            }
            
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        logger.info("成功下载 " + downloadedFiles.size() + "/" + segments.size() + " 个片段");
        return downloadedFiles;
    }
    
    /**
     * 合并片段
     */
    private boolean mergeSegments(List<File> segmentFiles, String outputPath) {
        try {
            logger.info("开始合并 " + segmentFiles.size() + " 个片段");
            
            File outputFile = new File(outputPath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (FileOutputStream fos = new FileOutputStream(outputFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                
                for (File segmentFile : segmentFiles) {
                    if (segmentFile.exists()) {
                        try (FileInputStream fis = new FileInputStream(segmentFile);
                             BufferedInputStream bis = new BufferedInputStream(fis)) {
                            
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = bis.read(buffer)) != -1) {
                                bos.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                }
                
                bos.flush();
            }
            
            logger.info("片段合并完成: " + outputPath);
            return outputFile.exists() && outputFile.length() > 0;
            
        } catch (Exception e) {
            logger.error("合并片段失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 清理临时文件
     */
    private void cleanupTempFiles(List<File> tempFiles) {
        for (File file : tempFiles) {
            try {
                if (file.exists()) {
                    file.delete();
                }
            } catch (Exception e) {
                logger.debug("清理临时文件失败: " + file.getName() + " - " + e.getMessage());
            }
        }
        logger.debug("临时文件清理完成");
    }
    
    /**
     * 片段信息类
     */
    public static class SegmentInfo {
        public final String url;
        
        public SegmentInfo(String url) {
            this.url = url;
        }
    }
}
