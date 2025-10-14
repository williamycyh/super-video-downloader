package com.btdlp.test;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 测试下载单个TS片段
 */
public class SingleSegmentTest {
    
    public static void main(String[] args) {
        System.out.println("=== 单个TS片段下载测试 ===");
        
        try {
            // 使用一个已知的TS片段URL（从之前的日志中获取）
            String tsUrl = "https://vod3.cf.dmcdn.net/sec2(NnY_H53En1EpPZ5Xm0UPAsj6HtdSn5EwnxGDCXUQB9VeQ6Qx7kGnbjLvbitaWzCUGvrGbrq3RnpjOJDq9LLs3nHwOVD1rrF3ZJ1HeNGlSzxfhBULmhZZ7WYExYdZ2FLC_hx-BUmRgtZs_On-IQRNpc65H0Qu5Y8oEoGohfAqaQ3jXPrahOm__d__RRqDvr-A)/video/fmp4/591310358/h264_aac_fhd/2/0.m4s";
            String outputPath = "/tmp/single_segment_test.m4s";
            
            System.out.println("下载URL: " + tsUrl);
            System.out.println("输出路径: " + outputPath);
            
            // 按照您的指导下载单个片段
            URL url = new URL(tsUrl);
            try (InputStream in = url.openStream();
                 FileOutputStream out = new FileOutputStream(outputPath)) {
                
                byte[] buffer = new byte[4096];
                int len;
                long totalBytes = 0;
                
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    totalBytes += len;
                }
                
                System.out.println("✅ 下载完成!");
                System.out.println("总下载字节数: " + totalBytes);
                
                // 检查文件
                Path file = Paths.get(outputPath);
                if (Files.exists(file)) {
                    long fileSize = Files.size(file);
                    System.out.println("文件大小: " + fileSize + " bytes");
                    System.out.println("文件类型: " + getFileType(file.toFile()));
                    
                    // 检查文件头部
                    checkFileHeader(file.toFile());
                    
                    System.out.println("📱 请尝试播放这个单个片段文件: " + outputPath);
                    System.out.println("💡 如果这个片段能播放，说明问题在合并过程");
                    System.out.println("💡 如果这个片段不能播放，说明问题在片段本身");
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 下载失败: " + e.getMessage());
            e.printStackTrace();
        }
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
    
    private static void checkFileHeader(java.io.File file) {
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            byte[] header = new byte[32];
            fis.read(header);
            fis.close();
            
            System.out.print("文件头部: ");
            for (byte b : header) {
                System.out.printf("%02X ", b);
            }
            System.out.println();
            
            // 检查是否为MP4/fMP4格式
            if (header.length >= 8) {
                int size = ((header[0] & 0xFF) << 24) | 
                          ((header[1] & 0xFF) << 16) | 
                          ((header[2] & 0xFF) << 8) | 
                          (header[3] & 0xFF);
                String type = new String(header, 4, 4);
                
                System.out.println("第一个原子: size=" + size + ", type=" + type);
                
                if ("styp".equals(type)) {
                    System.out.println("✅ 这是fMP4片段文件");
                } else if ("ftyp".equals(type)) {
                    System.out.println("✅ 这是MP4文件");
                } else if ("mdat".equals(type)) {
                    System.out.println("✅ 这是媒体数据片段");
                } else {
                    System.out.println("❓ 未知原子类型: " + type);
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ 无法读取文件头部: " + e.getMessage());
        }
    }
}
