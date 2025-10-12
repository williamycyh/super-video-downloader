package com.ytdlp.extractor.xvideos;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * XVideos视频提取器
 */
public class AdvancedXVideosExtractor extends InfoExtractor {
    
    private static final String VALID_URL_REGEX = "(?:https?://)?(?:www\\.)?xvideos\\.com/video([0-9]+)/([^/]+)";
    private static final Pattern VALID_URL_PATTERN = Pattern.compile(VALID_URL_REGEX);
    
    static {
        // 静态初始化
    }
    
    public AdvancedXVideosExtractor() {
        super();
    }
    
    @Override
    public String getIE_NAME() {
        return "xvideos";
    }
    
    @Override
    public String getIE_DESC() {
        return "XVideos video extractor";
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
        logger.info("Extracting XVideos video: " + videoId);
        
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("XVideos Video " + videoId);
        
        try {
            // 模拟下载网页内容
            String content = ""; // 在实际实现中会下载网页内容
            
            // 解析网页内容
            videoInfo = parseWebpageContent(content, videoId, url);
            
            return videoInfo;
            
        } catch (Exception e) {
            logger.error("Failed to extract XVideos video: " + e.getMessage());
            throw e;
        }
    }
    
    private VideoInfo parseWebpageContent(String content, String videoId, String url) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        
        // 提取标题
        String title = extractTitle(content);
        videoInfo.setTitle(title != null ? title : "XVideos Video " + videoId);
        
        // 提取缩略图
        String thumbnail = extractThumbnail(content);
        videoInfo.setThumbnail(thumbnail);
        
        // 提取时长
        Long duration = extractDuration(content);
        videoInfo.setDuration(duration);
        
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
            title = title.replaceAll("\\s*\\|\\s*XVideos.*$", "");
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
            return parseDuration(durationMatcher.group(1));
        }
        
        return null;
    }
    
    private Long parseDuration(String durationStr) {
        if (durationStr == null) {
            return null;
        }
        
        try {
            return Long.parseLong(durationStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private List<VideoFormat> extractFormats(String content, String videoId) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 查找各种可能的视频URL模式
        Pattern[] urlPatterns = {
            Pattern.compile("\"videoUrl\":\\s*\"([^\"]+)\""),
            Pattern.compile("\"url\":\\s*\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("src=\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("\"mp4\":\\s*\"([^\"]+)\""),
            Pattern.compile("\"hls\":\\s*\"([^\"]+\\.m3u8[^\"]*)\"")
        };
        
        for (Pattern pattern : urlPatterns) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String url = matcher.group(1);
                String ext = determineExt(url, content);
                
                VideoFormat format = createVideoFormat(url, ext);
                if (format != null) {
                    formats.add(format);
                }
            }
        }
        
        return formats;
    }
    
    private String determineExt(String url, String content) {
        if (url == null) {
            return "mp4";
        }
        
        if (url.contains(".m3u8")) {
            return "m3u8";
        } else if (url.contains(".mp4")) {
            return "mp4";
        } else if (url.contains(".webm")) {
            return "webm";
        } else if (url.contains(".flv")) {
            return "flv";
        }
        
        return "mp4";
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
    
    private VideoFormat createVideoFormat(String url, String ext) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        VideoFormat format = new VideoFormat();
        format.setUrl(url);
        format.setProtocol("https");
        format.setExt(ext);
        
        // 设置编解码器
        if ("mp4".equals(ext)) {
            format.setVcodec("h264");
            format.setAcodec("aac");
        } else if ("webm".equals(ext)) {
            format.setVcodec("vp8");
            format.setAcodec("vorbis");
        } else if ("m3u8".equals(ext)) {
            format.setProtocol("hls");
        }
        
        // 设置质量
        if (url.contains("720p")) {
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
