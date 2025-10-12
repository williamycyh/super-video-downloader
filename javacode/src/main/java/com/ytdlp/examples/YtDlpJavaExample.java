package com.ytdlp.examples;

import com.ytdlp.YtDlpJava;
import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.utils.Logger;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * YtDlpJava使用示例
 */
public class YtDlpJavaExample {
    
    public YtDlpJavaExample() {
    }
    
    public static void main(String[] args) {
        System.out.println("=== YtDlpJava 使用示例 ===\n");
        
        // 基本示例
        basicExample();
        
        // 高级示例
        advancedExample();
        
        // 提取信息示例
        extractInfoExample();
        
        // 列出格式示例
        listFormatsExample();
        
        // 带回调的下载示例
        downloadWithCallbackExample();
    }
    
    /**
     * 基本下载示例
     */
    public static void basicExample() {
        System.out.println("--- 基本下载示例 ---");
        
        try {
            YtDlpJava ytdlp = new YtDlpJava();
            String url = "https://example.com/video"; // 替换为实际URL
            
            YtDlpJava.DownloadResult result = ytdlp.download(url);
            
            if (result.isSuccess()) {
                System.out.println("下载成功: " + result.getFilePath());
            } else {
                System.out.println("下载失败: " + result.getErrorMessage());
            }
            
        } catch (Exception e) {
            System.err.println("基本示例执行失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * 高级下载示例
     */
    public static void advancedExample() {
        System.out.println("--- 高级下载示例 ---");
        
        try {
            // 创建带选项的实例
            Map<String, String> options = new HashMap<>();
            options.put("format", "best");
            options.put("output", "%(title)s.%(ext)s");
            options.put("quiet", "false");
            
            YtDlpJava ytdlp = new YtDlpJava(options);
            String url = "https://example.com/video"; // 替换为实际URL
            
            // 设置输出路径
            String outputPath = "/path/to/downloads/";
            YtDlpJava.DownloadResult result = ytdlp.download(url, outputPath);
            
            if (result.isSuccess()) {
                System.out.println("高级下载成功: " + result.getFilePath());
                VideoInfo videoInfo = result.getVideoInfo();
                if (videoInfo != null) {
                    System.out.println("视频标题: " + videoInfo.getTitle());
                    System.out.println("视频时长: " + videoInfo.getDuration() + " 秒");
                }
            } else {
                System.out.println("高级下载失败: " + result.getErrorMessage());
            }
            
        } catch (Exception e) {
            System.err.println("高级示例执行失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * 提取视频信息示例
     */
    public static void extractInfoExample() {
        System.out.println("--- 提取视频信息示例 ---");
        
        try {
            YtDlpJava ytdlp = new YtDlpJava();
            String url = "https://example.com/video"; // 替换为实际URL
            
            VideoInfo videoInfo = ytdlp.extractInfo(url);
            
            if (videoInfo != null) {
                System.out.println("视频信息提取成功:");
                System.out.println("  标题: " + videoInfo.getTitle());
                System.out.println("  上传者: " + videoInfo.getUploader());
                System.out.println("  时长: " + videoInfo.getDuration() + " 秒");
                System.out.println("  观看次数: " + videoInfo.getViewCount());
                System.out.println("  可用格式数量: " + videoInfo.getFormats().size());
                
                // 显示前3个格式
                System.out.println("  格式列表:");
                videoInfo.getFormats().stream()
                    .limit(3)
                    .forEach(format -> System.out.println("    - " + format.getFormatId() + 
                        " (" + format.getExt() + ", " + format.getResolution() + ")"));
            } else {
                System.out.println("无法提取视频信息");
            }
            
        } catch (Exception e) {
            System.err.println("提取信息示例执行失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * 列出可用格式示例
     */
    public static void listFormatsExample() {
        System.out.println("--- 列出可用格式示例 ---");
        
        try {
            YtDlpJava ytdlp = new YtDlpJava();
            String url = "https://example.com/video"; // 替换为实际URL
            
            List<VideoFormat> formats = ytdlp.listFormats(url);
            
            if (!formats.isEmpty()) {
                System.out.println("可用格式列表:");
                for (VideoFormat format : formats) {
                    System.out.println("  格式ID: " + format.getFormatId());
                    System.out.println("    扩展名: " + format.getExt());
                    System.out.println("    分辨率: " + format.getResolution());
                    System.out.println("    质量: " + format.getQuality());
                    System.out.println("    文件大小: " + format.getFilesize());
                    System.out.println("    视频编解码器: " + format.getVcodec());
                    System.out.println("    音频编解码器: " + format.getAcodec());
                    System.out.println("    URL: " + format.getUrl());
                    System.out.println();
                }
            } else {
                System.out.println("未找到可用格式");
            }
            
        } catch (Exception e) {
            System.err.println("列出格式示例执行失败: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * 带进度回调的下载示例
     */
    public static void downloadWithCallbackExample() {
        System.out.println("--- 带进度回调的下载示例 ---");
        
        try {
            YtDlpJava ytdlp = new YtDlpJava();
            String url = "https://example.com/video"; // 替换为实际URL
            
            // 添加进度回调
            ytdlp.addProgressCallback(new YtDlpJava.ProgressCallback() {
                @Override
                public void onProgress(int percentage, long bytesDownloaded, long totalBytes) {
                    System.out.println("下载进度: " + percentage + "% (" + 
                        bytesDownloaded + "/" + totalBytes + " bytes)");
                }
                
                @Override
                public void onComplete(String filePath) {
                    System.out.println("下载完成: " + filePath);
                }
                
                @Override
                public void onError(String error) {
                    System.err.println("下载错误: " + error);
                }
            });
            
            YtDlpJava.DownloadResult result = ytdlp.download(url);
            
            if (result.isSuccess()) {
                System.out.println("带回调的下载成功: " + result.getFilePath());
            } else {
                System.out.println("带回调的下载失败: " + result.getErrorMessage());
            }
            
        } catch (Exception e) {
            System.err.println("回调下载示例执行失败: " + e.getMessage());
        }
        
        System.out.println();
    }
}
