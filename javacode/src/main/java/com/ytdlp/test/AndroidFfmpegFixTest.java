package com.ytdlp.test;

import com.ytdlp.YtDlpJava;
import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.utils.Logger;

/**
 * 测试Android FFmpeg修复
 */
public class AndroidFfmpegFixTest {
    
    public static void main(String[] args) {
        System.out.println("=== Android FFmpeg修复测试 ===");
        
        try {
            // 创建YtDlpJava实例
            YtDlpJava ytdlpJava = new YtDlpJava();
            
            // 测试M3U8 URL（类似Android日志中的URL）
            String testUrl = "https://www.dailymotion.com/video/x9s1tzq";
            String outputPath = "/tmp/android_ffmpeg_fix_test.mp4";
            
            System.out.println("🎯 测试URL: " + testUrl);
            System.out.println("📱 输出路径: " + outputPath);
            
            // 步骤1: 提取视频信息
            System.out.println("\n📋 步骤1: 提取视频信息...");
            VideoInfo videoInfo = ytdlpJava.extractInfo(testUrl);
            
            if (videoInfo == null) {
                System.out.println("❌ 无法提取视频信息");
                return;
            }
            
            System.out.println("✅ 视频信息提取成功:");
            System.out.println("   标题: " + videoInfo.getTitle());
            System.out.println("   格式数量: " + videoInfo.getFormats().size());
            
            // 步骤2: 分析格式
            System.out.println("\n🔍 步骤2: 分析格式...");
            for (VideoFormat format : videoInfo.getFormats()) {
                System.out.println("   格式: " + format.getFormatId());
                System.out.println("   协议: " + format.getProtocol());
                System.out.println("   扩展: " + format.getExt());
                System.out.println("   URL: " + format.getUrl());
                
                // 测试流媒体格式检测
                boolean isStreaming = testStreamingFormatDetection(format);
                System.out.println("   流媒体格式: " + (isStreaming ? "✅ 是" : "❌ 否"));
            }
            
            // 步骤3: 测试下载
            System.out.println("\n⬇️  步骤3: 测试下载...");
            YtDlpJava.DownloadResult result = ytdlpJava.download(testUrl, outputPath);
            
            if (result.isSuccess()) {
                System.out.println("✅ 下载成功!");
                System.out.println("   文件路径: " + result.getFilePath());
                
                // 验证文件
                java.io.File file = new java.io.File(result.getFilePath());
                if (file.exists()) {
                    long fileSize = file.length();
                    System.out.println("   文件大小: " + (fileSize / 1024.0 / 1024.0) + " MB");
                    
                    String fileType = getFileType(file);
                    System.out.println("   文件类型: " + fileType);
                    
                    System.out.println("\n🎉 Android FFmpeg修复测试完成!");
                    System.out.println("📱 修复结果: 成功");
                }
            } else {
                System.out.println("❌ 下载失败: " + result.getErrorMessage());
                
                // 尝试使用纯Java方案
                System.out.println("\n🔄 尝试纯Java方案...");
                YtDlpJava.DownloadResult javaResult = ytdlpJava.download(testUrl, outputPath + ".java");
                
                if (javaResult.isSuccess()) {
                    System.out.println("✅ 纯Java方案成功: " + javaResult.getFilePath());
                } else {
                    System.out.println("❌ 纯Java方案也失败: " + javaResult.getErrorMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试流媒体格式检测
     */
    private static boolean testStreamingFormatDetection(VideoFormat format) {
        String protocol = format.getProtocol();
        String url = format.getUrl();
        
        // 检查协议
        if ("hls".equals(protocol) || "m3u8".equals(protocol) || 
            "dash".equals(protocol) || "mpd".equals(protocol) ||
            "mms".equals(protocol) || "rtmp".equals(protocol) ||
            "rtsp".equals(protocol) || "websocket".equals(protocol)) {
            return true;
        }
        
        // 检查URL中的流媒体标识
        if (url != null) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains(".m3u8") || 
                lowerUrl.contains(".mpd") || 
                lowerUrl.contains(".txt") ||
                lowerUrl.contains("manifest") ||
                lowerUrl.contains("playlist") ||
                lowerUrl.contains("stream") ||
                lowerUrl.contains("live")) {
                return true;
            }
        }
        
        return false;
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
