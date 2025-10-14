package com.btdlp.test;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 使用TS下载方案下载Dailymotion视频
 */
public class DailymotionTsDownloadTest {
    
    private static final int THREAD_COUNT = 4;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    
    public static void main(String[] args) {
        System.out.println("=== Dailymotion TS下载测试 ===");
        
        String dailymotionUrl = "https://www.dailymotion.com/video/x9s1tzq";
        
        try {
            System.out.println("目标URL: " + dailymotionUrl);
            
            // 1. 使用Java版本的Dailymotion提取器获取视频信息
            System.out.println("正在提取视频信息...");
            com.btdlp.BtdJava ytdlpJava = new com.btdlp.BtdJava();
            com.btdlp.core.VideoInfo videoInfo = ytdlpJava.extractInfo(dailymotionUrl);
            
            if (videoInfo == null) {
                System.out.println("❌ 无法提取视频信息");
                return;
            }
            
            System.out.println("✅ 视频信息提取成功:");
            System.out.println("标题: " + videoInfo.getTitle());
            System.out.println("找到 " + videoInfo.getFormats().size() + " 个格式");
            
            // 2. 查找HLS格式
            List<com.btdlp.core.VideoFormat> hlsFormats = new ArrayList<>();
            for (com.btdlp.core.VideoFormat format : videoInfo.getFormats()) {
                if ("hls".equals(format.getProtocol()) || format.getUrl().contains(".m3u8")) {
                    hlsFormats.add(format);
                    System.out.println("HLS格式: " + format.getFormatId() + " - " + format.getUrl());
                }
            }
            
            if (hlsFormats.isEmpty()) {
                System.out.println("❌ 没有找到HLS格式");
                return;
            }
            
            // 3. 选择最佳HLS格式
            com.btdlp.core.VideoFormat bestHlsFormat = hlsFormats.get(0);
            System.out.println("选择HLS格式: " + bestHlsFormat.getFormatId());
            
            // 4. 下载HLS播放列表
            String hlsUrl = bestHlsFormat.getUrl();
            System.out.println("HLS URL: " + hlsUrl);
            
            String playlistContent = downloadUrlWithHeaders(hlsUrl);
            System.out.println("播放列表内容预览:");
            System.out.println(playlistContent.substring(0, Math.min(500, playlistContent.length())));
            
            // 5. 检查是否是主播放列表
            if (playlistContent.contains("EXT-X-STREAM-INF")) {
                System.out.println("检测到主播放列表，查找变体播放列表...");
                String variantUrl = parseVariantUrl(playlistContent, hlsUrl);
                if (variantUrl != null) {
                    System.out.println("下载变体播放列表: " + variantUrl);
                    playlistContent = downloadUrlWithHeaders(variantUrl);
                    hlsUrl = variantUrl; // 更新base URL
                }
            }
            
            // 6. 解析视频片段（TS或fMP4）
            List<String> videoUrls = parseVideoUrls(playlistContent, hlsUrl);
            System.out.println("找到 " + videoUrls.size() + " 个视频片段");
            
            if (videoUrls.isEmpty()) {
                System.out.println("❌ 没有找到视频片段");
                return;
            }
            
            // 检查片段类型
            String firstUrl = videoUrls.get(0);
            boolean isTsFormat = firstUrl.contains(".ts");
            boolean isFmp4Format = firstUrl.contains(".m4s");
            String segmentExtension = isTsFormat ? ".ts" : (isFmp4Format ? ".m4s" : ".mp4");
            
            System.out.println("检测到片段格式: " + (isTsFormat ? "TS" : (isFmp4Format ? "fMP4" : "MP4")));
            System.out.println("片段扩展名: " + segmentExtension);
            
            // 7. 创建输出目录
            String safeTitle = sanitizeFileName(videoInfo.getTitle());
            Path outputDir = Paths.get("/tmp", "dailymotion_" + safeTitle);
            Files.createDirectories(outputDir);
            
            // 8. 并行下载所有视频片段
            System.out.println("开始并行下载 " + videoUrls.size() + " 个视频片段...");
            long startTime = System.currentTimeMillis();
            
            List<Future<Boolean>> downloadTasks = new ArrayList<>();
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            
            for (int i = 0; i < videoUrls.size(); i++) {
                final int index = i;
                final String videoUrl = videoUrls.get(i);
                
                Future<Boolean> task = executor.submit(() -> {
                    try {
                        String segmentFile = outputDir.resolve("segment_" + String.format("%03d", index) + segmentExtension).toString();
                        return downloadVideoSegment(videoUrl, segmentFile, index);
                    } catch (Exception e) {
                        System.err.println("❌ 下载片段 " + index + " 失败: " + e.getMessage());
                        return false;
                    }
                });
                downloadTasks.add(task);
            }
            
            // 等待所有下载完成
            int successCount = 0;
            for (Future<Boolean> task : downloadTasks) {
                if (task.get()) {
                    successCount++;
                }
            }
            executor.shutdown();
            
            long downloadTime = System.currentTimeMillis() - startTime;
            System.out.println("✅ 下载完成! 成功: " + successCount + "/" + videoUrls.size() + " 个片段");
            System.out.println("下载耗时: " + (downloadTime / 1000.0) + " 秒");
            
            if (successCount == 0) {
                System.out.println("❌ 没有成功下载任何片段");
                return;
            }
            
            // 9. 合并所有视频片段
            System.out.println("开始合并视频片段...");
            String outputFile = "/tmp/" + safeTitle + (isTsFormat ? ".ts" : ".mp4");
            long mergeStartTime = System.currentTimeMillis();
            
            boolean mergeSuccess = mergeVideoSegments(outputDir, outputFile, successCount, segmentExtension);
            
            if (mergeSuccess) {
                long mergeTime = System.currentTimeMillis() - mergeStartTime;
                System.out.println("✅ 视频文件合并完成!");
                System.out.println("合并耗时: " + (mergeTime / 1000.0) + " 秒");
                System.out.println("输出文件: " + outputFile);
                
                // 检查最终文件
                Path finalFile = Paths.get(outputFile);
                if (Files.exists(finalFile)) {
                    long fileSize = Files.size(finalFile);
                    System.out.println("最终文件大小: " + (fileSize / 1024.0 / 1024.0) + " MB");
                    
                    // 检查文件类型
                    String fileType = getFileType(finalFile.toFile());
                    System.out.println("文件类型: " + fileType);
                    
                    // 检查TS文件头部
                    checkTsFileHeader(finalFile.toFile());
                    
                    System.out.println("🎉 Dailymotion TS视频文件下载完成!");
                    System.out.println("📱 请尝试播放: " + outputFile);
                }
            } else {
                System.out.println("❌ TS文件合并失败");
            }
            
            // 10. 清理临时文件
            System.out.println("清理临时文件...");
            cleanupTempFiles(outputDir);
            
        } catch (Exception e) {
            System.err.println("❌ 处理过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String downloadUrlWithHeaders(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Referer", "https://www.dailymotion.com/");
        connection.setRequestProperty("Accept", "*/*");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        // 不设置Accept-Encoding，让服务器决定是否压缩
        
        try (InputStream in = connection.getInputStream()) {
            byte[] buffer = new byte[4096];
            StringBuilder content = new StringBuilder();
            int len;
            while ((len = in.read(buffer)) != -1) {
                content.append(new String(buffer, 0, len));
            }
            return content.toString();
        }
    }
    
    private static boolean downloadVideoSegment(String videoUrl, String outputPath, int index) throws Exception {
        URL url = new URL(videoUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Referer", "https://www.dailymotion.com/");
        
        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(outputPath)) {
            
            byte[] buffer = new byte[8192];
            int len;
            long totalBytes = 0;
            
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                totalBytes += len;
            }
            
            if (index % 10 == 0) { // 每10个片段显示一次进度
                System.out.println("下载进度: " + index + " 个片段完成, 当前片段大小: " + totalBytes + " bytes");
            }
            
            return totalBytes > 0;
        }
    }
    
    private static boolean mergeVideoSegments(Path tempDir, String outputFile, int segmentCount, String segmentExtension) throws Exception {
        try (FileOutputStream merged = new FileOutputStream(outputFile)) {
            
            System.out.println("合并 " + segmentCount + " 个视频片段...");
            
            for (int i = 0; i < segmentCount; i++) {
                Path segmentFile = tempDir.resolve("segment_" + String.format("%03d", i) + segmentExtension);
                
                if (Files.exists(segmentFile)) {
                    try (InputStream in = Files.newInputStream(segmentFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = in.read(buffer)) != -1) {
                            merged.write(buffer, 0, len);
                        }
                    }
                    
                    if (i % 20 == 0) { // 每20个片段显示一次进度
                        System.out.println("合并进度: " + i + "/" + segmentCount + " 个片段已合并");
                    }
                } else {
                    System.out.println("⚠️  片段文件不存在: " + segmentFile);
                }
            }
            
            System.out.println("✅ 所有视频片段合并完成");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ 合并视频片段时发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static void cleanupTempFiles(Path tempDir) {
        try {
            Files.walk(tempDir)
                .sorted((a, b) -> b.compareTo(a)) // 逆序，先删除文件再删除目录
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        System.err.println("清理文件失败: " + path + " - " + e.getMessage());
                    }
                });
            System.out.println("✅ 临时文件清理完成");
        } catch (Exception e) {
            System.err.println("清理临时文件时发生错误: " + e.getMessage());
        }
    }
    
    private static String parseVariantUrl(String playlistContent, String baseUrl) {
        String[] lines = playlistContent.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("#") && !line.isEmpty() && line.contains(".m3u8")) {
                // 处理相对URL
                String fullUrl = line;
                if (line.startsWith("/")) {
                    try {
                        URL base = new URL(baseUrl);
                        fullUrl = base.getProtocol() + "://" + base.getHost() + line;
                    } catch (Exception e) {
                        fullUrl = line;
                    }
                } else if (!line.startsWith("http")) {
                    try {
                        URL base = new URL(baseUrl);
                        String basePath = base.getPath();
                        if (basePath.contains("/")) {
                            basePath = basePath.substring(0, basePath.lastIndexOf("/") + 1);
                        }
                        fullUrl = base.getProtocol() + "://" + base.getHost() + basePath + line;
                    } catch (Exception e) {
                        fullUrl = line;
                    }
                }
                return fullUrl;
            }
        }
        return null;
    }
    
    private static List<String> parseVideoUrls(String playlistContent, String baseUrl) {
        List<String> tsUrls = new ArrayList<>();
        String[] lines = playlistContent.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("#") && !line.isEmpty()) {
                // 处理相对URL
                String fullUrl = line;
                if (line.startsWith("/")) {
                    try {
                        URL base = new URL(baseUrl);
                        fullUrl = base.getProtocol() + "://" + base.getHost() + line;
                    } catch (Exception e) {
                        fullUrl = line;
                    }
                } else if (!line.startsWith("http")) {
                    try {
                        URL base = new URL(baseUrl);
                        String basePath = base.getPath();
                        if (basePath.contains("/")) {
                            basePath = basePath.substring(0, basePath.lastIndexOf("/") + 1);
                        }
                        fullUrl = base.getProtocol() + "://" + base.getHost() + basePath + line;
                    } catch (Exception e) {
                        fullUrl = line;
                    }
                }
                
                // 添加视频片段文件（.ts, .m4s, .mp4）
                if (fullUrl.contains(".ts") || fullUrl.contains(".m4s") || fullUrl.contains(".mp4")) {
                    tsUrls.add(fullUrl);
                }
            }
        }
        
        return tsUrls;
    }
    
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) return "video";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_")
                      .replaceAll("_{2,}", "_")
                      .replaceAll("^_|_$", "");
    }
    
    private static String getFileType(java.io.File file) {
        try {
            Process process = Runtime.getRuntime().exec("file " + file.getAbsolutePath());
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            return line != null ? line : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    private static void checkTsFileHeader(java.io.File file) {
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            byte[] header = new byte[16];
            fis.read(header);
            fis.close();
            
            System.out.print("文件头部: ");
            for (byte b : header) {
                System.out.printf("%02X ", b);
            }
            System.out.println();
            
            // 检查TS格式的同步字节 (0x47)
            if (header[0] == 0x47) {
                System.out.println("✅ 这是真正的TS文件 (同步字节 0x47)");
            } else {
                System.out.println("❌ 这不是TS文件，同步字节不是 0x47");
            }
            
        } catch (Exception e) {
            System.out.println("❌ 无法读取文件头部: " + e.getMessage());
        }
    }
}
