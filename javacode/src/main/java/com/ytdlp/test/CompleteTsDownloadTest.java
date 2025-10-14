package com.ytdlp.test;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 下载并合并所有TS片段为完整的TS文件
 */
public class CompleteTsDownloadTest {
    
    private static final int THREAD_COUNT = 4; // 并发下载线程数
    
    public static void main(String[] args) {
        System.out.println("=== 完整的TS文件下载和合并测试 ===");
        
        try {
            // 使用公开的HLS流
            String hlsUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8";
            
            System.out.println("HLS URL: " + hlsUrl);
            
            // 下载HLS播放列表
            String playlistContent = downloadUrl(hlsUrl);
            
            // 下载变体播放列表
            String variantUrl = parseVariantUrl(playlistContent, hlsUrl);
            if (variantUrl == null) {
                System.out.println("❌ 无法找到变体播放列表");
                return;
            }
            
            System.out.println("下载变体播放列表: " + variantUrl);
            String variantContent = downloadUrl(variantUrl);
            
            // 解析所有TS URL
            List<String> tsUrls = parseTsUrls(variantContent, variantUrl);
            System.out.println("找到 " + tsUrls.size() + " 个TS片段");
            
            if (tsUrls.isEmpty()) {
                System.out.println("❌ 没有找到TS片段");
                return;
            }
            
            // 创建临时目录
            Path tempDir = Paths.get("/tmp/ts_segments");
            Files.createDirectories(tempDir);
            
            System.out.println("开始并行下载 " + tsUrls.size() + " 个TS片段...");
            
            // 并行下载所有TS片段
            long startTime = System.currentTimeMillis();
            List<Future<Boolean>> downloadTasks = new ArrayList<>();
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            
            for (int i = 0; i < tsUrls.size(); i++) {
                final int index = i;
                final String tsUrl = tsUrls.get(i);
                
                Future<Boolean> task = executor.submit(() -> {
                    try {
                        String segmentFile = tempDir.resolve("segment_" + String.format("%03d", index) + ".ts").toString();
                        return downloadTsSegment(tsUrl, segmentFile, index);
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
            System.out.println("✅ 下载完成! 成功: " + successCount + "/" + tsUrls.size() + " 个片段");
            System.out.println("下载耗时: " + (downloadTime / 1000.0) + " 秒");
            
            if (successCount == 0) {
                System.out.println("❌ 没有成功下载任何片段");
                return;
            }
            
            // 合并所有TS片段
            System.out.println("开始合并TS片段...");
            String outputFile = "/tmp/complete_video.ts";
            long mergeStartTime = System.currentTimeMillis();
            
            boolean mergeSuccess = mergeTsSegments(tempDir, outputFile, successCount);
            
            if (mergeSuccess) {
                long mergeTime = System.currentTimeMillis() - mergeStartTime;
                System.out.println("✅ TS文件合并完成!");
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
                    
                    System.out.println("🎉 完整的TS视频文件已生成!");
                    System.out.println("📱 请尝试播放: " + outputFile);
                }
            } else {
                System.out.println("❌ TS文件合并失败");
            }
            
            // 清理临时文件
            System.out.println("清理临时文件...");
            cleanupTempFiles(tempDir);
            
        } catch (Exception e) {
            System.err.println("❌ 处理过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static boolean downloadTsSegment(String tsUrl, String outputPath, int index) throws Exception {
        URL url = new URL(tsUrl);
        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(outputPath)) {
            
            byte[] buffer = new byte[8192];
            int len;
            long totalBytes = 0;
            
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                totalBytes += len;
            }
            
            if (index % 20 == 0) { // 每20个片段显示一次进度
                System.out.println("下载进度: " + index + " 个片段完成, 当前片段大小: " + totalBytes + " bytes");
            }
            
            return totalBytes > 0;
        }
    }
    
    private static boolean mergeTsSegments(Path tempDir, String outputFile, int segmentCount) throws Exception {
        try (FileOutputStream merged = new FileOutputStream(outputFile)) {
            
            System.out.println("合并 " + segmentCount + " 个TS片段...");
            
            for (int i = 0; i < segmentCount; i++) {
                Path segmentFile = tempDir.resolve("segment_" + String.format("%03d", i) + ".ts");
                
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
            
            System.out.println("✅ 所有TS片段合并完成");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ 合并TS片段时发生错误: " + e.getMessage());
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
    
    private static String downloadUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        try (InputStream in = url.openStream()) {
            byte[] buffer = new byte[4096];
            StringBuilder content = new StringBuilder();
            int len;
            while ((len = in.read(buffer)) != -1) {
                content.append(new String(buffer, 0, len));
            }
            return content.toString();
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
    
    private static List<String> parseTsUrls(String playlistContent, String baseUrl) {
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
                
                // 只添加.ts文件
                if (fullUrl.contains(".ts")) {
                    tsUrls.add(fullUrl);
                }
            }
        }
        
        return tsUrls;
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
