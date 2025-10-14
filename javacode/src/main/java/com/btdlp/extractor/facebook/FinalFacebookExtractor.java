package com.btdlp.extractor.facebook;

import com.btdlp.core.VideoInfo;
import com.btdlp.core.VideoFormat;
import com.btdlp.extractor.InfoExtractor;
import com.btdlp.utils.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Facebook视频提取器
 */
public class FinalFacebookExtractor extends InfoExtractor {
    
    private static final String TAG = "FinalFacebookExtractor";
    private static final String FACEBOOK_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    
    public FinalFacebookExtractor() {
        super();
    }
    
    @Override
    public String getIE_NAME() {
        return "facebook";
    }
    
    @Override
    public String getIE_DESC() {
        return "Facebook video extractor";
    }
    
    @Override
    public Pattern getVALID_URL() {
        return Pattern.compile(
            "(?:https?://)?(?:www\\.)?facebook\\.com/(?:[^/]+/videos/|watch/\\?v=)([0-9]+)"
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
        logger.info("Extracting Facebook video: " + videoId);
        
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        
        try {
            // 下载网页内容
            String webpage = downloadWebpageWithUserAgent(url);
            
            // 提取基本信息
            extractBasicInfo(webpage, videoInfo);
            
            // 提取视频格式
            List<VideoFormat> formats = extractVideoFormatsFromScheduledServerJS(webpage, videoId);
            if (formats.isEmpty()) {
                formats = findAlternativeFormats(webpage);
            }
            videoInfo.setFormats(formats);
            
            return videoInfo;
            
        } catch (Exception e) {
            logger.error("Failed to extract Facebook video: " + e.getMessage());
            throw e;
        }
    }
    
    private String downloadWebpageWithUserAgent(String url) throws IOException {
        // 简化的网页下载实现
        // 在实际实现中，这里会使用HTTP客户端下载网页内容
        return "";
    }
    
    private void extractBasicInfo(String webpage, VideoInfo videoInfo) {
        // 从网页内容中提取基本信息
        // 这里使用正则表达式提取标题、描述等信息
        
        // 提取标题
        Pattern titlePattern = Pattern.compile("<title>([^<]+)</title>");
        Matcher titleMatcher = titlePattern.matcher(webpage);
        if (titleMatcher.find()) {
            videoInfo.setTitle(titleMatcher.group(1).trim());
        } else {
            videoInfo.setTitle("Facebook Video " + videoInfo.getId());
        }
        
        // 提取描述
        Pattern descPattern = Pattern.compile("<meta name=\"description\" content=\"([^\"]+)\"");
        Matcher descMatcher = descPattern.matcher(webpage);
        if (descMatcher.find()) {
            videoInfo.setDescription(descMatcher.group(1));
        }
    }
    
    private List<VideoFormat> extractVideoFormatsFromScheduledServerJS(String webpage, String videoId) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 从JavaScript代码中提取视频URL
        Pattern jsPattern = Pattern.compile("scheduled_server_data.*?video_url.*?\"([^\"]+)\"");
        Matcher jsMatcher = jsPattern.matcher(webpage);
        
        while (jsMatcher.find()) {
            String videoUrl = jsMatcher.group(1);
            VideoFormat format = createVideoFormatFromUrl(videoUrl);
            if (format != null) {
                formats.add(format);
            }
        }
        
        return formats;
    }
    
    private VideoFormat createVideoFormatFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        VideoFormat format = new VideoFormat();
        format.setUrl(url);
        format.setProtocol("https");
        
        // 根据URL确定格式
        if (url.contains(".mp4")) {
            format.setExt("mp4");
            format.setVcodec("h264");
        } else if (url.contains(".m3u8")) {
            format.setExt("m3u8");
            format.setProtocol("hls");
        }
        
        // 设置质量信息
        if (url.contains("hd")) {
            format.setQuality(720);
            format.setResolution("720p");
        } else if (url.contains("sd")) {
            format.setQuality(480);
            format.setResolution("480p");
        } else {
            format.setQuality(360);
            format.setResolution("360p");
        }
        
        format.setFormatId("default");
        format.setPreference(1);
        
        return format;
    }
    
    private List<VideoFormat> findAlternativeFormats(String webpage) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 寻找其他可能的视频URL模式
        Pattern[] urlPatterns = {
            Pattern.compile("\"video_url\":\"([^\"]+)\""),
            Pattern.compile("\"src\":\"([^\"]+\\.mp4[^\"]*)\""),
            Pattern.compile("video_src=\"([^\"]+)\"")
        };
        
        for (Pattern pattern : urlPatterns) {
            Matcher matcher = pattern.matcher(webpage);
            while (matcher.find()) {
                String url = matcher.group(1);
                VideoFormat format = createVideoFormatFromUrl(url);
                if (format != null) {
                    formats.add(format);
                }
            }
        }
        
        return formats;
    }
}
