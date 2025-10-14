package com.ytdlp.test;

import com.ytdlp.YtDlpJava;
import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.utils.Logger;

/**
 * 测试FFmpeg下载器
 */
public class FfmpegDownloadTest {
    
    public static void main(String[] args) {
        System.out.println("=== FFmpeg下载器测试 ===");
        
        try {
            // 创建YtDlpJava实例
            YtDlpJava ytdlpJava = new YtDlpJava();
            
            // 测试URL
            String testUrl = "https://www.dailymotion.com/video/x9s1tzq";
            String outputPath = "/tmp/ffmpeg_test.mp4";
            
            System.out.println("测试URL: " + testUrl);
            System.out.println("输出路径: " + outputPath);
            
            // 提取视频信息
            System.out.println("正在提取视频信息...");
            VideoInfo videoInfo = ytdlpJava.extractInfo(testUrl);
            
            if (videoInfo == null) {
                System.out.println("❌ 无法提取视频信息");
                return;
            }
            
            System.out.println("✅ 视频信息提取成功:");
            System.out.println("标题: " + videoInfo.getTitle());
            System.out.println("找到 " + videoInfo.getFormats().size() + " 个格式");
            
            // 查找HLS格式
            VideoFormat hlsFormat = null;
            for (VideoFormat format : videoInfo.getFormats()) {
                if ("hls".equals(format.getProtocol()) || format.getUrl().contains(".m3u8")) {
                    hlsFormat = format;
                    System.out.println("HLS格式: " + format.getFormatId() + " - " + format.getUrl());
                    break;
                }
            }
            
            if (hlsFormat == null) {
                System.out.println("❌ 没有找到HLS格式");
                return;
            }
            
            // 使用FFmpeg下载
            System.out.println("开始使用FFmpeg下载...");
            YtDlpJava.DownloadResult result = ytdlpJava.downloadFormat(hlsFormat, outputPath);
            
            if (result.isSuccess()) {
                System.out.println("✅ FFmpeg下载成功!");
                System.out.println("文件路径: " + result.getFilePath());
                
                // 检查文件
                java.io.File file = new java.io.File(result.getFilePath());
                if (file.exists()) {
                    long fileSize = file.length();
                    System.out.println("文件大小: " + (fileSize / 1024.0 / 1024.0) + " MB");
                    
                    // 检查文件类型
                    String fileType = getFileType(file);
                    System.out.println("文件类型: " + fileType);
                    
                    System.out.println("📱 请尝试播放: " + result.getFilePath());
                }
            } else {
                System.out.println("❌ FFmpeg下载失败: " + result.getErrorMessage());
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试过程中发生错误: " + e.getMessage());
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
