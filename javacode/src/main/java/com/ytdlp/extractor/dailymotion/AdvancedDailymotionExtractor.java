package com.ytdlp.extractor.dailymotion;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Dailymotion视频提取器
 */
public class AdvancedDailymotionExtractor extends InfoExtractor {
    
    private static final String VALID_URL_REGEX = "(?:https?://)?(?:www\\.)?dailymotion\\.com/(?:video|embed)/([a-zA-Z0-9]+)";
    private static final Pattern VALID_URL_PATTERN = Pattern.compile(VALID_URL_REGEX);
    
    private Object auth; // DailymotionAuth对象，这里简化为Object
    
    static {
        // 静态初始化块
    }
    
    public AdvancedDailymotionExtractor() {
        super();
    }
    
    @Override
    public String getIE_NAME() {
        return "dailymotion";
    }
    
    @Override
    public String getIE_DESC() {
        return "Dailymotion video extractor";
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
        logger.info("Extracting Dailymotion video: " + videoId);
        
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("Dailymotion Video " + videoId);
        
        try {
            // 模拟下载网页内容
            String content = ""; // 在实际实现中会下载网页内容
            
            // 解析网页内容
            videoInfo = parseWebpageContent(content, url, videoId);
            
            return videoInfo;
            
        } catch (Exception e) {
            logger.error("Failed to extract Dailymotion video: " + e.getMessage());
            throw e;
        }
    }
    
    private VideoInfo parseWebpageContent(String content, String url, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        
        // 提取基本信息
        String title = extractTitle(content);
        videoInfo.setTitle(title != null ? title : "Dailymotion Video " + videoId);
        
        String description = extractDescription(content);
        videoInfo.setDescription(description);
        
        String thumbnail = extractThumbnail(content);
        videoInfo.setThumbnail(thumbnail);
        
        Long duration = extractDuration(content);
        videoInfo.setDuration(duration);
        
        String uploader = extractUploader(content);
        videoInfo.setUploader(uploader);
        
        Long viewCount = extractViewCount(content);
        videoInfo.setViewCount(viewCount);
        
        // 提取视频格式
        List<VideoFormat> formats = extractFormats(content, videoId);
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    private String extractTitle(String content) {
        // 从网页内容中提取标题
        String title = searchOgProperty(content, "og:title", "title", true);
        if (title == null) {
            title = searchRegex(content, "<title>([^<]+)</title>", "title", true);
        }
        return title;
    }
    
    private String extractDescription(String content) {
        // 从网页内容中提取描述
        return searchOgProperty(content, "og:description", "description", true);
    }
    
    private String extractThumbnail(String content) {
        // 从网页内容中提取缩略图
        return searchOgProperty(content, "og:image", "thumbnail", true);
    }
    
    private Long extractDuration(String content) {
        // 从网页内容中提取时长
        String durationStr = searchOgProperty(content, "video:duration", "duration", true);
        if (durationStr != null) {
            try {
                return Long.parseLong(durationStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private String extractUploader(String content) {
        // 从网页内容中提取上传者
        return searchOgProperty(content, "video:author", "uploader", true);
    }
    
    private Long extractViewCount(String content) {
        // 从网页内容中提取观看次数
        String viewCountStr = searchRegex(content, "\"views_count\":([0-9]+)", "view_count", true);
        if (viewCountStr != null) {
            try {
                return Long.parseLong(viewCountStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private List<VideoFormat> extractFormats(String content, String videoId) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 尝试多种方法提取视频格式
        formats.addAll(searchDirectVideoUrls(content));
        
        if (formats.isEmpty()) {
            formats.addAll(tryMetadataApi(videoId));
        }
        
        return formats;
    }
    
    private List<VideoFormat> searchDirectVideoUrls(String content) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 搜索直接的视频URL
        Pattern[] urlPatterns = {
            Pattern.compile("\"video_url\":\"([^\"]+)\""),
            Pattern.compile("\"url\":\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("src=\"([^\"]+\\.mp4[^\"]*)\"")
        };
        
        for (Pattern pattern : urlPatterns) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String url = matcher.group(1);
                if (url != null && !url.isEmpty()) {
                    VideoFormat format = createVideoFormat(url);
                    if (format != null) {
                        formats.add(format);
                    }
                }
            }
        }
        
        return formats;
    }
    
    private List<VideoFormat> tryMetadataApi(String videoId) {
        List<VideoFormat> formats = new ArrayList<>();
        
        try {
            // 尝试使用Dailymotion元数据API
            String apiUrl = "https://www.dailymotion.com/player/metadata/video/" + videoId;
            String response = ""; // 在实际实现中会下载API响应
            
            if (response != null && !response.isEmpty()) {
                formats.addAll(parseMetadataQualities(response));
            }
        } catch (Exception e) {
            logger.debug("Metadata API extraction failed: " + e.getMessage());
        }
        
        return formats;
    }
    
    private List<VideoFormat> parseMetadataQualities(String metadata) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 解析元数据中的质量信息
        Pattern qualityPattern = Pattern.compile("\"qualities\":\\{([^}]+)\\}");
        Matcher qualityMatcher = qualityPattern.matcher(metadata);
        
        if (qualityMatcher.find()) {
            String qualitiesContent = qualityMatcher.group(1);
            
            // 解析各种质量
            Pattern[] qualityPatterns = {
                Pattern.compile("\"auto\":\\{[^}]*\"url\":\"([^\"]+)\""),
                Pattern.compile("\"720\":\\{[^}]*\"url\":\"([^\"]+)\""),
                Pattern.compile("\"480\":\\{[^}]*\"url\":\"([^\"]+)\""),
                Pattern.compile("\"360\":\\{[^}]*\"url\":\"([^\"]+)\""),
                Pattern.compile("\"240\":\\{[^}]*\"url\":\"([^\"]+)\"")
            };
            
            for (Pattern pattern : qualityPatterns) {
                Matcher matcher = pattern.matcher(qualitiesContent);
                while (matcher.find()) {
                    String url = matcher.group(1);
                    VideoFormat format = createVideoFormat(url);
                    if (format != null) {
                        formats.add(format);
                    }
                }
            }
        }
        
        return formats;
    }
    
    private Integer parseQuality(String url) {
        // 从URL中解析质量信息
        if (url.contains("720")) return 720;
        if (url.contains("480")) return 480;
        if (url.contains("360")) return 360;
        if (url.contains("240")) return 240;
        if (url.contains("auto")) return 720;
        
        return 720; // 默认质量
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
    
    private String searchOgProperty(String content, String property, String name, boolean group) {
        String regex = "<meta property=\"" + property + "\" content=\"([^\"]+)\"";
        return searchRegex(content, regex, name, group);
    }
    
    private VideoFormat createVideoFormat(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        VideoFormat format = new VideoFormat();
        format.setUrl(url);
        format.setProtocol("https");
        
        // 根据URL确定格式和质量
        if (url.contains(".mp4")) {
            format.setExt("mp4");
            format.setVcodec("h264");
            format.setAcodec("aac");
        } else if (url.contains(".m3u8")) {
            format.setExt("m3u8");
            format.setProtocol("hls");
        }
        
        // 设置质量
        Integer quality = parseQuality(url);
        format.setQuality(quality);
        format.setResolution(quality + "p");
        
        format.setFormatId("default");
        format.setPreference(1);
        
        return format;
    }
}
