package com.btdlp.utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Instagram工具类
 */
public class InstagramUtils {
    
    public static final String VALID_URL_REGEX = "(?:https?://)?(?:www\\.)?instagram\\.com/(?:p|reel|tv)/([a-zA-Z0-9_-]+)";
    public static final Pattern VALID_URL_PATTERN = Pattern.compile(VALID_URL_REGEX);
    
    private static final String ENCODING_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    
    public InstagramUtils() {
    }
    
    /**
     * 将PK转换为ID
     */
    public static String pkToId(String pk) {
        if (pk == null || pk.isEmpty()) {
            return null;
        }
        
        try {
            long id = decodeBaseN(pk, ENCODING_CHARS);
            return String.valueOf(id);
        } catch (Exception e) {
            return pk; // 如果转换失败，返回原始值
        }
    }
    
    /**
     * 将ID转换为PK
     */
    public static String idToPk(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        
        try {
            long longId = Long.parseLong(id);
            return encodeBaseN(longId, ENCODING_CHARS);
        } catch (NumberFormatException e) {
            return id; // 如果转换失败，返回原始值
        }
    }
    
    /**
     * Base64-like编码
     */
    private static String encodeBaseN(long value, String charset) {
        if (value == 0) {
            return String.valueOf(charset.charAt(0));
        }
        
        StringBuilder result = new StringBuilder();
        while (value > 0) {
            result.insert(0, charset.charAt((int) (value % 64)));
            value /= 64;
        }
        
        return result.toString();
    }
    
    /**
     * Base64-like解码
     */
    private static long decodeBaseN(String encoded, String charset) {
        long result = 0;
        long base = 1;
        
        for (int i = encoded.length() - 1; i >= 0; i--) {
            char c = encoded.charAt(i);
            int index = charset.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid character: " + c);
            }
            result += index * base;
            base *= 64;
        }
        
        return result;
    }
    
    /**
     * 从URL中提取短代码
     */
    public static String extractShortcode(String url) {
        if (url == null) {
            return null;
        }
        
        Matcher matcher = VALID_URL_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 构建API URL
     */
    public static String buildApiUrl(String shortcode, String type) {
        if (shortcode == null || shortcode.isEmpty()) {
            return null;
        }
        
        StringBuilder url = new StringBuilder("https://www.instagram.com/");
        url.append(type).append("/").append(shortcode).append("/");
        
        return url.toString();
    }
    
    /**
     * 构建GraphQL URL
     */
    public static String buildGraphQLUrl() {
        return "https://www.instagram.com/graphql/query/";
    }
    
    /**
     * 构建嵌入URL
     */
    public static String buildEmbedUrl(String shortcode) {
        if (shortcode == null || shortcode.isEmpty()) {
            return null;
        }
        
        return "https://www.instagram.com/p/" + shortcode + "/embed/";
    }
    
    /**
     * 构建视频URL
     */
    public static String buildVideoUrl(String shortcode) {
        if (shortcode == null || shortcode.isEmpty()) {
            return null;
        }
        
        return "https://www.instagram.com/p/" + shortcode + "/";
    }
    
    /**
     * 检查是否为Instagram URL
     */
    public static boolean isInstagramUrl(String url) {
        return url != null && VALID_URL_PATTERN.matcher(url).matches();
    }
    
    /**
     * 获取默认请求头
     */
    public static java.util.Map<String, String> getDefaultHeaders() {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
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
    
    /**
     * 获取API请求头
     */
    public static java.util.Map<String, String> getApiHeaders() {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Connection", "keep-alive");
        headers.put("Sec-Fetch-Dest", "empty");
        headers.put("Sec-Fetch-Mode", "cors");
        headers.put("Sec-Fetch-Site", "same-origin");
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("X-CSRFToken", "missing");
        headers.put("Referer", "https://www.instagram.com/");
        return headers;
    }
    
    /**
     * 获取移动端请求头
     */
    public static java.util.Map<String, String> getMobileHeaders() {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("User-Agent", "Instagram 219.0.0.12.117 Android");
        headers.put("Accept", "*/*");
        headers.put("Accept-Language", "en-US");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Connection", "keep-alive");
        return headers;
    }
    
    /**
     * 提取视频ID（兼容方法）
     */
    public static String extractVideoId(String url) {
        return extractShortcode(url);
    }
    
    public static void main(String[] args) {
        // 测试方法
        String testUrl = "https://www.instagram.com/p/ABC123DEF456/";
        System.out.println("Shortcode: " + extractShortcode(testUrl));
        System.out.println("Video URL: " + buildVideoUrl("ABC123DEF456"));
        System.out.println("Embed URL: " + buildEmbedUrl("ABC123DEF456"));
        System.out.println("Is Instagram URL: " + isInstagramUrl(testUrl));
        
        // 测试PK/ID转换
        String pk = "ABC123DEF456";
        String id = pkToId(pk);
        String backToPk = idToPk(id);
        System.out.println("PK: " + pk + " -> ID: " + id + " -> PK: " + backToPk);
    }
}
