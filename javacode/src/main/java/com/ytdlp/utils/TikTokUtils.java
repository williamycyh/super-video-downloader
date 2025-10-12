package com.ytdlp.utils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * TikTok工具类
 */
public class TikTokUtils {
    
    private static final Random RANDOM = new Random();
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("/video/(\\d+)");
    private static final Pattern USER_ID_PATTERN = Pattern.compile("@([^/]+)");
    
    static {
        // 静态初始化
    }
    
    public TikTokUtils() {
    }
    
    public static TikTokVideoInfo extractVideoInfo(String html) {
        TikTokVideoInfo info = new TikTokVideoInfo();
        
        // 提取视频ID
        info.setVideoId(extractVideoId(html));
        
        // 提取用户ID
        info.setUserId(extractUserId(html));
        
        // 提取标题
        Pattern titlePattern = Pattern.compile("\"desc\":\"([^\"]+)\"");
        Matcher titleMatcher = titlePattern.matcher(html);
        if (titleMatcher.find()) {
            info.setTitle(titleMatcher.group(1));
        }
        
        // 提取作者
        Pattern authorPattern = Pattern.compile("\"nickname\":\"([^\"]+)\"");
        Matcher authorMatcher = authorPattern.matcher(html);
        if (authorMatcher.find()) {
            info.setAuthor(authorMatcher.group(1));
        }
        
        // 提取视频URL
        Pattern urlPattern = Pattern.compile("\"playAddr\":\"([^\"]+)\"");
        Matcher urlMatcher = urlPattern.matcher(html);
        if (urlMatcher.find()) {
            info.setVideoUrl(urlMatcher.group(1));
        }
        
        // 提取封面URL
        Pattern coverPattern = Pattern.compile("\"cover\":\"([^\"]+)\"");
        Matcher coverMatcher = coverPattern.matcher(html);
        if (coverMatcher.find()) {
            info.setCoverUrl(coverMatcher.group(1));
        }
        
        return info;
    }
    
    public static String extractVideoId(String url) {
        Matcher matcher = VIDEO_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    public static String extractUserId(String url) {
        Matcher matcher = USER_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    public static String buildVideoUrl(String userId, String videoId) {
        return "https://www.tiktok.com/@" + userId + "/video/" + videoId;
    }
    
    public static String buildEmbedUrl(String videoId) {
        return "https://www.tiktok.com/embed/" + videoId;
    }
    
    public static String generateDeviceId() {
        StringBuilder deviceId = new StringBuilder();
        String chars = "0123456789abcdef";
        
        for (int i = 0; i < 32; i++) {
            deviceId.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        
        return deviceId.toString();
    }
    
    public static String generateInstallId() {
        StringBuilder installId = new StringBuilder();
        String chars = "0123456789abcdef";
        
        for (int i = 0; i < 16; i++) {
            installId.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        
        return installId.toString();
    }
    
    public static boolean isTikTokUrl(String url) {
        return url != null && (url.contains("tiktok.com") || url.contains("vm.tiktok.com"));
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
        return headers;
    }
    
    public static Map<String, String> getMobileHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Mobile/15E148 Safari/604.1");
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Connection", "keep-alive");
        headers.put("X-Requested-With", "XMLHttpRequest");
        return headers;
    }
    
    public static QualityInfo parseUrlKey(String url) {
        QualityInfo qualityInfo = new QualityInfo();
        
        if (url.contains("720p")) {
            qualityInfo.setWidth(1280);
            qualityInfo.setHeight(720);
            qualityInfo.setQuality("720p");
            qualityInfo.setVcodec("h264");
        } else if (url.contains("480p")) {
            qualityInfo.setWidth(854);
            qualityInfo.setHeight(480);
            qualityInfo.setQuality("480p");
            qualityInfo.setVcodec("h264");
        } else if (url.contains("360p")) {
            qualityInfo.setWidth(640);
            qualityInfo.setHeight(360);
            qualityInfo.setQuality("360p");
            qualityInfo.setVcodec("h264");
        } else if (url.contains("240p")) {
            qualityInfo.setWidth(426);
            qualityInfo.setHeight(240);
            qualityInfo.setQuality("240p");
            qualityInfo.setVcodec("h264");
        } else {
            qualityInfo.setWidth(1280);
            qualityInfo.setHeight(720);
            qualityInfo.setQuality("720p");
            qualityInfo.setVcodec("h264");
        }
        
        return qualityInfo;
    }
    
    public static void main(String[] args) {
        // 测试方法
        String testUrl = "https://www.tiktok.com/@user/video/1234567890123456789";
        System.out.println("Video ID: " + extractVideoId(testUrl));
        System.out.println("User ID: " + extractUserId(testUrl));
        System.out.println("Is TikTok URL: " + isTikTokUrl(testUrl));
        System.out.println("Device ID: " + generateDeviceId());
        System.out.println("Install ID: " + generateInstallId());
    }
    
    /**
     * TikTok视频信息类
     */
    public static class TikTokVideoInfo {
        private String videoId;
        private String userId;
        private String title;
        private String author;
        private String videoUrl;
        private String coverUrl;
        
        public TikTokVideoInfo() {
        }
        
        public TikTokVideoInfo(String videoId, String userId) {
            this.videoId = videoId;
            this.userId = userId;
        }
        
        public String getVideoId() { return videoId; }
        public void setVideoId(String videoId) { this.videoId = videoId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getVideoUrl() { return videoUrl; }
        public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
        
        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    }
    
    /**
     * 质量信息类
     */
    public static class QualityInfo {
        private int width;
        private int height;
        private String quality;
        private String vcodec;
        
        public QualityInfo() {
        }
        
        public QualityInfo(int width, int height, String quality, String vcodec) {
            this.width = width;
            this.height = height;
            this.quality = quality;
            this.vcodec = vcodec;
        }
        
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        
        public String getQuality() { return quality; }
        public void setQuality(String quality) { this.quality = quality; }
        
        public String getVcodec() { return vcodec; }
        public void setVcodec(String vcodec) { this.vcodec = vcodec; }
    }
}
