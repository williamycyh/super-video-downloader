package com.btdlp.test;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试下载真正的TS文件
 */
public class RealTsTest {
    
    public static void main(String[] args) {
        System.out.println("=== 真正的TS文件下载测试 ===");
        
        try {
            // 使用一个公开的HLS流，这个流提供真正的TS格式
            String hlsUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8";
            
            System.out.println("HLS URL: " + hlsUrl);
            
            // 下载HLS播放列表
            String playlistContent = downloadUrl(hlsUrl);
            System.out.println("播放列表内容:");
            System.out.println(playlistContent);
            
            // 解析TS URL
            List<String> tsUrls = parseTsUrls(playlistContent, hlsUrl);
            System.out.println("找到 " + tsUrls.size() + " 个TS片段");
            
            if (tsUrls.isEmpty()) {
                System.out.println("❌ 没有找到TS片段URL，尝试下载变体播放列表");
                
                // 下载第一个变体播放列表
                String variantUrl = parseVariantUrl(playlistContent, hlsUrl);
                if (variantUrl != null) {
                    System.out.println("下载变体播放列表: " + variantUrl);
                    String variantContent = downloadUrl(variantUrl);
                    System.out.println("变体播放列表内容:");
                    System.out.println(variantContent);
                    
                    tsUrls = parseTsUrls(variantContent, variantUrl);
                    System.out.println("从变体播放列表找到 " + tsUrls.size() + " 个TS片段");
                }
            }
            
            if (tsUrls.isEmpty()) {
                System.out.println("❌ 仍然没有找到TS片段URL");
                return;
            }
            
            // 下载第一个TS片段测试
            String firstTsUrl = tsUrls.get(0);
            String outputPath = "/tmp/real_ts_test.ts";
            
            System.out.println("下载第一个TS片段: " + firstTsUrl);
            
            // 按照您的指导下载TS片段
            URL url = new URL(firstTsUrl);
            try (InputStream in = url.openStream();
                 FileOutputStream out = new FileOutputStream(outputPath)) {
                
                byte[] buffer = new byte[4096];
                int len;
                long totalBytes = 0;
                
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    totalBytes += len;
                }
                
                System.out.println("✅ TS片段下载完成!");
                System.out.println("总下载字节数: " + totalBytes);
                
                // 检查文件
                Path file = Paths.get(outputPath);
                if (Files.exists(file)) {
                    long fileSize = Files.size(file);
                    System.out.println("文件大小: " + fileSize + " bytes");
                    System.out.println("文件类型: " + getFileType(file.toFile()));
                    
                    // 检查文件头部
                    checkTsFileHeader(file.toFile());
                    
                    System.out.println("📱 请尝试播放这个真正的TS文件: " + outputPath);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 下载失败: " + e.getMessage());
            e.printStackTrace();
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
                    // 绝对路径
                    try {
                        URL base = new URL(baseUrl);
                        fullUrl = base.getProtocol() + "://" + base.getHost() + line;
                    } catch (Exception e) {
                        fullUrl = line;
                    }
                } else if (!line.startsWith("http")) {
                    // 相对路径
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
                    // 绝对路径
                    try {
                        URL base = new URL(baseUrl);
                        fullUrl = base.getProtocol() + "://" + base.getHost() + line;
                    } catch (Exception e) {
                        fullUrl = line;
                    }
                } else if (!line.startsWith("http")) {
                    // 相对路径
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
