package com.btdlp.extractor.vimeo;

import com.btdlp.core.VideoInfo;
import com.btdlp.core.VideoFormat;
import com.btdlp.extractor.InfoExtractor;
import com.btdlp.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Vimeo视频提取器
 */
public class AdvancedVimeoExtractor extends InfoExtractor {
    
    private static final String TAG = "AdvancedVimeoExtractor";
    private static final String VIMEO_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    
    public AdvancedVimeoExtractor() {
        super();
    }
    
    @Override
    public String getIE_NAME() {
        return "vimeo";
    }
    
    @Override
    public String getIE_DESC() {
        return "Vimeo video extractor";
    }
    
    @Override
    public Pattern getVALID_URL() {
        return Pattern.compile(
            "(?:https?://)?(?:www\\.)?vimeo\\.com/(?:[^/]+/)*([0-9]+)"
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
        logger.info("Extracting Vimeo video: " + videoId);
        
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("Vimeo Video " + videoId);
        
        try {
            // 尝试多种提取方法
            videoInfo = tryConfigApi(url);
            if (videoInfo.getFormats() == null || videoInfo.getFormats().isEmpty()) {
                videoInfo = tryWebpageExtraction(url, videoId);
            }
            if (videoInfo.getFormats() == null || videoInfo.getFormats().isEmpty()) {
                videoInfo = tryEmbedExtraction(url);
            }
            
            return videoInfo;
            
        } catch (Exception e) {
            logger.error("Failed to extract Vimeo video: " + e.getMessage());
            throw e;
        }
    }
    
    private VideoInfo tryConfigApi(String url) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setUrl(url);
        videoInfo.setTitle("Vimeo Video");
        
        try {
            // 尝试使用Vimeo配置API
            String configUrl = url.replace("/videos/", "/player/config/");
            String response = ""; // 在实际实现中会下载API响应
            
            return parseConfigResponse(response, url);
        } catch (Exception e) {
            logger.debug("Config API extraction failed: " + e.getMessage());
        }
        
        return videoInfo;
    }
    
    private VideoInfo tryWebpageExtraction(String url, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("Vimeo Video " + videoId);
        
        try {
            String content = ""; // 在实际实现中会下载网页内容
            
            return parseWebpageContent(content, videoId);
        } catch (Exception e) {
            logger.debug("Webpage extraction failed: " + e.getMessage());
        }
        
        return videoInfo;
    }
    
    private VideoInfo tryEmbedExtraction(String url) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setUrl(url);
        videoInfo.setTitle("Vimeo Video");
        
        try {
            String embedUrl = url.replace("/videos/", "/video/");
            String content = ""; // 在实际实现中会下载嵌入页面内容
            
            return parseEmbedContent(content, url);
        } catch (Exception e) {
            logger.debug("Embed extraction failed: " + e.getMessage());
        }
        
        return videoInfo;
    }
    
    private VideoInfo parseConfigResponse(String response, String url) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setUrl(url);
        videoInfo.setTitle("Vimeo Video");
        
        // 解析配置响应
        List<VideoFormat> progressiveFormats = parseProgressiveFormatsFromConfig(response);
        List<VideoFormat> hlsFormats = parseHlsFormatsFromConfig(response);
        List<VideoFormat> dashFormats = parseDashFormatsFromConfig(response);
        
        List<VideoFormat> allFormats = new ArrayList<>();
        allFormats.addAll(progressiveFormats);
        allFormats.addAll(hlsFormats);
        allFormats.addAll(dashFormats);
        
        videoInfo.setFormats(allFormats);
        
        // 提取基本信息
        extractBasicInfo(response, videoInfo);
        
        return videoInfo;
    }
    
    private VideoInfo parseWebpageContent(String content, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setTitle("Vimeo Video " + videoId);
        
        // 解析网页内容
        List<VideoFormat> progressiveFormats = parseProgressiveFormats(content);
        List<VideoFormat> hlsFormats = parseHlsFormats(content);
        List<VideoFormat> dashFormats = parseDashFormats(content);
        
        List<VideoFormat> allFormats = new ArrayList<>();
        allFormats.addAll(progressiveFormats);
        allFormats.addAll(hlsFormats);
        allFormats.addAll(dashFormats);
        
        videoInfo.setFormats(allFormats);
        
        // 提取基本信息
        extractBasicInfo(content, videoInfo);
        
        return videoInfo;
    }
    
    private VideoInfo parseEmbedContent(String content, String url) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setUrl(url);
        videoInfo.setTitle("Vimeo Video");
        
        // 解析嵌入内容
        List<VideoFormat> formats = parseProgressiveFormats(content);
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    private List<VideoFormat> parseProgressiveFormats(String content) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 解析渐进式视频格式
        Pattern progressivePattern = Pattern.compile("\"progressive\":\\[([^\\]]+)\\]");
        Matcher progressiveMatcher = progressivePattern.matcher(content);
        
        if (progressiveMatcher.find()) {
            String progressiveContent = progressiveMatcher.group(1);
            List<String> urls = extractUrlsFromJson(progressiveContent);
            
            for (String url : urls) {
                VideoFormat format = createVideoFormat(url, "mp4", "progressive");
                if (format != null) {
                    formats.add(format);
                }
            }
        }
        
        return formats;
    }
    
    private List<VideoFormat> parseHlsFormats(String content) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 解析HLS格式
        Pattern hlsPattern = Pattern.compile("\"hls\":\\{\"cdns\":\\{([^}]+)\\}\\}");
        Matcher hlsMatcher = hlsPattern.matcher(content);
        
        while (hlsMatcher.find()) {
            String hlsContent = hlsMatcher.group(1);
            List<String> urls = extractUrlsFromJson(hlsContent);
            
            for (String url : urls) {
                if (url.contains(".m3u8")) {
                    VideoFormat format = createVideoFormat(url, "m3u8", "hls");
                    if (format != null) {
                        format.setProtocol("hls");
                        formats.add(format);
                    }
                }
            }
        }
        
        return formats;
    }
    
    private List<VideoFormat> parseDashFormats(String content) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 解析DASH格式
        Pattern dashPattern = Pattern.compile("\"dash\":\\{\"cdns\":\\{([^}]+)\\}\\}");
        Matcher dashMatcher = dashPattern.matcher(content);
        
        while (dashMatcher.find()) {
            String dashContent = dashMatcher.group(1);
            List<String> urls = extractUrlsFromJson(dashContent);
            
            for (String url : urls) {
                if (url.contains(".mpd")) {
                    VideoFormat format = createVideoFormat(url, "mpd", "dash");
                    if (format != null) {
                        format.setProtocol("dash");
                        formats.add(format);
                    }
                }
            }
        }
        
        return formats;
    }
    
    private List<String> searchVideoUrls(String content) {
        List<String> urls = new ArrayList<>();
        
        // 搜索各种可能的视频URL模式
        Pattern[] urlPatterns = {
            Pattern.compile("\"url\":\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("src=\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("\"video_url\":\"([^\"]+)\"")
        };
        
        for (Pattern pattern : urlPatterns) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String url = matcher.group(1);
                if (isValidVideoUrl(url)) {
                    urls.add(url);
                }
            }
        }
        
        return urls;
    }
    
    private boolean isValidVideoUrl(String url) {
        return url != null && !url.isEmpty() && 
               (url.contains(".mp4") || url.contains(".m3u8") || url.contains(".mpd"));
    }
    
    private List<String> extractUrlsFromJson(String jsonContent) {
        List<String> urls = new ArrayList<>();
        
        // 从JSON内容中提取URL
        Pattern urlPattern = Pattern.compile("\"url\":\"([^\"]+)\"");
        Matcher urlMatcher = urlPattern.matcher(jsonContent);
        
        while (urlMatcher.find()) {
            urls.add(urlMatcher.group(1));
        }
        
        return urls;
    }
    
    private List<VideoFormat> parseHlsFormatsFromConfig(String config) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 从配置中解析HLS格式
        Pattern hlsPattern = Pattern.compile("\"hls\":\\{[^}]*\"url\":\"([^\"]+)\"");
        Matcher hlsMatcher = hlsPattern.matcher(config);
        
        while (hlsMatcher.find()) {
            String url = hlsMatcher.group(1);
            VideoFormat format = createVideoFormat(url, "m3u8", "hls");
            if (format != null) {
                format.setProtocol("hls");
                formats.add(format);
            }
        }
        
        return formats;
    }
    
    private List<VideoFormat> parseDashFormatsFromConfig(String config) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 从配置中解析DASH格式
        Pattern dashPattern = Pattern.compile("\"dash\":\\{[^}]*\"url\":\"([^\"]+)\"");
        Matcher dashMatcher = dashPattern.matcher(config);
        
        while (dashMatcher.find()) {
            String url = dashMatcher.group(1);
            VideoFormat format = createVideoFormat(url, "mpd", "dash");
            if (format != null) {
                format.setProtocol("dash");
                formats.add(format);
            }
        }
        
        return formats;
    }
    
    private List<VideoFormat> parseProgressiveFormatsFromConfig(String config) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 从配置中解析渐进式格式
        Pattern progressivePattern = Pattern.compile("\"progressive\":\\[[^\\]]*\"url\":\"([^\"]+)\"");
        Matcher progressiveMatcher = progressivePattern.matcher(config);
        
        while (progressiveMatcher.find()) {
            String url = progressiveMatcher.group(1);
            VideoFormat format = createVideoFormat(url, "mp4", "progressive");
            if (format != null) {
                formats.add(format);
            }
        }
        
        return formats;
    }
    
    private Integer parseQuality(String url) {
        // 从URL中解析质量信息
        if (url.contains("1080p")) return 1080;
        if (url.contains("720p")) return 720;
        if (url.contains("540p")) return 540;
        if (url.contains("480p")) return 480;
        if (url.contains("360p")) return 360;
        if (url.contains("240p")) return 240;
        
        return 720; // 默认质量
    }
    
    private void extractBasicInfo(String content, VideoInfo videoInfo) {
        // 提取标题
        Pattern titlePattern = Pattern.compile("\"title\":\"([^\"]+)\"");
        Matcher titleMatcher = titlePattern.matcher(content);
        if (titleMatcher.find()) {
            videoInfo.setTitle(titleMatcher.group(1));
        }
        
        // 提取描述
        Pattern descPattern = Pattern.compile("\"description\":\"([^\"]+)\"");
        Matcher descMatcher = descPattern.matcher(content);
        if (descMatcher.find()) {
            videoInfo.setDescription(descMatcher.group(1));
        }
        
        // 提取时长
        Pattern durationPattern = Pattern.compile("\"duration\":([0-9]+)");
        Matcher durationMatcher = durationPattern.matcher(content);
        if (durationMatcher.find()) {
            try {
                videoInfo.setDuration(Long.parseLong(durationMatcher.group(1)));
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }
    }
    
    private VideoFormat createVideoFormat(String url, String ext, String formatType) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        VideoFormat format = new VideoFormat();
        format.setUrl(url);
        format.setExt(ext);
        format.setFormat(formatType);
        format.setProtocol("https");
        
        // 设置质量
        Integer quality = parseQuality(url);
        format.setQuality(quality);
        format.setResolution(quality + "p");
        
        // 设置编解码器
        format.setVcodec("h264");
        format.setAcodec("aac");
        
        format.setFormatId(formatType);
        format.setPreference(1);
        
        return format;
    }
}
