package com.btdlp.test;

import com.btdlp.BtdJava;
import com.btdlp.core.VideoInfo;
import com.btdlp.core.VideoFormat;
import com.btdlp.utils.Logger;

/**
 * 测试扩展的流媒体格式支持
 * 参考Android工程的判断逻辑
 */
public class StreamingFormatTest {
    
    public static void main(String[] args) {
        System.out.println("=== 流媒体格式支持测试 ===");
        
        try {
            // 创建BtdJava实例
            BtdJava ytdlpJava = new BtdJava();
            
            // 测试不同的流媒体格式
            String[] testUrls = {
                "https://www.dailymotion.com/video/x9s1tzq",  // HLS
                "https://vimeo.com/1064681837",               // 可能包含MPD
                "https://www.facebook.com/crochetbydrachi/videos/434555017883634/"  // 可能包含M3U8
            };
            
            for (String testUrl : testUrls) {
                System.out.println("\n🎯 测试URL: " + testUrl);
                
                // 提取视频信息
                System.out.println("📋 提取视频信息...");
                VideoInfo videoInfo = ytdlpJava.extractInfo(testUrl);
                
                if (videoInfo != null) {
                    System.out.println("✅ 视频信息提取成功:");
                    System.out.println("   标题: " + videoInfo.getTitle());
                    System.out.println("   格式数量: " + videoInfo.getFormats().size());
                    
                    // 分析每个格式
                    for (VideoFormat format : videoInfo.getFormats()) {
                        System.out.println("\n🔍 分析格式: " + format.getFormatId());
                        System.out.println("   协议: " + format.getProtocol());
                        System.out.println("   扩展: " + format.getExt());
                        System.out.println("   URL: " + format.getUrl());
                        
                        // 测试流媒体格式检测
                        boolean isStreaming = testStreamingFormatDetection(format);
                        System.out.println("   流媒体格式: " + (isStreaming ? "✅ 是" : "❌ 否"));
                        
                        if (isStreaming) {
                            System.out.println("   📱 将使用FFmpeg下载器");
                        } else {
                            System.out.println("   🌐 将使用HTTP下载器");
                        }
                    }
                    
                } else {
                    System.out.println("❌ 无法提取视频信息");
                }
            }
            
            // 测试各种流媒体URL格式
            System.out.println("\n🧪 测试流媒体URL格式检测...");
            testStreamingUrlDetection();
            
            System.out.println("\n🎉 流媒体格式支持测试完成!");
            
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
    
    /**
     * 测试各种流媒体URL格式检测
     */
    private static void testStreamingUrlDetection() {
        String[] testUrls = {
            "https://example.com/video.m3u8",
            "https://example.com/video.mpd",
            "https://example.com/manifest.txt",
            "https://example.com/playlist.m3u8",
            "https://example.com/stream.mpd",
            "https://example.com/live.m3u8",
            "https://example.com/video.mp4",
            "https://example.com/video.avi"
        };
        
        for (String url : testUrls) {
            boolean isStreaming = testStreamingUrl(url);
            String status = isStreaming ? "✅ 流媒体" : "❌ 普通文件";
            System.out.println("   " + url + " -> " + status);
        }
    }
    
    /**
     * 测试单个URL是否为流媒体
     */
    private static boolean testStreamingUrl(String url) {
        if (url == null) return false;
        
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains(".m3u8") || 
               lowerUrl.contains(".mpd") || 
               lowerUrl.contains(".txt") ||
               lowerUrl.contains("manifest") ||
               lowerUrl.contains("playlist") ||
               lowerUrl.contains("stream") ||
               lowerUrl.contains("live");
    }
}
