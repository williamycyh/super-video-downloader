package com.btdlp.test;

import com.btdlp.BtdJava;
import com.btdlp.core.VideoInfo;
import com.btdlp.core.VideoFormat;
import com.btdlp.utils.Logger;

/**
 * 测试Android版本的完整功能
 * 模拟Android环境中的使用
 */
public class AndroidVersionTest {
    
    public static void main(String[] args) {
        System.out.println("=== Android版本完整功能测试 ===");
        
        try {
            // 模拟Android环境中的BtdJava使用
            BtdJava ytdlpJava = new BtdJava();
            
            // 测试URL
            String testUrl = "https://www.dailymotion.com/video/x9s1tzq";
            String outputPath = "/tmp/android_test.mp4";
            
            System.out.println("🎯 测试URL: " + testUrl);
            System.out.println("📱 模拟Android环境");
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
            
            // 步骤2: 选择最佳格式
            System.out.println("\n🎬 步骤2: 选择最佳格式...");
            VideoFormat bestFormat = null;
            for (VideoFormat format : videoInfo.getFormats()) {
                System.out.println("   格式: " + format.getFormatId() + 
                                 " (协议: " + format.getProtocol() + 
                                 ", 扩展: " + format.getExt() + ")");
                if (bestFormat == null) {
                    bestFormat = format;
                }
            }
            
            if (bestFormat == null) {
                System.out.println("❌ 没有找到可用格式");
                return;
            }
            
            System.out.println("✅ 选择格式: " + bestFormat.getFormatId());
            
            // 步骤3: 下载视频
            System.out.println("\n⬇️  步骤3: 下载视频...");
            System.out.println("   协议: " + bestFormat.getProtocol());
            System.out.println("   下载器: " + (bestFormat.getProtocol().equals("hls") ? "FFmpeg (Android)" : "HTTP"));
            
            BtdJava.DownloadResult result = ytdlpJava.downloadFormat(bestFormat, outputPath);
            
            if (result.isSuccess()) {
                System.out.println("✅ 下载成功!");
                System.out.println("   文件路径: " + result.getFilePath());
                
                // 步骤4: 验证文件
                System.out.println("\n🔍 步骤4: 验证文件...");
                java.io.File file = new java.io.File(result.getFilePath());
                if (file.exists()) {
                    long fileSize = file.length();
                    System.out.println("   文件大小: " + (fileSize / 1024.0 / 1024.0) + " MB");
                    
                    // 检查文件类型
                    String fileType = getFileType(file);
                    System.out.println("   文件类型: " + fileType);
                    
                    // 检查是否可播放
                    boolean isPlayable = isPlayableFile(fileType);
                    System.out.println("   可播放性: " + (isPlayable ? "✅ 可播放" : "❌ 不可播放"));
                    
                    System.out.println("\n🎉 Android版本测试完成!");
                    System.out.println("📱 请在Android设备上播放: " + result.getFilePath());
                    
                    if (isPlayable) {
                        System.out.println("\n✅ 测试结果: Android版本工作正常!");
                        System.out.println("   - 视频信息提取: ✅");
                        System.out.println("   - 格式选择: ✅");
                        System.out.println("   - 视频下载: ✅");
                        System.out.println("   - 文件可播放: ✅");
                    } else {
                        System.out.println("\n⚠️  测试结果: 文件格式需要进一步优化");
                        System.out.println("   建议: 在Android中使用FFmpegKit进行后处理");
                    }
                }
            } else {
                System.out.println("❌ 下载失败: " + result.getErrorMessage());
                
                // 如果FFmpeg失败，尝试纯Java方案
                System.out.println("\n🔄 尝试纯Java方案...");
                BtdJava.DownloadResult javaResult = ytdlpJava.download(testUrl, outputPath + ".java");
                
                if (javaResult.isSuccess()) {
                    System.out.println("✅ 纯Java方案成功!");
                    System.out.println("   文件路径: " + javaResult.getFilePath());
                    
                    java.io.File javaFile = new java.io.File(javaResult.getFilePath());
                    if (javaFile.exists()) {
                        long fileSize = javaFile.length();
                        System.out.println("   文件大小: " + (fileSize / 1024.0 / 1024.0) + " MB");
                        
                        String fileType = getFileType(javaFile);
                        System.out.println("   文件类型: " + fileType);
                        
                        System.out.println("\n📝 总结:");
                        System.out.println("   - FFmpeg方案: ❌ 失败");
                        System.out.println("   - 纯Java方案: ✅ 成功");
                        System.out.println("   - 建议: 在Android中使用纯Java方案作为备选");
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Android版本测试失败: " + e.getMessage());
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
    
    private static boolean isPlayableFile(String fileType) {
        if (fileType == null) return false;
        
        String lowerType = fileType.toLowerCase();
        return lowerType.contains("mp4") || 
               lowerType.contains("iso media") ||
               lowerType.contains("mpeg") ||
               lowerType.contains("quicktime");
    }
}
