package com.ytdlp.utils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * XHamster工具类
 */
public class XHamsterUtils {
    
    private static final String DOMAINS = "xhamster.com|xhamster2.com|xhamster3.com";
    public static final String VALID_URL_REGEX = "(?:https?://)?(?:www\\.)?(?:xhamster\\.com|xhamster2\\.com|xhamster3\\.com)/videos/([a-zA-Z0-9\\-_]+)-([a-zA-Z0-9]+)";
    public static final Pattern VALID_URL_PATTERN = Pattern.compile(VALID_URL_REGEX);
    private static final byte[] XOR_KEY = {0x13, 0x37, 0x42};
    
    static {
        // 静态初始化
    }
    
    public XHamsterUtils() {
    }
    
    public static XHamsterVideoInfo extractVideoInfo(String html) {
        XHamsterVideoInfo info = new XHamsterVideoInfo();
        
        // 提取标题
        Pattern titlePattern = Pattern.compile("<title>([^<]+)</title>");
        Matcher titleMatcher = titlePattern.matcher(html);
        if (titleMatcher.find()) {
            String title = titleMatcher.group(1).trim();
            title = title.replaceAll("\\s*\\|\\s*XHamster.*$", "");
            info.setTitle(title);
        }
        
        // 提取时长
        Pattern durationPattern = Pattern.compile("\"duration\":\\s*(\\d+)");
        Matcher durationMatcher = durationPattern.matcher(html);
        if (durationMatcher.find()) {
            info.setDuration(Long.parseLong(durationMatcher.group(1)));
        }
        
        // 提取视频URL
        Pattern urlPattern = Pattern.compile("\"url\":\\s*\"([^\"]+)\"");
        Matcher urlMatcher = urlPattern.matcher(html);
        while (urlMatcher.find()) {
            String url = urlMatcher.group(1);
            if (url != null && !url.isEmpty()) {
                info.addVideoUrl(url);
            }
        }
        
        return info;
    }
    
    public static String decipherFormatUrl(String url, String key) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        try {
            // 简单的ROT13解码
            return rot13Decode(url);
        } catch (Exception e) {
            return url;
        }
    }
    
    private static String rot13Decode(String input) {
        StringBuilder result = new StringBuilder();
        
        for (char c : input.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                result.append((char) (((c - 'a' + 13) % 26) + 'a'));
            } else if (c >= 'A' && c <= 'Z') {
                result.append((char) (((c - 'A' + 13) % 26) + 'A'));
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    public static Integer getHeight(String url) {
        if (url == null) {
            return null;
        }
        
        // 从URL中提取高度信息
        if (url.contains("720p")) {
            return 720;
        } else if (url.contains("480p")) {
            return 480;
        } else if (url.contains("360p")) {
            return 360;
        } else if (url.contains("240p")) {
            return 240;
        }
        
        return null;
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
        headers.put("Cookie", "age_verified=1");
        return headers;
    }
    
    public static String getDesktopUrl(String mobileUrl) {
        if (mobileUrl == null) {
            return null;
        }
        
        return mobileUrl.replace("m.xhamster.com", "www.xhamster.com")
                       .replace("xhamster.com/m", "xhamster.com");
    }
    
    public static Map<String, String> parseUrlKey(String url) {
        Map<String, String> result = new HashMap<>();
        
        if (url == null) {
            return result;
        }
        
        Matcher matcher = VALID_URL_PATTERN.matcher(url);
        if (matcher.find()) {
            result.put("id", matcher.group(2));
            result.put("title", matcher.group(1));
        }
        
        return result;
    }
    
    /**
     * XHamster视频信息类
     */
    public static class XHamsterVideoInfo {
        private String title;
        private Long duration;
        private List<String> videoUrls;
        
        public XHamsterVideoInfo() {
            this.videoUrls = new ArrayList<>();
        }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public Long getDuration() { return duration; }
        public void setDuration(Long duration) { this.duration = duration; }
        
        public List<String> getVideoUrls() { return videoUrls; }
        public void setVideoUrls(List<String> videoUrls) { this.videoUrls = videoUrls; }
        
        public void addVideoUrl(String url) {
            if (url != null && !url.isEmpty()) {
                this.videoUrls.add(url);
            }
        }
    }
}
