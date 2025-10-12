package com.ytdlp.extractor.tiktok;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * TikTok视频提取器
 */
public class AdvancedTikTokExtractor extends InfoExtractor {
    
    private static final String TAG = "AdvancedTikTokExtractor";
    private static final String TIKTOK_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    private static final String TIKTOK_MOBILE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0.3 Mobile/15E148 Safari/604.1";
    
    public AdvancedTikTokExtractor() {
        super();
    }
    
    @Override
    public String getIE_NAME() {
        return "tiktok";
    }
    
    @Override
    public String getIE_DESC() {
        return "TikTok video extractor";
    }
    
    @Override
    public Pattern getVALID_URL() {
        return Pattern.compile(
            "(?:https?://)?(?:www\\.)?(?:vm\\.)?tiktok\\.com/(?:@[^/]+/video/|t/[^/]+/)([0-9]+)"
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
        logger.info("Extracting TikTok video: " + videoId);
        
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("TikTok Video " + videoId);
        
        try {
            // 尝试多种提取方法
            String cleanUrl = url.replace("/vm/", "/").replace("/t/", "/");
            
            videoInfo = tryWebpageExtraction(cleanUrl, videoId, "desktop");
            if (videoInfo.getFormats() == null || videoInfo.getFormats().isEmpty()) {
                videoInfo = tryEmbedExtraction(url);
            }
            if (videoInfo.getFormats() == null || videoInfo.getFormats().isEmpty()) {
                videoInfo = tryMobileExtraction(cleanUrl);
            }
            
            return videoInfo;
            
        } catch (Exception e) {
            logger.error("Failed to extract TikTok video: " + e.getMessage());
            throw e;
        }
    }
    
    private VideoInfo tryWebpageExtraction(String url, String videoId, String userAgent) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        videoInfo.setTitle("TikTok Video " + videoId);
        
        try {
            // 模拟下载网页内容
            String content = ""; // 在实际实现中会下载网页内容
            
            return parseWebpageContent(content, videoId, userAgent);
        } catch (Exception e) {
            logger.debug("Webpage extraction failed: " + e.getMessage());
        }
        
        return videoInfo;
    }
    
    private VideoInfo tryEmbedExtraction(String url) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setUrl(url);
        
        try {
            String embedUrl = url.replace("tiktok.com", "tiktok.com/embed");
            String content = ""; // 在实际实现中会下载嵌入页面内容
            
            return parseEmbedContent(content, url);
        } catch (Exception e) {
            logger.debug("Embed extraction failed: " + e.getMessage());
        }
        
        return videoInfo;
    }
    
    private VideoInfo tryMobileExtraction(String url) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setUrl(url);
        
        try {
            String mobileUrl = url.replace("www.", "m.");
            String content = ""; // 在实际实现中会下载移动页面内容
            
            return parseMobileApiResponse(content, url);
        } catch (Exception e) {
            logger.debug("Mobile extraction failed: " + e.getMessage());
        }
        
        return videoInfo;
    }
    
    private VideoInfo parseWebpageContent(String content, String videoId, String userAgent) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setTitle("TikTok Video " + videoId);
        
        // 解析网页内容中的视频信息
        List<VideoFormat> formats = parseJsonData(content, videoId);
        videoInfo.setFormats(formats);
        
        // 提取基本信息
        extractBasicInfo(content, videoInfo);
        
        return videoInfo;
    }
    
    private VideoInfo parseEmbedContent(String content, String url) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setUrl(url);
        videoInfo.setTitle("TikTok Video");
        
        // 从嵌入内容中解析视频信息
        List<VideoFormat> formats = parseJsonData(content, "");
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    private VideoInfo parseMobileApiResponse(String content, String url) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setUrl(url);
        videoInfo.setTitle("TikTok Video");
        
        // 解析移动API响应
        List<VideoFormat> formats = parseJsonData(content, "");
        videoInfo.setFormats(formats);
        
        return videoInfo;
    }
    
    private List<VideoFormat> parseJsonData(String content, String videoId) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 尝试解析比特率信息
        List<VideoFormat> bitrateFormats = parseBitrateInfo(content);
        if (!bitrateFormats.isEmpty()) {
            formats.addAll(bitrateFormats);
        }
        
        // 尝试解析播放地址
        List<VideoFormat> playAddrFormats = parsePlayAddr(content);
        if (!playAddrFormats.isEmpty()) {
            formats.addAll(playAddrFormats);
        }
        
        // 搜索视频URL
        List<String> urls = searchVideoUrls(content);
        for (String url : urls) {
            VideoFormat format = createVideoFormat(url, "mp4", "video");
            if (format != null) {
                formats.add(format);
            }
        }
        
        return formats;
    }
    
    private List<VideoFormat> parseBitrateInfo(String content) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 解析比特率信息
        Pattern bitratePattern = Pattern.compile("\"bitrate\":([0-9]+)");
        Matcher bitrateMatcher = bitratePattern.matcher(content);
        
        while (bitrateMatcher.find()) {
            String bitrate = bitrateMatcher.group(1);
            // 根据比特率创建格式
            VideoFormat format = new VideoFormat();
            format.setAbr(Integer.parseInt(bitrate));
            format.setExt("mp4");
            format.setVcodec("h264");
            format.setFormatId("bitrate_" + bitrate);
            formats.add(format);
        }
        
        return formats;
    }
    
    private List<VideoFormat> parsePlayAddr(String content) {
        List<VideoFormat> formats = new ArrayList<>();
        
        // 解析播放地址
        Pattern playAddrPattern = Pattern.compile("\"playAddr\":\"([^\"]+)\"");
        Matcher playAddrMatcher = playAddrPattern.matcher(content);
        
        while (playAddrMatcher.find()) {
            String playAddr = playAddrMatcher.group(1);
            VideoFormat format = createVideoFormat(playAddr, "mp4", "video");
            if (format != null) {
                format.setFormatId("playaddr");
                formats.add(format);
            }
        }
        
        return formats;
    }
    
    private List<String> extractUrlsFromArray(String content) {
        List<String> urls = new ArrayList<>();
        
        // 从数组中提取URL
        Pattern arrayPattern = Pattern.compile("\\[([^\\]]*\"[^\"]*\"[^\\]]*)\\]");
        Matcher arrayMatcher = arrayPattern.matcher(content);
        
        while (arrayMatcher.find()) {
            String arrayContent = arrayMatcher.group(1);
            Pattern urlPattern = Pattern.compile("\"([^\"]+\\.mp4[^\"]*)\"");
            Matcher urlMatcher = urlPattern.matcher(arrayContent);
            
            while (urlMatcher.find()) {
                urls.add(urlMatcher.group(1));
            }
        }
        
        return urls;
    }
    
    private List<String> searchVideoUrls(String content) {
        List<String> urls = new ArrayList<>();
        
        // 搜索各种可能的视频URL模式
        Pattern[] urlPatterns = {
            Pattern.compile("\"video_url\":\"([^\"]+)\""),
            Pattern.compile("\"playAddr\":\"([^\"]+)\""),
            Pattern.compile("\"downloadAddr\":\"([^\"]+)\""),
            Pattern.compile("src=\"([^\"]+\\.mp4[^\"]*)\"")
        };
        
        for (Pattern pattern : urlPatterns) {
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                String url = matcher.group(1);
                if (url != null && !url.isEmpty()) {
                    urls.add(url);
                }
            }
        }
        
        return urls;
    }
    
    private String decodeHtmlEntities(String text) {
        if (text == null) return null;
        
        // 简单的HTML实体解码
        return text.replace("&amp;", "&")
                  .replace("&lt;", "<")
                  .replace("&gt;", ">")
                  .replace("&quot;", "\"")
                  .replace("&#39;", "'");
    }
    
    private void extractBasicInfo(String content, VideoInfo videoInfo) {
        // 提取标题
        Pattern titlePattern = Pattern.compile("\"desc\":\"([^\"]+)\"");
        Matcher titleMatcher = titlePattern.matcher(content);
        if (titleMatcher.find()) {
            String desc = decodeHtmlEntities(titleMatcher.group(1));
            if (desc != null && desc.length() > 100) {
                desc = desc.substring(0, 100) + "...";
            }
            videoInfo.setTitle(desc != null ? desc : "TikTok Video " + videoInfo.getId());
        }
        
        // 提取作者信息
        Pattern authorPattern = Pattern.compile("\"nickname\":\"([^\"]+)\"");
        Matcher authorMatcher = authorPattern.matcher(content);
        if (authorMatcher.find()) {
            videoInfo.setUploader(authorMatcher.group(1));
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
}
