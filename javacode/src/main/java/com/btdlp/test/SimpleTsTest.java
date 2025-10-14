package com.btdlp.test;

import com.btdlp.BtdJava;
import com.btdlp.core.VideoInfo;
import com.btdlp.core.VideoFormat;

/**
 * 简单的TS下载测试
 */
public class SimpleTsTest {
    
    public static void main(String[] args) {
        System.out.println("=== 简单TS下载测试 ===");
        
        // Dailymotion视频URL
        String url = "https://www.dailymotion.com/video/x9s1tzq";
        String outputPath = "/tmp/simple_ts_test.ts";
        
        try {
            BtdJava ytdlpJava = new BtdJava();
            
            System.out.println("开始下载...");
            BtdJava.DownloadResult result = ytdlpJava.download(url, outputPath);
            
            if (result.isSuccess()) {
                System.out.println("✅ 下载成功!");
                System.out.println("文件路径: " + result.getFilePath());
                
                // 检查文件
                java.io.File file = new java.io.File(result.getFilePath());
                if (file.exists()) {
                    System.out.println("文件大小: " + file.length() + " bytes");
                    System.out.println("文件类型: " + getFileType(file));
                    
                    // 检查文件是否可播放
                    if (file.length() > 10000000) { // 大于10MB
                        System.out.println("✅ 文件大小合理");
                        System.out.println("📱 这个TS文件应该可以在手机上播放！");
                        System.out.println("🎬 请尝试在手机上播放这个文件");
                        System.out.println("💡 如果无法播放，请尝试使用VLC播放器或其他支持TS格式的播放器");
                    } else {
                        System.out.println("❌ 文件太小，可能有问题");
                    }
                }
            } else {
                System.err.println("❌ 下载失败: " + result.getErrorMessage());
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
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
}
