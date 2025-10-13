package com.ytdlp.core;

import com.ytdlp.extractor.ExtractorRegistry;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.options.YoutubeDLOptions;
import com.ytdlp.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Core YoutubeDL class for video extraction and downloading
 */
public class YoutubeDL {
    private static YoutubeDL instance;
    private YoutubeDLOptions params;
    private Logger logger;
    private ExtractorRegistry extractorRegistry;

    private YoutubeDL() {
        this.logger = new Logger(true, true, true);  // 启用所有日志级别
        this.params = new YoutubeDLOptions();
        this.extractorRegistry = new ExtractorRegistry();
    }

    public static YoutubeDL getInstance() {
        if (instance == null) {
            instance = new YoutubeDL();
        }
        return instance;
    }

    public YoutubeDLOptions getParams() {
        return params;
    }

    public void setParams(YoutubeDLOptions params) {
        this.params = params;
    }
    
    /**
     * 设置HTTP头部信息
     */
    public void setHttpHeaders(String httpHeaders) {
        if (httpHeaders != null && !httpHeaders.isEmpty()) {
            // 将HTTP头部信息存储到params中
            this.params.setHttpHeaders(httpHeaders);
            logger.info("设置HTTP头部信息: {}", httpHeaders);
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public VideoInfo extractInfo(String url) throws Exception {
        logger.info("Extracting info from URL: " + url);
        
        InfoExtractor extractor = extractorRegistry.getExtractor(url);
        if (extractor == null) {
            throw new Exception("No suitable extractor found for URL: " + url);
        }
        
        extractor.initialize(this);
        VideoInfo info = extractor.extract(url);
        
        logger.info("Successfully extracted info for: " + info.getTitle());
        return info;
    }

    public List<VideoFormat> listFormats(String url) throws Exception {
        VideoInfo info = extractInfo(url);
        return info.getFormats();
    }

    public boolean download(String url, String outputPath) throws Exception {
        VideoInfo info = extractInfo(url);
        List<VideoFormat> formats = info.getFormats();
        
        if (formats == null || formats.isEmpty()) {
            throw new Exception("No formats available for download");
        }
        
        // Select the best format
        VideoFormat selectedFormat = selectBestFormat(formats);
        
        // Download the video
        return downloadFormat(selectedFormat, outputPath, info.getTitle());
    }

    private VideoFormat selectBestFormat(List<VideoFormat> formats) {
        // Simple format selection logic - prefer video+audio formats
        for (VideoFormat format : formats) {
            if (format.getVcodec() != null && !format.getVcodec().equals("none") &&
                format.getAcodec() != null && !format.getAcodec().equals("none")) {
                return format;
            }
        }
        
        // Fallback to first format
        return formats.get(0);
    }

    public boolean downloadFormat(VideoFormat format, String outputPath, String title) throws Exception {
        logger.info("Downloading format: " + format.getFormatId());
        logger.info("Format protocol: " + format.getProtocol());
        logger.info("Format URL: " + format.getUrl());
        logger.info("Output path: " + outputPath);
        
        // 根据格式类型选择下载器
        boolean isHls = isHlsFormat(format);
        logger.info("Is HLS format: " + isHls);
        
        if (isHls) {
            logger.info("Using HLS downloader");
            return downloadWithHlsDownloader(format, outputPath, title);
        } else {
            logger.info("Using HTTP downloader");
            return downloadWithHttpDownloader(format, outputPath, title);
        }
    }
    
    private boolean isHlsFormat(VideoFormat format) {
        String protocol = format.getProtocol();
        String url = format.getUrl();
        return "hls".equals(protocol) || 
               (url != null && (url.contains(".m3u8") || url.contains("m3u8")));
    }
    
    private boolean downloadWithHlsDownloader(VideoFormat format, String outputPath, String title) throws Exception {
        try {
            logger.info("Using HLS downloader for format: " + format.getFormatId());
            com.ytdlp.downloader.hls.HlsDownloader hlsDownloader = new com.ytdlp.downloader.hls.HlsDownloader();
            
            // 初始化下载器
            hlsDownloader.initialize(params, logger);
            
            // 创建临时VideoInfo用于下载
            VideoInfo tempInfo = new VideoInfo();
            tempInfo.setTitle(title);
            tempInfo.setUrl(format.getUrl());
            
            return hlsDownloader.download(tempInfo, format, outputPath);
        } catch (Exception e) {
            logger.error("HLS download failed: " + e.getMessage());
            throw e;
        }
    }
    
    private boolean downloadWithHttpDownloader(VideoFormat format, String outputPath, String title) throws Exception {
        try {
            logger.info("Using HTTP downloader for format: " + format.getFormatId());
            com.ytdlp.downloader.http.HttpDownloader httpDownloader = new com.ytdlp.downloader.http.HttpDownloader();
            
            // 初始化下载器
            httpDownloader.initialize(params, logger);
            
            // 创建临时VideoInfo用于下载
            VideoInfo tempInfo = new VideoInfo();
            tempInfo.setTitle(title);
            tempInfo.setUrl(format.getUrl());
            
            return httpDownloader.download(tempInfo, format, outputPath);
        } catch (Exception e) {
            logger.error("HTTP download failed: " + e.getMessage());
            throw e;
        }
    }
}
