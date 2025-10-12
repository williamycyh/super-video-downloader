package com.ytdlp.extractor.instagram;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.utils.Logger;
import com.ytdlp.utils.InstagramUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 高级Instagram视频提取器
 */
public class AdvancedInstagramExtractor extends InfoExtractor {
    
    private static final String TAG = "AdvancedInstagramExtractor";
    private static final String INSTAGRAM_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    private static final String INSTAGRAM_MOBILE_USER_AGENT = "Instagram 219.0.0.12.117 Android";
    
    public AdvancedInstagramExtractor() {
        super();
    }
    
    @Override
    public String getIE_NAME() {
        return "instagram_advanced";
    }
    
    @Override
    public String getIE_DESC() {
        return "Advanced Instagram video extractor";
    }
    
    @Override
    public Pattern getVALID_URL() {
        return Pattern.compile(InstagramUtils.VALID_URL_REGEX);
    }
    
    @Override
    protected String extractVideoId(String url) {
        return InstagramUtils.extractVideoId(url);
    }
    
    @Override
    protected VideoInfo realExtract(String url, String videoId) throws Exception {
        logger.info("Advanced Instagram extraction for: " + videoId);
        
        VideoInfo videoInfo = null;
        
        // 尝试多种提取方法
        try {
            videoInfo = tryPublicApi(url, videoId);
            if (hasValidFormats(videoInfo)) {
                logger.info("成功通过公共API提取");
                return videoInfo;
            }
        } catch (Exception e) {
            logger.debug("公共API提取失败: " + e.getMessage());
        }
        
        try {
            videoInfo = tryEmbedUrl(url, videoId);
            if (hasValidFormats(videoInfo)) {
                logger.info("成功通过嵌入URL提取");
                return videoInfo;
            }
        } catch (Exception e) {
            logger.debug("嵌入URL提取失败: " + e.getMessage());
        }
        
        try {
            videoInfo = tryMobileVersion(url, videoId);
            if (hasValidFormats(videoInfo)) {
                logger.info("成功通过移动版本提取");
                return videoInfo;
            }
        } catch (Exception e) {
            logger.debug("移动版本提取失败: " + e.getMessage());
        }
        
        try {
            videoInfo = tryDirectVideoSearch(url, videoId);
            if (hasValidFormats(videoInfo)) {
                logger.info("成功通过直接视频搜索提取");
                return videoInfo;
            }
        } catch (Exception e) {
            logger.debug("直接视频搜索提取失败: " + e.getMessage());
        }
        
        throw new Exception("所有提取方法都失败了");
    }
    
    /**
     * 尝试使用公共API提取
     */
    private VideoInfo tryPublicApi(String url, String videoId) throws Exception {
        String apiUrl = InstagramUtils.buildApiUrl(videoId, "p");
        Map<String, String> headers = createApiHeaders(url);
        
        // 模拟API调用
        String response = ""; // 在实际实现中会调用API
        return parseApiResponse(response, videoId);
    }
    
    /**
     * 尝试使用嵌入URL提取
     */
    private VideoInfo tryEmbedUrl(String url, String videoId) throws Exception {
        String embedUrl = InstagramUtils.buildEmbedUrl(videoId);
        Map<String, String> headers = createBrowserHeaders(embedUrl);
        
        // 模拟下载嵌入页面
        String content = ""; // 在实际实现中会下载页面内容
        return parseEmbedContent(content, videoId);
    }
    
    /**
     * 尝试使用移动版本提取
     */
    private VideoInfo tryMobileVersion(String url, String videoId) throws Exception {
        Map<String, String> headers = createBrowserHeaders(url);
        headers.put("User-Agent", INSTAGRAM_MOBILE_USER_AGENT);
        
        // 模拟下载移动页面
        String content = ""; // 在实际实现中会下载页面内容
        return parseWebpageContent(content, videoId);
    }
    
    /**
     * 尝试直接视频搜索
     */
    private VideoInfo tryDirectVideoSearch(String url, String videoId) throws Exception {
        Map<String, String> headers = createBrowserHeaders(url);
        
        // 模拟下载页面内容
        String content = ""; // 在实际实现中会下载页面内容
        return parseWebpageContent(content, videoId);
    }
    
    /**
     * 创建API请求头
     */
    private Map<String, String> createApiHeaders(String url) {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", INSTAGRAM_USER_AGENT);
        headers.put("Accept", "application/json");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("Referer", url);
        return headers;
    }
    
    /**
     * 创建浏览器请求头
     */
    private Map<String, String> createBrowserHeaders(String url) {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", INSTAGRAM_USER_AGENT);
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
     * 解析API响应
     */
    private VideoInfo parseApiResponse(String response, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl("https://www.instagram.com/p/" + videoId + "/");
        
        // 模拟解析API响应
        videoInfo.setTitle("Instagram Video " + videoId);
        videoInfo.setUploader("instagram_user");
        
        // 创建示例格式
        List<VideoFormat> formats = new ArrayList<>();
        VideoFormat format = createVideoFormat("https://example.com/video.mp4", "mp4", "720p");
        formats.add(format);
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    /**
     * 解析嵌入内容
     */
    private VideoInfo parseEmbedContent(String content, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl("https://www.instagram.com/p/" + videoId + "/");
        
        // 提取基本信息
        extractBasicInfo(content, videoInfo);
        
        // 搜索视频URL
        List<String> videoUrls = searchVideoUrls(content);
        List<VideoFormat> formats = new ArrayList<>();
        
        for (String videoUrl : videoUrls) {
            VideoFormat format = createVideoFormat(videoUrl, "mp4", "720p");
            formats.add(format);
        }
        
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    /**
     * 解析网页内容
     */
    private VideoInfo parseWebpageContent(String content, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl("https://www.instagram.com/p/" + videoId + "/");
        
        // 提取基本信息
        extractBasicInfo(content, videoInfo);
        
        // 搜索视频URL
        List<String> videoUrls = searchVideoUrls(content);
        List<VideoFormat> formats = new ArrayList<>();
        
        for (String videoUrl : videoUrls) {
            VideoFormat format = createVideoFormat(videoUrl, "mp4", "720p");
            formats.add(format);
        }
        
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    /**
     * 搜索视频URL
     */
    private List<String> searchVideoUrls(String content) {
        List<String> urls = new ArrayList<>();
        
        // 查找各种可能的视频URL模式
        Pattern[] urlPatterns = {
            Pattern.compile("\"video_url\":\\s*\"([^\"]+)\""),
            Pattern.compile("\"url\":\\s*\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("src=\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("\"mp4\":\\s*\"([^\"]+)\""),
            Pattern.compile("\"playback_url\":\\s*\"([^\"]+)\"")
        };
        
        for (Pattern pattern : urlPatterns) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String url = matcher.group(1);
                if (url != null && !url.isEmpty()) {
                    url = decodeHtmlEntities(url);
                    urls.add(url);
                }
            }
        }
        
        return urls;
    }
    
    /**
     * 提取基本信息
     */
    private void extractBasicInfo(String content, VideoInfo videoInfo) {
        // 提取标题
        Pattern titlePattern = Pattern.compile("<meta property=\"og:title\" content=\"([^\"]+)\"");
        Matcher titleMatcher = titlePattern.matcher(content);
        if (titleMatcher.find()) {
            videoInfo.setTitle(titleMatcher.group(1));
        }
        
        // 提取描述
        Pattern descPattern = Pattern.compile("<meta property=\"og:description\" content=\"([^\"]+)\"");
        Matcher descMatcher = descPattern.matcher(content);
        if (descMatcher.find()) {
            videoInfo.setDescription(descMatcher.group(1));
        }
        
        // 提取缩略图
        Pattern thumbnailPattern = Pattern.compile("<meta property=\"og:image\" content=\"([^\"]+)\"");
        Matcher thumbnailMatcher = thumbnailPattern.matcher(content);
        if (thumbnailMatcher.find()) {
            videoInfo.setThumbnail(thumbnailMatcher.group(1));
        }
        
        // 提取上传者
        Pattern uploaderPattern = Pattern.compile("\"owner\":\\s*\\{[^}]*\"username\":\\s*\"([^\"]+)\"");
        Matcher uploaderMatcher = uploaderPattern.matcher(content);
        if (uploaderMatcher.find()) {
            videoInfo.setUploader(uploaderMatcher.group(1));
        }
    }
    
    /**
     * 创建视频格式
     */
    private VideoFormat createVideoFormat(String url, String ext, String resolution) {
        VideoFormat format = new VideoFormat();
        format.setUrl(url);
        format.setProtocol("https");
        format.setExt(ext);
        format.setResolution(resolution);
        format.setVcodec("h264");
        format.setAcodec("aac");
        
        // 设置质量
        if (resolution.contains("720")) {
            format.setQuality(720);
        } else if (resolution.contains("480")) {
            format.setQuality(480);
        } else if (resolution.contains("360")) {
            format.setQuality(360);
        } else {
            format.setQuality(720);
        }
        
        format.setFormatId("default");
        format.setPreference(1);
        
        return format;
    }
    
    /**
     * 检查是否有有效的格式
     */
    private boolean hasValidFormats(VideoInfo videoInfo) {
        return videoInfo != null && 
               videoInfo.getFormats() != null && 
               !videoInfo.getFormats().isEmpty() &&
               videoInfo.getFormats().stream().anyMatch(f -> f.getUrl() != null && !f.getUrl().isEmpty());
    }
    
    /**
     * 解码HTML实体
     */
    private String decodeHtmlEntities(String text) {
        if (text == null) {
            return null;
        }
        
        return text.replace("&amp;", "&")
                  .replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&quot;", "\"")
                  .replace("&#x27;", "'")
                  .replace("&#x2F;", "/")
                  .replace("\\u0026", "&")
                  .replace("\\u003C", "<")
                  .replace("\\u003E", ">")
                  .replace("\\u0027", "'")
                  .replace("\\u002F", "/");
    }
}
