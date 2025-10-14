package com.btdlp.extractor.xnxx;

import com.btdlp.core.VideoInfo;
import com.btdlp.core.VideoFormat;
import com.btdlp.extractor.InfoExtractor;
import com.btdlp.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * XNXX视频提取器
 */
public class AdvancedXNXXExtractor extends InfoExtractor {
    
    private static final String VALID_URL_REGEX = "(?:https?://)?(?:www\\.)?xnxx\\.com/video-([a-zA-Z0-9]+)/[^/]+";
    private static final Pattern VALID_URL_PATTERN = Pattern.compile(VALID_URL_REGEX);
    
    static {
        // 静态初始化
    }
    
    public AdvancedXNXXExtractor() {
        super();
    }
    
    @Override
    public String getIE_NAME() {
        return "xnxx";
    }
    
    @Override
    public String getIE_DESC() {
        return "XNXX video extractor";
    }
    
    @Override
    public Pattern getVALID_URL() {
        return VALID_URL_PATTERN;
    }
    
    @Override
    public boolean suitable(String url) {
        return VALID_URL_PATTERN.matcher(url).matches();
    }
    
    @Override
    protected String extractVideoId(String url) {
        Matcher matcher = VALID_URL_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    @Override
    protected VideoInfo realExtract(String url, String videoId) throws Exception {
        logger.info("Extracting XNXX video: " + videoId);
        
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("XNXX Video " + videoId);
        
        try {
            // 模拟下载网页内容
            String content = ""; // 在实际实现中会下载网页内容
            
            // 解析网页内容
            videoInfo = parseWebpageContent(content, videoId, url);
            
            return videoInfo;
            
        } catch (Exception e) {
            logger.error("Failed to extract XNXX video: " + e.getMessage());
            throw e;
        }
    }
    
    private VideoInfo parseWebpageContent(String content, String videoId, String url) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        
        // 提取标题
        String title = extractTitle(content);
        videoInfo.setTitle(title != null ? title : "XNXX Video " + videoId);
        
        // 提取缩略图
        String thumbnail = extractThumbnail(content);
        videoInfo.setThumbnail(thumbnail);
        
        // 提取时长
        Long duration = extractDuration(content);
        videoInfo.setDuration(duration);
        
        // 提取观看次数
        Long viewCount = extractViewCount(content);
        videoInfo.setViewCount(viewCount);
        
        // 提取视频格式
        List<VideoFormat> formats = extractFormats(content, videoId);
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    private String extractTitle(String content) {
        Pattern titlePattern = Pattern.compile("<title>([^<]+)</title>");
        Matcher titleMatcher = titlePattern.matcher(content);
        
        if (titleMatcher.find()) {
            String title = titleMatcher.group(1).trim();
            title = title.replaceAll("\\s*\\|\\s*XNXX.*$", "");
            return title;
        }
        
        return null;
    }
    
    private String extractThumbnail(String content) {
        Pattern thumbnailPattern = Pattern.compile("<meta property=\"og:image\" content=\"([^\"]+)\"");
        Matcher thumbnailMatcher = thumbnailPattern.matcher(content);
        
        if (thumbnailMatcher.find()) {
            return thumbnailMatcher.group(1);
        }
        
        return null;
    }
    
    private Long extractDuration(String content) {
        Pattern durationPattern = Pattern.compile("\"duration\":\\s*(\\d+)");
        Matcher durationMatcher = durationPattern.matcher(content);
        
        if (durationMatcher.find()) {
            try {
                return Long.parseLong(durationMatcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    private Long extractViewCount(String content) {
        Pattern viewPattern = Pattern.compile("\"viewCount\":\\s*(\\d+)");
        Matcher viewMatcher = viewPattern.matcher(content);
        
        if (viewMatcher.find()) {
            try {
                return Long.parseLong(viewMatcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    private List<VideoFormat> extractFormats(String content, String videoId) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 查找各种可能的视频URL模式
        Pattern[] urlPatterns = {
            Pattern.compile("\"videoUrl\":\\s*\"([^\"]+)\""),
            Pattern.compile("\"url\":\\s*\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("src=\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("\"mp4\":\\s*\"([^\"]+)\""),
            Pattern.compile("\"hls\":\\s*\"([^\"]+\\.m3u8[^\"]*)\""),
            Pattern.compile("\"high\":\\s*\"([^\"]+)\""),
            Pattern.compile("\"low\":\\s*\"([^\"]+)\"")
        };
        
        for (Pattern pattern : urlPatterns) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String url = matcher.group(1);
                VideoFormat format = createVideoFormat(url, pattern.pattern());
                if (format != null) {
                    formats.add(format);
                }
            }
        }
        
        return formats;
    }
    
    private String searchRegex(String content, String regex, String name, boolean group) {
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);
            
            if (matcher.find()) {
                if (group) {
                    return matcher.group(1);
                } else {
                    return matcher.group(0);
                }
            }
        } catch (Exception e) {
            logger.debug("Regex search failed for " + name + ": " + e.getMessage());
        }
        
        return null;
    }
    
    private VideoFormat createVideoFormat(String url, String pattern) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        VideoFormat format = new VideoFormat();
        format.setUrl(url);
        format.setProtocol("https");
        
        // 根据URL确定格式
        if (url.contains(".m3u8")) {
            format.setExt("m3u8");
            format.setProtocol("hls");
        } else if (url.contains(".mp4")) {
            format.setExt("mp4");
            format.setVcodec("h264");
            format.setAcodec("aac");
        } else if (url.contains(".webm")) {
            format.setExt("webm");
            format.setVcodec("vp8");
            format.setAcodec("vorbis");
        } else if (url.contains(".flv")) {
            format.setExt("flv");
        } else {
            format.setExt("mp4");
            format.setVcodec("h264");
            format.setAcodec("aac");
        }
        
        // 根据模式确定质量
        if (pattern.contains("high")) {
            format.setQuality(720);
            format.setResolution("720p");
        } else if (pattern.contains("low")) {
            format.setQuality(360);
            format.setResolution("360p");
        } else if (url.contains("720p")) {
            format.setQuality(720);
            format.setResolution("720p");
        } else if (url.contains("480p")) {
            format.setQuality(480);
            format.setResolution("480p");
        } else if (url.contains("360p")) {
            format.setQuality(360);
            format.setResolution("360p");
        } else if (url.contains("240p")) {
            format.setQuality(240);
            format.setResolution("240p");
        } else {
            format.setQuality(720);
            format.setResolution("720p");
        }
        
        format.setFormatId("default");
        format.setPreference(1);
        
        return format;
    }
}
