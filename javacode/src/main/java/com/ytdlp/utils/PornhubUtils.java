package com.ytdlp.utils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Pornhub工具类
 */
public class PornhubUtils {
    
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile("viewkey=([a-zA-Z0-9]+)");
    private static final Pattern HOST_PATTERN = Pattern.compile("https?://([^/]+)");
    private static final Pattern QUALITY_PATTERN = Pattern.compile("quality\\s*=\\s*['\"](\\d+)['\"]");
    private static final Pattern URL_PATTERN = Pattern.compile("['\"](https?://[^'\"]+\\.mp4[^'\"]*)['\"]");
    
    public PornhubUtils() {
    }
    
    public static PornhubVideoInfo extractVideoInfo(String html) {
        PornhubVideoInfo info = new PornhubVideoInfo();
        
        // 提取视频ID
        info.setVideoId(extractVideoId(html));
        
        // 提取标题
        Pattern titlePattern = Pattern.compile("<title>([^<]+)</title>");
        Matcher titleMatcher = titlePattern.matcher(html);
        if (titleMatcher.find()) {
            info.setTitle(titleMatcher.group(1).trim());
        }
        
        // 提取时长
        Pattern durationPattern = Pattern.compile("\"duration\":\\s*(\\d+)");
        Matcher durationMatcher = durationPattern.matcher(html);
        if (durationMatcher.find()) {
            info.setDuration(Long.parseLong(durationMatcher.group(1)));
        }
        
        // 提取视频URL
        List<String> videoUrls = extractVideoUrls(html);
        info.setVideoUrls(videoUrls);
        
        return info;
    }
    
    public static String extractVideoId(String html) {
        Matcher matcher = VIDEO_ID_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    public static String extractHost(String url) {
        Matcher matcher = HOST_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "www.pornhub.com";
    }
    
    public static String buildVideoUrl(String host, String videoId) {
        return "https://" + host + "/view_video.php?viewkey=" + videoId;
    }
    
    public static String buildEmbedUrl(String host, String videoId) {
        return "https://" + host + "/embed/" + videoId;
    }
    
    public static boolean isPornhubUrl(String url) {
        return url != null && (url.contains("pornhub.com") || url.contains("pornhubpremium.com"));
    }
    
    public static Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "en-US,en;q=0.5");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Connection", "keep-alive");
        headers.put("Upgrade-Insecure-Requests", "1");
        return headers;
    }
    
    public static Map<String, String> getVideoHeaders() {
        Map<String, String> headers = getDefaultHeaders();
        headers.put("Referer", "https://www.pornhub.com/");
        return headers;
    }
    
    public static Map<String, String> getAgeVerificationCookies(String url) {
        Map<String, String> cookies = new HashMap<>();
        cookies.put("age_verified", "1");
        cookies.put("platform", "pc");
        return cookies;
    }
    
    public static QualityInfo parseQualityFromUrl(String url) {
        QualityInfo qualityInfo = new QualityInfo();
        
        if (url.contains("720p")) {
            qualityInfo.setHeight(720);
            qualityInfo.setQuality("720p");
            qualityInfo.setFormat("mp4");
        } else if (url.contains("480p")) {
            qualityInfo.setHeight(480);
            qualityInfo.setQuality("480p");
            qualityInfo.setFormat("mp4");
        } else if (url.contains("360p")) {
            qualityInfo.setHeight(360);
            qualityInfo.setQuality("360p");
            qualityInfo.setFormat("mp4");
        } else if (url.contains("240p")) {
            qualityInfo.setHeight(240);
            qualityInfo.setQuality("240p");
            qualityInfo.setFormat("mp4");
        } else {
            qualityInfo.setHeight(720);
            qualityInfo.setQuality("720p");
            qualityInfo.setFormat("mp4");
        }
        
        return qualityInfo;
    }
    
    public static Map<String, Object> parseFlashvars(String flashVars) {
        Map<String, Object> vars = new HashMap<>();
        
        if (flashVars != null && !flashVars.isEmpty()) {
            String[] pairs = flashVars.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    try {
                        String key = keyValue[0];
                        String value = keyValue[1];
                        
                        // 尝试解析为数字
                        if (value.matches("\\d+")) {
                            vars.put(key, Long.parseLong(value));
                        } else if (value.matches("\\d+\\.\\d+")) {
                            vars.put(key, Double.parseDouble(value));
                        } else {
                            vars.put(key, value);
                        }
                    } catch (Exception e) {
                        // 忽略解析错误
                    }
                }
            }
        }
        
        return vars;
    }
    
    public static List<String> extractVideoUrls(String html) {
        List<String> urls = new ArrayList<>();
        
        // 查找各种可能的视频URL模式
        Pattern[] patterns = {
            Pattern.compile("\"videoUrl\":\\s*\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("\"url\":\\s*\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("src=\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("'([^']+\\.mp4[^']*)'"),
            Pattern.compile("\"([^\"]+\\.mp4[^\"]*)\"")
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(html);
            while (matcher.find()) {
                String url = matcher.group(1);
                if (url != null && !url.isEmpty() && !urls.contains(url)) {
                    urls.add(url);
                }
            }
        }
        
        return urls;
    }
    
    public static void main(String[] args) {
        // 测试方法
        String testUrl = "https://www.pornhub.com/view_video.php?viewkey=ph5f8b8c8c8c8c8";
        System.out.println("Video ID: " + extractVideoId(testUrl));
        System.out.println("Host: " + extractHost(testUrl));
        System.out.println("Is Pornhub URL: " + isPornhubUrl(testUrl));
    }
    
    /**
     * Pornhub视频信息类
     */
    public static class PornhubVideoInfo {
        private String videoId;
        private String host;
        private String baseUrl;
        private String title;
        private Long duration;
        private List<String> videoUrls;
        
        public PornhubVideoInfo() {
            this.videoUrls = new ArrayList<>();
        }
        
        public PornhubVideoInfo(String videoId, String host, String baseUrl) {
            this.videoId = videoId;
            this.host = host;
            this.baseUrl = baseUrl;
            this.videoUrls = new ArrayList<>();
        }
        
        public String getVideoId() { return videoId; }
        public void setVideoId(String videoId) { this.videoId = videoId; }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public Long getDuration() { return duration; }
        public void setDuration(Long duration) { this.duration = duration; }
        
        public List<String> getVideoUrls() { return videoUrls; }
        public void setVideoUrls(List<String> videoUrls) { this.videoUrls = videoUrls; }
    }
    
    /**
     * 质量信息类
     */
    public static class QualityInfo {
        private int height;
        private String quality;
        private String format;
        
        public QualityInfo() {
        }
        
        public QualityInfo(int height, String quality, String format) {
            this.height = height;
            this.quality = quality;
            this.format = format;
        }
        
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        
        public String getQuality() { return quality; }
        public void setQuality(String quality) { this.quality = quality; }
        
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
    }
}
