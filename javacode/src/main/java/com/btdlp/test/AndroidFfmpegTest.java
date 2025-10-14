package com.btdlp.test;

import com.btdlp.BtdJava;
import com.btdlp.core.VideoInfo;
import com.btdlp.core.VideoFormat;
import com.btdlp.utils.Logger;

/**
 * 测试Android版本的FFmpeg集成
 */
public class AndroidFfmpegTest {
    
    public static void main(String[] args) {
        System.out.println("=== Android FFmpeg集成测试 ===");
        
        try {
            // 创建BtdJava实例
            BtdJava ytdlpJava = new BtdJava();
            
            // 测试URL
            String testUrl = "https://www.dailymotion.com/video/x9s1tzq";
            String outputPath = "/tmp/android_ffmpeg_test.mp4";
            
            System.out.println("🎯 测试URL: " + testUrl);
            System.out.println("📱 Android FFmpeg测试");
            System.out.println("💾 输出路径: " + outputPath);
            
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
            
            // 步骤2: 查找HLS格式
            System.out.println("\n🎬 步骤2: 查找HLS格式...");
            VideoFormat hlsFormat = null;
            for (VideoFormat format : videoInfo.getFormats()) {
                System.out.println("   格式: " + format.getFormatId() + 
                                 " (协议: " + format.getProtocol() + 
                                 ", 扩展: " + format.getExt() + ")");
                if ("hls".equals(format.getProtocol()) || format.getUrl().contains(".m3u8")) {
                    hlsFormat = format;
                }
            }
            
            if (hlsFormat == null) {
                System.out.println("❌ 没有找到HLS格式");
                return;
            }
            
            System.out.println("✅ 找到HLS格式: " + hlsFormat.getFormatId());
            
            // 步骤3: 测试Android FFmpeg下载器
            System.out.println("\n⬇️  步骤3: 测试Android FFmpeg下载器...");
            
            // 创建Android FFmpeg下载器
            com.btdlp.downloader.hls.AndroidFfmpegHlsDownloader androidFfmpeg = 
                new com.btdlp.downloader.hls.AndroidFfmpegHlsDownloader();
            
            // 设置日志器
            Logger logger = new Logger(true, true, false);
            androidFfmpeg.setLogger(logger);
            
            // 初始化FFmpegKit
            System.out.println("🔧 初始化FFmpegKit...");
            androidFfmpeg.initialize();
            
            // 检查FFmpeg可用性
            System.out.println("🔍 检查FFmpeg可用性...");
            boolean isAvailable = androidFfmpeg.isAvailable();
            System.out.println("   FFmpeg可用性: " + (isAvailable ? "✅ 可用" : "❌ 不可用"));
            
            if (isAvailable) {
                // 执行下载
                System.out.println("🚀 开始下载...");
                boolean success = androidFfmpeg.download(videoInfo, hlsFormat, outputPath);
                
                if (success) {
                    System.out.println("✅ Android FFmpeg下载成功!");
                    
                    // 验证文件
                    java.io.File file = new java.io.File(outputPath);
                    if (file.exists()) {
                        long fileSize = file.length();
                        System.out.println("   文件大小: " + (fileSize / 1024.0 / 1024.0) + " MB");
                        
                        String fileType = getFileType(file);
                        System.out.println("   文件类型: " + fileType);
                        
                        System.out.println("\n🎉 Android FFmpeg测试完成!");
                        System.out.println("📱 请在Android设备上测试: " + outputPath);
                        
                        System.out.println("\n✅ 测试结果总结:");
                        System.out.println("   - 视频信息提取: ✅");
                        System.out.println("   - FFmpegKit初始化: ✅");
                        System.out.println("   - FFmpeg可用性检查: ✅");
                        System.out.println("   - 视频下载: ✅");
                        
                    } else {
                        System.out.println("❌ 下载文件不存在");
                    }
                } else {
                    System.out.println("❌ Android FFmpeg下载失败");
                    
                    // 测试回退机制
                    System.out.println("\n🔄 测试回退机制...");
                    BtdJava.DownloadResult fallbackResult = ytdlpJava.download(testUrl, outputPath + ".fallback");
                    
                    if (fallbackResult.isSuccess()) {
                        System.out.println("✅ 回退方案成功: " + fallbackResult.getFilePath());
                    } else {
                        System.out.println("❌ 回退方案也失败了");
                    }
                }
            } else {
                System.out.println("❌ FFmpeg不可用，无法进行下载测试");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Android FFmpeg测试失败: " + e.getMessage());
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
