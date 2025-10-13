package com.ytdlp.extractor.m3u8;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.downloader.hls.HlsDownloader;
import com.ytdlp.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * M3U8流媒体提取器
 */
public class M3U8Extractor extends InfoExtractor {
    
    private static final String VALID_URL_REGEX = "(?i).*\\.m3u8.*";
    private static final Pattern VALID_URL_PATTERN = Pattern.compile(VALID_URL_REGEX);
    
    private HlsDownloader hlsDownloader;
    
    public M3U8Extractor() {
        super();
        this.hlsDownloader = new HlsDownloader();
    }
    
    @Override
    public String getIE_NAME() {
        return "m3u8";
    }
    
    @Override
    public String getIE_DESC() {
        return "M3U8 stream extractor";
    }
    
    @Override
    public Pattern getVALID_URL() {
        return VALID_URL_PATTERN;
    }
    
    @Override
    public boolean suitable(String url) {
        logger.info("M3U8Extractor.suitable() called for URL: " + url);
        
        boolean matches = VALID_URL_PATTERN.matcher(url).matches();
        if (matches) {
            logger.info("M3U8Extractor accepting URL: " + url);
        } else {
            logger.info("URL does not match M3U8 pattern: " + url);
        }
        
        return matches;
    }
    
    @Override
    protected String extractVideoId(String url) {
        // 从URL中提取视频ID，使用URL的hash作为ID
        return String.valueOf(url.hashCode());
    }
    
    @Override
    protected VideoInfo realExtract(String url, String videoId) throws Exception {
        System.out.println("M3U8Extractor.realExtract() called for URL: " + url);
        
        try {
            // 创建VideoInfo对象
            VideoInfo videoInfo = new VideoInfo();
            videoInfo.setId(videoId);
            videoInfo.setTitle("M3U8 Stream");
            videoInfo.setUrl(url);
            
            // 创建HLS格式
            VideoFormat hlsFormat = new VideoFormat();
            hlsFormat.setFormatId("hls");
            hlsFormat.setUrl(url);
            hlsFormat.setProtocol("hls");
            hlsFormat.setExt("m3u8");
            hlsFormat.setVcodec("unknown");
            hlsFormat.setAcodec("unknown");
            hlsFormat.setVbr(0);
            hlsFormat.setAbr(0);
            hlsFormat.setFilesize(0L);
            hlsFormat.setTbr(0);
            hlsFormat.setWidth(0);
            hlsFormat.setHeight(0);
            hlsFormat.setFps(0);
            hlsFormat.setPreference(0);
            
            // 添加到格式列表
            List<VideoFormat> formats = new ArrayList<>();
            formats.add(hlsFormat);
            videoInfo.setFormats(formats);
            
            System.out.println("M3U8Extractor: Successfully created M3U8 VideoInfo with HLS format");
            System.out.println("M3U8Extractor: VideoInfo ID: " + videoInfo.getId());
            System.out.println("M3U8Extractor: VideoInfo Title: " + videoInfo.getTitle());
            System.out.println("M3U8Extractor: VideoInfo URL: " + videoInfo.getUrl());
            System.out.println("M3U8Extractor: Formats count: " + videoInfo.getFormats().size());
            System.out.println("M3U8Extractor: HLS Format URL: " + hlsFormat.getUrl());
            System.out.println("M3U8Extractor: HLS Format Protocol: " + hlsFormat.getProtocol());
            return videoInfo;
            
        } catch (Exception e) {
            System.out.println("M3U8Extractor: Failed to extract M3U8 stream: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
