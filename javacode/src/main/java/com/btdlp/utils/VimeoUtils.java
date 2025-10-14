package com.btdlp.utils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Vimeo工具类
 */
public class VimeoUtils {
    
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("/videos/(\\d+)");
    private static final Pattern EMBED_ID_PATTERN = Pattern.compile("/video/(\\d+)");
    
    public VimeoUtils() {
    }
    
    public static String extractVideoId(String url) {
        // 尝试从标准URL提取
        Matcher matcher = VIDEO_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // 尝试从嵌入URL提取
        matcher = EMBED_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    public static String buildApiUrl(String videoId) {
        return "https://player.vimeo.com/video/" + videoId + "/config";
    }
    
    public static String buildVideoUrl(String videoId) {
        return "https://vimeo.com/" + videoId;
    }
    
    public static String buildEmbedUrl(String videoId) {
        return "https://player.vimeo.com/video/" + videoId;
    }
    
    public static boolean isVimeoUrl(String url) {
        return url != null && (url.contains("vimeo.com") || url.contains("player.vimeo.com"));
    }
    
    public static Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "en-US,en;q=0.5");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Connection", "keep-alive");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "none");
        return headers;
    }
    
    public static Map<String, String> getApiHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Connection", "keep-alive");
        headers.put("Sec-Fetch-Dest", "empty");
        headers.put("Sec-Fetch-Mode", "cors");
        headers.put("Sec-Fetch-Site", "same-origin");
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("Referer", "https://vimeo.com/");
        return headers;
    }
    
    public static void main(String[] args) {
        // 测试方法
        String testUrl = "https://vimeo.com/123456789";
        System.out.println("Video ID: " + extractVideoId(testUrl));
        System.out.println("API URL: " + buildApiUrl("123456789"));
        System.out.println("Is Vimeo URL: " + isVimeoUrl(testUrl));
    }
}
