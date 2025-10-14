package com.btdlp.test;

import com.btdlp.BtdJava;
import com.btdlp.core.VideoInfo;
import com.btdlp.core.VideoFormat;
import com.btdlp.utils.Logger;

/**
 * 测试纯Java下载器（不使用FFmpeg）
 */
public class PureJavaDownloadTest {
    
    public static void main(String[] args) {
        System.out.println("=== 纯Java下载器测试 ===");
        
        try {
            // 创建BtdJava实例
            BtdJava ytdlpJava = new BtdJava();
            
            // 测试URL
            String testUrl = "https://www.dailymotion.com/video/x9s1tzq";
            String outputPath = "/tmp/pure_java_test.mp4";
            
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
            
            // 临时修改为强制使用纯Java下载器
            System.out.println("强制使用纯Java HLS下载器...");
            
            // 使用纯Java下载器
            com.btdlp.downloader.hls.HlsDownloader hlsDownloader = 
                new com.btdlp.downloader.hls.HlsDownloader();
            
            // 初始化下载器
            Logger logger = new Logger(true, true, false);
            hlsDownloader.initialize(null, logger);
            
            // 执行下载
            java.io.File outputFile = new java.io.File(outputPath);
            boolean success = hlsDownloader.download(videoInfo, hlsFormat, outputFile.getAbsolutePath());
            
            if (success) {
                System.out.println("✅ 纯Java下载成功!");
                
                // 检查文件
                if (outputFile.exists()) {
                    long fileSize = outputFile.length();
                    System.out.println("文件大小: " + (fileSize / 1024.0 / 1024.0) + " MB");
                    
                    // 检查文件类型
                    String fileType = getFileType(outputFile);
                    System.out.println("文件类型: " + fileType);
                    
                    System.out.println("📱 请尝试播放: " + outputPath);
                }
            } else {
                System.out.println("❌ 纯Java下载失败");
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
