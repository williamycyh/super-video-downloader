package com.ytdlp.extractor.instagram;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.utils.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Instagram视频提取器
 */
public class InstagramExtractor extends InfoExtractor {
    
    private static final String TAG = "InstagramExtractor";
    private static final String INSTAGRAM_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    
    public InstagramExtractor() {
        super();
    }
    
    @Override
    public String getIE_NAME() {
        return "instagram";
    }
    
    @Override
    public String getIE_DESC() {
        return "Instagram video extractor";
    }
    
    @Override
    public Pattern getVALID_URL() {
        return Pattern.compile(
            "(?:https?://)?(?:www\\.)?instagram\\.com/(?:p|reel)/([^/]+)/?"
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
        logger.info("Extracting Instagram video: " + videoId);
        
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        
        try {
            // 尝试多种提取方法
            videoInfo = tryApiExtraction(url, videoId);
            if (videoInfo.getFormats() == null || videoInfo.getFormats().isEmpty()) {
                videoInfo = tryWebpageExtraction(url, videoId);
            }
            if (videoInfo.getFormats() == null || videoInfo.getFormats().isEmpty()) {
                videoInfo = tryGraphQLExtraction(url, videoId);
            }
            
            return videoInfo;
            
        } catch (Exception e) {
            logger.error("Failed to extract Instagram video: " + e.getMessage());
            throw e;
        }
    }
    
    private VideoInfo tryApiExtraction(String url, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("Instagram Video " + videoId);
        
        // 尝试使用Instagram API提取
        try {
            String apiUrl = "https://www.instagram.com/p/" + videoId + "/?__a=1";
            String response = downloadWebpage(apiUrl);
            
            if (response != null && !response.isEmpty()) {
                return parseApiResponse(response, videoId);
            }
        } catch (Exception e) {
            logger.debug("API extraction failed: " + e.getMessage());
        }
        
        return videoInfo;
    }
    
    private VideoInfo tryWebpageExtraction(String url, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("Instagram Video " + videoId);
        
        try {
            String webpage = downloadWebpage(url);
            if (webpage != null && !webpage.isEmpty()) {
                return parseWebpageData(webpage, videoId);
            }
        } catch (Exception e) {
            logger.debug("Webpage extraction failed: " + e.getMessage());
        }
        
        return videoInfo;
    }
    
    private VideoInfo tryGraphQLExtraction(String url, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("Instagram Video " + videoId);
        
        try {
            String graphqlUrl = "https://www.instagram.com/graphql/query/";
            String response = downloadWebpage(graphqlUrl);
            
            if (response != null && !response.isEmpty()) {
                return parseGraphQLResponse(response, videoId);
            }
        } catch (Exception e) {
            logger.debug("GraphQL extraction failed: " + e.getMessage());
        }
        
        return videoInfo;
    }
    
    protected String downloadWebpage(String url) throws IOException {
        // 简化的网页下载实现
        // 在实际实现中，这里会使用HTTP客户端下载网页内容
        return "";
    }
    
    private VideoInfo parseApiResponse(String response, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setTitle("Instagram Video " + videoId);
        
        // 解析API响应中的视频信息
        List<VideoFormat> formats = parseVideoVersions(response);
        videoInfo.setFormats(formats);
        
        // 提取基本信息
        extractBasicInfo(response, videoInfo);
        
        return videoInfo;
    }
    
    private VideoInfo parseSharedData(String sharedData, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setTitle("Instagram Video " + videoId);
        
        // 解析共享数据中的视频信息
        List<VideoFormat> formats = parseVideoVersions(sharedData);
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    private VideoInfo parseWebpageData(String webpage, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setTitle("Instagram Video " + videoId);
        
        // 从网页中提取视频信息
        List<VideoFormat> formats = parseVideoVersions(webpage);
        videoInfo.setFormats(formats);
        
        // 提取基本信息
        extractBasicInfo(webpage, videoInfo);
        
        return videoInfo;
    }
    
    private VideoInfo parseGraphQLResponse(String response, String videoId) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setTitle("Instagram Video " + videoId);
        
        // 解析GraphQL响应
        List<VideoFormat> formats = parseVideoVersions(response);
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    private List<VideoFormat> parseVideoVersions(String content) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 查找视频URL模式
        Pattern[] urlPatterns = {
            Pattern.compile("\"video_url\":\"([^\"]+)\""),
            Pattern.compile("\"url\":\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("src=\"([^\"]+\\.mp4[^\"]*)\"")
        };
        
        for (Pattern pattern : urlPatterns) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String url = matcher.group(1);
                VideoFormat format = createVideoFormat(url, "mp4", "video");
                if (format != null) {
                    formats.add(format);
                }
            }
        }
        
        return formats;
    }
    
    private void extractBasicInfo(String content, VideoInfo videoInfo) {
        // 提取标题
        Pattern titlePattern = Pattern.compile("\"caption\":\"([^\"]+)\"");
        Matcher titleMatcher = titlePattern.matcher(content);
        if (titleMatcher.find()) {
            String caption = titleMatcher.group(1);
            if (caption.length() > 100) {
                caption = caption.substring(0, 100) + "...";
            }
            videoInfo.setTitle(caption);
        }
        
        // 提取描述
        Pattern descPattern = Pattern.compile("\"description\":\"([^\"]+)\"");
        Matcher descMatcher = descPattern.matcher(content);
        if (descMatcher.find()) {
            videoInfo.setDescription(descMatcher.group(1));
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
        
        // 设置默认质量
        format.setQuality(720);
        format.setResolution("720p");
        format.setVcodec("h264");
        
        format.setFormatId("default");
        format.setPreference(1);
        
        return format;
    }
    
    private String simpleJsonEncode(Map<String, Object> map) {
        // 简化的JSON编码实现
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value);
            }
            
            first = false;
        }
        
        json.append("}");
        return json.toString();
    }
}
