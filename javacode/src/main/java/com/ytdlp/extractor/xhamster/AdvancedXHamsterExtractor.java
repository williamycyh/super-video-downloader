package com.ytdlp.extractor.xhamster;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.utils.Logger;
import com.ytdlp.utils.XHamsterUtils;
import com.ytdlp.utils.EnhancedHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * XHamster视频提取器
 */
public class AdvancedXHamsterExtractor extends InfoExtractor {
    
    private EnhancedHttpClient httpClient;
    
    public AdvancedXHamsterExtractor() {
        super();
        this.httpClient = new EnhancedHttpClient();
    }
    
    @Override
    public String getIE_NAME() {
        return "xhamster";
    }
    
    @Override
    public String getIE_DESC() {
        return "XHamster video extractor";
    }
    
    @Override
    public Pattern getVALID_URL() {
        return Pattern.compile(XHamsterUtils.VALID_URL_REGEX);
    }
    
    @Override
    public boolean suitable(String url) {
        return XHamsterUtils.VALID_URL_PATTERN.matcher(url).matches();
    }
    
    @Override
    public VideoInfo extract(String url) {
        try {
            String videoId = extractVideoId(url);
            return realExtract(url, videoId);
        } catch (Exception e) {
            logger.error("Failed to extract XHamster video: " + e.getMessage());
            return null;
        }
    }
    
    @Override
    protected String extractVideoId(String url) {
        Map<String, String> urlInfo = XHamsterUtils.parseUrlKey(url);
        return urlInfo.get("id");
    }
    
    @Override
    protected VideoInfo realExtract(String url, String videoId) throws Exception {
        logger.info("Extracting XHamster video: " + videoId);
        
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("XHamster Video " + videoId);
        
        try {
            // 模拟下载网页内容
            String content = ""; // 在实际实现中会下载网页内容
            
            // 提取视频信息
            XHamsterUtils.XHamsterVideoInfo xhInfo = XHamsterUtils.extractVideoInfo(content);
            videoInfo = realExtract(url, xhInfo);
            
            return videoInfo;
            
        } catch (Exception e) {
            logger.error("Failed to extract XHamster video: " + e.getMessage());
            throw e;
        }
    }
    
    private VideoInfo realExtract(String url, XHamsterUtils.XHamsterVideoInfo xhInfo) throws Exception {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setUrl(url);
        
        // 设置基本信息
        if (xhInfo.getTitle() != null) {
            videoInfo.setTitle(xhInfo.getTitle());
        }
        
        if (xhInfo.getDuration() != null) {
            videoInfo.setDuration(xhInfo.getDuration());
        }
        
        // 提取视频格式
        List<VideoFormat> formats = extractFormatsFromVideoUrls(xhInfo.getVideoUrls());
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    private String extractError(String content) {
        Pattern errorPattern = Pattern.compile("<h1[^>]*>([^<]+)</h1>");
        Matcher errorMatcher = errorPattern.matcher(content);
        
        if (errorMatcher.find()) {
            String errorText = errorMatcher.group(1).trim();
            if (errorText.toLowerCase().contains("error") || 
                errorText.toLowerCase().contains("not found")) {
                return errorText;
            }
        }
        
        return null;
    }
    
    private String extractAgeLimit(String content) {
        if (content.contains("age_verification") || content.contains("age-restricted")) {
            return "18";
        }
        return null;
    }
    
    private VideoInfo extractFromInitials(String content, String videoId, String url, String webpage) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        
        // 提取基本信息
        extractBasicInfo(content, videoInfo);
        
        // 提取视频格式
        List<VideoFormat> formats = extractFormatsFromInitials(content, videoId);
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    private List<VideoFormat> extractFormatsFromInitials(String content, String videoId) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 查找视频URL
        Pattern urlPattern = Pattern.compile("\"url\":\\s*\"([^\"]+\\.mp4[^\"]*)\"");
        Matcher urlMatcher = urlPattern.matcher(content);
        
        while (urlMatcher.find()) {
            String url = urlMatcher.group(1);
            VideoFormat format = createVideoFormat(url);
            if (format != null) {
                formats.add(format);
            }
        }
        
        return formats;
    }
    
    private VideoInfo extractFromOldLayout(String content, String videoId, String url, String webpage) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        
        // 提取基本信息
        extractBasicInfo(content, videoInfo);
        
        // 提取视频格式
        List<VideoFormat> formats = extractFormatsFromOldLayout(content);
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    private String extractTitle(String content) {
        Pattern titlePattern = Pattern.compile("<title>([^<]+)</title>");
        Matcher titleMatcher = titlePattern.matcher(content);
        
        if (titleMatcher.find()) {
            String title = titleMatcher.group(1).trim();
            title = title.replaceAll("\\s*\\|\\s*XHamster.*$", "");
            return title;
        }
        
        return null;
    }
    
    private String extractDescription(String content) {
        Pattern descPattern = Pattern.compile("\"description\":\\s*\"([^\"]+)\"");
        Matcher descMatcher = descPattern.matcher(content);
        
        if (descMatcher.find()) {
            return descMatcher.group(1);
        }
        
        return null;
    }
    
    private String extractUploadDate(String content) {
        Pattern datePattern = Pattern.compile("\"uploadDate\":\\s*\"([^\"]+)\"");
        Matcher dateMatcher = datePattern.matcher(content);
        
        if (dateMatcher.find()) {
            return dateMatcher.group(1);
        }
        
        return null;
    }
    
    private String extractUploader(String content) {
        Pattern uploaderPattern = Pattern.compile("\"uploader\":\\s*\"([^\"]+)\"");
        Matcher uploaderMatcher = uploaderPattern.matcher(content);
        
        if (uploaderMatcher.find()) {
            return uploaderMatcher.group(1);
        }
        
        return null;
    }
    
    private String extractThumbnail(String content) {
        Pattern thumbnailPattern = Pattern.compile("\"thumbnail\":\\s*\"([^\"]+)\"");
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
    
    private Long[] extractLikeDislikeCount(String content) {
        Long[] counts = new Long[2];
        
        Pattern likePattern = Pattern.compile("\"likeCount\":\\s*(\\d+)");
        Matcher likeMatcher = likePattern.matcher(content);
        if (likeMatcher.find()) {
            try {
                counts[0] = Long.parseLong(likeMatcher.group(1));
            } catch (NumberFormatException e) {
                counts[0] = null;
            }
        }
        
        Pattern dislikePattern = Pattern.compile("\"dislikeCount\":\\s*(\\d+)");
        Matcher dislikeMatcher = dislikePattern.matcher(content);
        if (dislikeMatcher.find()) {
            try {
                counts[1] = Long.parseLong(dislikeMatcher.group(1));
            } catch (NumberFormatException e) {
                counts[1] = null;
            }
        }
        
        return counts;
    }
    
    private Long extractCommentCount(String content) {
        Pattern commentPattern = Pattern.compile("\"commentCount\":\\s*(\\d+)");
        Matcher commentMatcher = commentPattern.matcher(content);
        
        if (commentMatcher.find()) {
            try {
                return Long.parseLong(commentMatcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    private List<String> extractCategories(String content) {
        List<String> categories = new ArrayList<>();
        
        Pattern categoryPattern = Pattern.compile("\"category\":\\s*\"([^\"]+)\"");
        Matcher categoryMatcher = categoryPattern.matcher(content);
        
        while (categoryMatcher.find()) {
            categories.add(categoryMatcher.group(1));
        }
        
        return categories;
    }
    
    private List<VideoFormat> extractFormatsFromOldLayout(String content) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 查找各种可能的视频URL模式
        Pattern[] urlPatterns = {
            Pattern.compile("\"videoUrl\":\\s*\"([^\"]+)\""),
            Pattern.compile("\"url\":\\s*\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("src=\"([^\"]+\\.mp4[^\"]*)\"")
        };
        
        for (Pattern pattern : urlPatterns) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String url = matcher.group(1);
                VideoFormat format = createVideoFormat(url);
                if (format != null) {
                    formats.add(format);
                }
            }
        }
        
        return formats;
    }
    
    private List<VideoFormat> extractFormatsFromVideoUrls(List<String> videoUrls) {
        List<VideoFormat> formats = new ArrayList<>();
        
        for (String url : videoUrls) {
            VideoFormat format = createVideoFormat(url);
            if (format != null) {
                formats.add(format);
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
        
        // 提取描述
        String description = extractDescription(content);
        if (description != null) {
            videoInfo.setDescription(description);
        }
        
        // 提取上传者
        String uploader = extractUploader(content);
        if (uploader != null) {
            videoInfo.setUploader(uploader);
        }
        
        // 提取时长
        Long duration = extractDuration(content);
        if (duration != null) {
            videoInfo.setDuration(duration);
        }
        
        // 提取缩略图
        String thumbnail = extractThumbnail(content);
        if (thumbnail != null) {
            videoInfo.setThumbnail(thumbnail);
        }
        
        // 提取观看次数
        Long viewCount = extractViewCount(content);
        if (viewCount != null) {
            videoInfo.setViewCount(viewCount);
        }
    }
    
    private VideoFormat createVideoFormat(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        VideoFormat format = new VideoFormat();
        format.setUrl(url);
        format.setProtocol("https");
        format.setExt("mp4");
        format.setVcodec("h264");
        format.setAcodec("aac");
        
        // 设置质量
        Integer height = XHamsterUtils.getHeight(url);
        if (height != null) {
            format.setQuality(height);
            format.setResolution(height + "p");
        } else {
            format.setQuality(720);
            format.setResolution("720p");
        }
        
        format.setFormatId("default");
        format.setPreference(1);
        
        return format;
    }
}
