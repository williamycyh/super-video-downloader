package com.btdlp.extractor.dailymotion;

import com.btdlp.core.VideoInfo;
import com.btdlp.core.VideoFormat;
import com.btdlp.extractor.InfoExtractor;
import com.btdlp.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Dailymotion视频提取器
 */
public class AdvancedDailymotionExtractor extends InfoExtractor {
    
    private static final String VALID_URL_REGEX = "(?:https?://)?(?:www\\.)?dailymotion\\.com/(?:video|embed)/([a-zA-Z0-9]+)";
    private static final Pattern VALID_URL_PATTERN = Pattern.compile(VALID_URL_REGEX);
    
    // 排除M3U8和其他流媒体URL
    private static final String EXCLUDED_URL_REGEX = "(?:cdn|player|stream|manifest).*?\\.(?:m3u8|mpd|mp4|webm)";
    private static final Pattern EXCLUDED_URL_PATTERN = Pattern.compile(EXCLUDED_URL_REGEX, Pattern.CASE_INSENSITIVE);
    
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
        // 首先检查是否匹配基本模式
        if (!VALID_URL_PATTERN.matcher(url).matches()) {
            return false;
        }
        
        // 排除M3U8和其他流媒体URL
        if (EXCLUDED_URL_PATTERN.matcher(url).find()) {
            logger.info("Rejecting Dailymotion URL (streaming media): " + url);
            return false;
        }
        
        return true;
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
        
        try {
            // 按照Python版本的逻辑：使用元数据API
            String metadataUrl = "https://www.dailymotion.com/player/metadata/video/" + videoId;
            String metadata = downloadUrl(metadataUrl);
            
            if (metadata == null || metadata.isEmpty()) {
                throw new Exception("Failed to download metadata");
            }
            
            // 解析元数据
            VideoInfo videoInfo = parseMetadata(metadata, videoId, url);
            
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
    
    /**
     * 下载URL内容
     */
    private String downloadUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求头，模拟浏览器
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11; SM-A5160 Build/RP1A.200720.012; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/140.0.7339.207 Mobile Safari/537.36");
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            connection.setRequestProperty("Referer", "https://www.dailymotion.com/");
            connection.setRequestProperty("Origin", "https://www.dailymotion.com");
            
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return response.toString();
            } else {
                logger.error("HTTP request failed with code: " + responseCode);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("Failed to download URL: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析Dailymotion元数据
     */
    private VideoInfo parseMetadata(String metadata, String videoId, String url) {
        VideoInfo videoInfo = new VideoInfo();
        videoInfo.setId(videoId);
        videoInfo.setUrl(url);
        
        try {
            // 提取标题
            String title = searchRegex(metadata, "\"title\":\"([^\"]+)\"", "title", true);
            videoInfo.setTitle(title != null ? title : "Dailymotion Video " + videoId);
            
            // 提取描述
            String description = searchRegex(metadata, "\"description\":\"([^\"]+)\"", "description", true);
            videoInfo.setDescription(description);
            
            // 提取时长
            String durationStr = searchRegex(metadata, "\"duration\":(\\d+)", "duration", true);
            if (durationStr != null) {
                try {
                    videoInfo.setDuration(Long.parseLong(durationStr));
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
            
            // 提取缩略图
            String thumbnail = searchRegex(metadata, "\"posters\":\\{[^}]*\"([^\"]+\\.jpg[^\"]*)\"", "thumbnail", true);
            videoInfo.setThumbnail(thumbnail);
            
            // 提取上传者
            String uploader = searchRegex(metadata, "\"owner\":\\{[^}]*\"screenname\":\"([^\"]+)\"", "uploader", true);
            videoInfo.setUploader(uploader);
            
            // 提取观看次数
            String viewCountStr = searchRegex(metadata, "\"views_total\":(\\d+)", "view_count", true);
            if (viewCountStr != null) {
                try {
                    videoInfo.setViewCount(Long.parseLong(viewCountStr));
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
            
            // 提取视频格式 - 这是关键部分
            List<VideoFormat> formats = extractFormatsFromMetadata(metadata);
            videoInfo.setFormats(formats);
            
            logger.info("Successfully parsed metadata for video: " + videoInfo.getTitle() + 
                       ", formats: " + formats.size());
            
        } catch (Exception e) {
            logger.error("Failed to parse metadata: " + e.getMessage());
            // 返回基本信息
            videoInfo.setTitle("Dailymotion Video " + videoId);
            videoInfo.setFormats(new ArrayList<>());
        }
        
        return videoInfo;
    }
    
    /**
     * 从元数据中提取视频格式 - 按照Python版本的逻辑
     */
    private List<VideoFormat> extractFormatsFromMetadata(String metadata) {
        List<VideoFormat> formats = new ArrayList<>();
        
        try {
            logger.debug("Starting format extraction from metadata");
            
            // 按照Python版本：直接解析qualities字段
            // Python版本: for quality, media_list in metadata['qualities'].items():
            
            // 使用简单的字符串处理方法提取qualities
            int qualitiesIndex = metadata.indexOf("\"qualities\":");
            if (qualitiesIndex != -1) {
                logger.debug("Found qualities at index: " + qualitiesIndex);
                
                // 提取qualities部分
                String qualitiesStart = metadata.substring(qualitiesIndex);
                logger.debug("Qualities start: " + qualitiesStart.substring(0, Math.min(200, qualitiesStart.length())));
                
                // 解析每个质量等级
                String[] qualityKeys = {"auto", "1080", "720", "480", "360", "240"};
                
                for (String quality : qualityKeys) {
                    String qualityKey = "\"" + quality + "\":";
                    int qualityIndex = qualitiesStart.indexOf(qualityKey);
                    if (qualityIndex != -1) {
                        logger.debug("Found quality " + quality + " at index: " + qualityIndex);
                        
                        // 查找数组开始和结束
                        int arrayStart = qualitiesStart.indexOf("[", qualityIndex);
                        int arrayEnd = qualitiesStart.indexOf("]", arrayStart);
                        
                        if (arrayStart != -1 && arrayEnd != -1) {
                            String autoArray = qualitiesStart.substring(arrayStart + 1, arrayEnd);
                            logger.debug("Quality " + quality + " array content: " + autoArray);
                            
                            // 提取URL
                            int urlIndex = autoArray.indexOf("\"url\":\"");
                            if (urlIndex != -1) {
                                String urlStart = autoArray.substring(urlIndex + 7);
                                int urlEnd = urlStart.indexOf("\"");
                                if (urlEnd != -1) {
                                    String mediaUrl = urlStart.substring(0, urlEnd).replace("\\/", "/");
                                    
                                    // 提取type
                                    int typeIndex = autoArray.indexOf("\"type\":\"");
                                    if (typeIndex != -1) {
                                        String typeStart = autoArray.substring(typeIndex + 8);
                                        int typeEnd = typeStart.indexOf("\"");
                                        if (typeEnd != -1) {
                                            String mediaType = typeStart.substring(0, typeEnd).replace("\\/", "/");
                                            
                                            logger.debug("Found media: URL=" + mediaUrl + ", type=" + mediaType);
                                            
                                            // 跳过无效的URL或特殊类型
                                            if (mediaUrl != null && !mediaUrl.isEmpty() && 
                                                !"application/vnd.lumberjack.manifest".equals(mediaType)) {
                                                
                                                VideoFormat format = createVideoFormatFromMedia(mediaUrl, mediaType, quality);
                                                if (format != null) {
                                                    formats.add(format);
                                                    logger.debug("Added format: " + quality + " - " + mediaType + " - " + mediaUrl);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                logger.debug("No qualities section found in metadata");
            }
            
            // 如果仍然没有找到格式，尝试备用方法
            if (formats.isEmpty()) {
                logger.debug("No formats found via qualities, trying alternative extraction");
                formats.addAll(searchDirectVideoUrls(metadata));
            }
            
            logger.info("Total formats extracted: " + formats.size());
            
        } catch (Exception e) {
            logger.error("Failed to extract formats from metadata: " + e.getMessage());
            e.printStackTrace();
        }
        
        return formats;
    }
    
    /**
     * 从媒体对象创建视频格式 - 按照Python版本的逻辑
     */
    private VideoFormat createVideoFormatFromMedia(String mediaUrl, String mediaType, String quality) {
        VideoFormat format = new VideoFormat();
        format.setUrl(mediaUrl);
        
        // 根据媒体类型设置协议和扩展名
        if ("application/x-mpegURL".equals(mediaType)) {
            // HLS格式
            format.setProtocol("hls");
            format.setExt("mp4");
            format.setFormatId("hls-" + quality);
        } else {
            // HTTP格式
            format.setProtocol("http");
            format.setExt("mp4");
            format.setFormatId("http-" + quality);
            
            // 尝试从URL中提取分辨率信息
            Pattern resolutionPattern = Pattern.compile("/H264-(\\d+)x(\\d+)(?:-(\\d+)/)?");
            Matcher matcher = resolutionPattern.matcher(mediaUrl);
            if (matcher.find()) {
                try {
                    int width = Integer.parseInt(matcher.group(1));
                    int height = Integer.parseInt(matcher.group(2));
                    format.setQuality(height); // 使用高度作为质量指标
                    
                    if (matcher.group(3) != null) {
                        int fps = Integer.parseInt(matcher.group(3));
                        format.setFps(fps);
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }
        
        return format;
    }
    
    /**
     * 创建视频格式对象（带质量参数）
     */
    private VideoFormat createVideoFormatWithQuality(String url, String quality) {
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
        Integer qualityInt = parseQualityFromString(quality);
        format.setQuality(qualityInt);
        format.setResolution(qualityInt + "p");
        
        format.setFormatId(quality);
        format.setPreference(getQualityPreference(quality));
        
        return format;
    }
    
    /**
     * 解析质量字符串为整数
     */
    private Integer parseQualityFromString(String quality) {
        if (quality.equals("auto")) return 720;
        if (quality.equals("1080")) return 1080;
        if (quality.equals("720")) return 720;
        if (quality.equals("480")) return 480;
        if (quality.equals("360")) return 360;
        if (quality.equals("240")) return 240;
        
        return 720; // 默认质量
    }
    
    /**
     * 获取质量优先级
     */
    private Integer getQualityPreference(String quality) {
        switch (quality) {
            case "1080": return 1;
            case "auto": return 2;
            case "720": return 3;
            case "480": return 4;
            case "360": return 5;
            case "240": return 6;
            default: return 10;
        }
    }
}
