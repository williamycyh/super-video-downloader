package com.btdlp.extractor.pornhub;

import com.btdlp.core.VideoInfo;
import com.btdlp.core.VideoFormat;
import com.btdlp.extractor.InfoExtractor;
import com.btdlp.utils.Logger;
import com.btdlp.utils.PornhubUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Pornhub视频提取器
 */
public class AdvancedPornhubExtractor extends InfoExtractor {
    
    private static final String TAG = "AdvancedPornhubExtractor";
    private static final String PORNHUB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    
    public AdvancedPornhubExtractor() {
        super();
    }
    
    @Override
    public String getIE_NAME() {
        return "pornhub";
    }
    
    @Override
    public String getIE_DESC() {
        return "Pornhub video extractor";
    }
    
    @Override
    public Pattern getVALID_URL() {
        return Pattern.compile(
            "(?:https?://)?(?:www\\.)?(?:pornhub\\.com|pornhubpremium\\.com)/(?:view_video\\.php\\?viewkey=|embed/)([a-zA-Z0-9]+)"
        );
    }
    
    @Override
    protected String extractVideoId(String url) {
        Matcher matcher = getVALID_URL().matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    @Override
    protected VideoInfo realExtract(String url, String videoId) throws Exception {
        logger.info("Extracting Pornhub video: " + videoId);
        
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("Pornhub Video " + videoId);
        
        try {
            String host = PornhubUtils.extractHost(url);
            
            // 尝试多种提取方法
            videoInfo = tryWebpageExtraction(url, videoId, host);
            if (videoInfo.getFormats() == null || videoInfo.getFormats().isEmpty()) {
                videoInfo = tryEmbedExtraction(url, videoId);
            }
            
            return videoInfo;
            
        } catch (Exception e) {
            logger.error("Failed to extract Pornhub video: " + e.getMessage());
            throw e;
        }
    }
    
    private VideoInfo tryWebpageExtraction(String url, String videoId, String host) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("Pornhub Video " + videoId);
        
        try {
            String content = ""; // 在实际实现中会下载网页内容
            
            return parseWebpageContent(content, videoId, host);
        } catch (Exception e) {
            logger.debug("Webpage extraction failed: " + e.getMessage());
        }
        
        return videoInfo;
    }
    
    private VideoInfo tryEmbedExtraction(String url, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("Pornhub Video " + videoId);
        
        try {
            String embedUrl = PornhubUtils.buildEmbedUrl(PornhubUtils.extractHost(url), videoId);
            String content = ""; // 在实际实现中会下载嵌入页面内容
            
            return parseEmbedContent(content, videoId);
        } catch (Exception e) {
            logger.debug("Embed extraction failed: " + e.getMessage());
        }
        
        return videoInfo;
    }
    
    private String extractErrorMessage(String content) {
        Pattern errorPattern = Pattern.compile("<h1[^>]*>([^<]+)</h1>");
        Matcher errorMatcher = errorPattern.matcher(content);
        
        if (errorMatcher.find()) {
            String errorText = errorMatcher.group(1).trim();
            if (errorText.toLowerCase().contains("error") || 
                errorText.toLowerCase().contains("not found") ||
                errorText.toLowerCase().contains("private")) {
                return errorText;
            }
        }
        
        return null;
    }
    
    private boolean isGeoBlocked(String content) {
        return content.contains("This content is not available in your country") ||
               content.contains("geo-blocked") ||
               content.contains("not available in your region");
    }
    
    private VideoInfo parseWebpageContent(String content, String videoId, String host) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setTitle("Pornhub Video " + videoId);
        
        // 检查错误
        String errorMessage = extractErrorMessage(content);
        if (errorMessage != null) {
            logger.error("Video error: " + errorMessage);
            return videoInfo;
        }
        
        // 检查地理限制
        if (isGeoBlocked(content)) {
            logger.error("Video is geo-blocked");
            return videoInfo;
        }
        
        // 提取基本信息
        extractBasicInfo(content, videoInfo);
        
        // 提取Flashvars
        String flashvars = extractFlashvars(content);
        if (flashvars != null) {
            Map<String, Object> vars = PornhubUtils.parseFlashvars(flashvars);
            List<VideoFormat> formats = parseFlashvarsFormats(vars);
            videoInfo.setFormats(formats);
        }
        
        return videoInfo;
    }
    
    private VideoInfo parseEmbedContent(String content, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setTitle("Pornhub Video " + videoId);
        
        // 提取基本信息
        extractBasicInfo(content, videoInfo);
        
        // 提取Flashvars
        String flashvars = extractFlashvars(content);
        if (flashvars != null) {
            Map<String, Object> vars = PornhubUtils.parseFlashvars(flashvars);
            List<VideoFormat> formats = parseFlashvarsFormats(vars);
            videoInfo.setFormats(formats);
        }
        
        return videoInfo;
    }
    
    private String extractTitle(String content) {
        Pattern titlePattern = Pattern.compile("<title>([^<]+)</title>");
        Matcher titleMatcher = titlePattern.matcher(content);
        
        if (titleMatcher.find()) {
            String title = titleMatcher.group(1).trim();
            // 移除网站名称
            title = title.replaceAll("\\s*\\|\\s*Pornhub.*$", "");
            return title;
        }
        
        return null;
    }
    
    private String extractFlashvars(String content) {
        Pattern flashvarsPattern = Pattern.compile("flashvars_\\d+\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher flashvarsMatcher = flashvarsPattern.matcher(content);
        
        if (flashvarsMatcher.find()) {
            return flashvarsMatcher.group(1);
        }
        
        return null;
    }
    
    private List<VideoFormat> parseFlashvarsFormats(Map<String, Object> vars) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 解析各种质量格式
        String[] qualityKeys = {"quality_720p", "quality_480p", "quality_360p", "quality_240p"};
        String[] qualityNames = {"720p", "480p", "360p", "240p"};
        
        for (int i = 0; i < qualityKeys.length; i++) {
            String qualityKey = qualityKeys[i];
            String qualityName = qualityNames[i];
            
            Object qualityValue = vars.get(qualityKey);
            if (qualityValue instanceof String) {
                String url = (String) qualityValue;
                if (url != null && !url.isEmpty()) {
                    VideoFormat format = createVideoFormat(url, "mp4", qualityName);
                    if (format != null) {
                        formats.add(format);
                    }
                }
            }
        }
        
        // 如果没有找到质量格式，尝试默认格式
        Object defaultUrl = vars.get("mediaDefinitions");
        if (defaultUrl instanceof String) {
            String url = (String) defaultUrl;
            if (url != null && !url.isEmpty()) {
                VideoFormat format = createVideoFormat(url, "mp4", "default");
                if (format != null) {
                    formats.add(format);
                }
            }
        }
        
        return formats;
    }
    
    private void extractBasicInfo(String content, VideoInfo videoInfo) {
        // 提取标题
        String title = extractTitle(content);
        if (title != null) {
            videoInfo.setTitle(title);
        }
        
        // 提取时长
        Pattern durationPattern = Pattern.compile("\"duration\":\\s*(\\d+)");
        Matcher durationMatcher = durationPattern.matcher(content);
        if (durationMatcher.find()) {
            try {
                videoInfo.setDuration(Long.parseLong(durationMatcher.group(1)));
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }
        
        // 提取描述
        Pattern descPattern = Pattern.compile("\"description\":\\s*\"([^\"]+)\"");
        Matcher descMatcher = descPattern.matcher(content);
        if (descMatcher.find()) {
            videoInfo.setDescription(descMatcher.group(1));
        }
        
        // 提取缩略图
        Pattern thumbnailPattern = Pattern.compile("\"thumbnail_url\":\\s*\"([^\"]+)\"");
        Matcher thumbnailMatcher = thumbnailPattern.matcher(content);
        if (thumbnailMatcher.find()) {
            videoInfo.setThumbnail(thumbnailMatcher.group(1));
        }
    }
    
    private VideoFormat createVideoFormat(String url, String ext, String quality) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        VideoFormat format = new VideoFormat();
        format.setUrl(url);
        format.setExt(ext);
        format.setProtocol("https");
        format.setVcodec("h264");
        format.setAcodec("aac");
        
        // 设置质量
        if ("720p".equals(quality)) {
            format.setQuality(720);
            format.setResolution("720p");
        } else if ("480p".equals(quality)) {
            format.setQuality(480);
            format.setResolution("480p");
        } else if ("360p".equals(quality)) {
            format.setQuality(360);
            format.setResolution("360p");
        } else if ("240p".equals(quality)) {
            format.setQuality(240);
            format.setResolution("240p");
        } else {
            format.setQuality(720);
            format.setResolution("720p");
        }
        
        format.setFormatId(quality);
        format.setPreference(1);
        
        return format;
    }
}
