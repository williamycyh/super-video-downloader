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
        this.logger = new Logger(false, false, false);
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

    private boolean downloadFormat(VideoFormat format, String outputPath, String title) throws Exception {
        // This would implement actual downloading logic
        // For now, return true as placeholder
        logger.info("Downloading format: " + format.getFormatId());
        return true;
    }
}
