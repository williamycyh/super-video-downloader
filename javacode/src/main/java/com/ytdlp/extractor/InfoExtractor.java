package com.ytdlp.extractor;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.YoutubeDL;
import com.ytdlp.options.YoutubeDLOptions;
import com.ytdlp.utils.Logger;

import java.util.regex.Pattern;

/**
 * Abstract base class for information extractors
 */
public abstract class InfoExtractor {
    protected YoutubeDL downloader;
    protected YoutubeDLOptions params;
    protected Logger logger;

    public InfoExtractor() {
        this.logger = new Logger(false, false, false);
    }

    public void initialize(YoutubeDL downloader) {
        this.downloader = downloader;
        this.params = downloader.getParams();
    }

    public boolean suitable(String url) {
        return getVALID_URL().matcher(url).matches();
    }

    public VideoInfo extract(String url) throws ExtractorException {
        try {
            String videoId = extractVideoId(url);
            return realExtract(url, videoId);
        } catch (Exception e) {
            throw new ExtractorException("Failed to extract video info: " + e.getMessage(), e);
        }
    }

    protected abstract String extractVideoId(String url);
    protected abstract VideoInfo realExtract(String url, String videoId) throws Exception;

    protected String downloadWebpage(String url) throws Exception {
        // This would use the downloader's HTTP client
        // For now, return a placeholder
        return "";
    }

    protected Object downloadJson(String url) throws Exception {
        // This would download and parse JSON
        // For now, return a placeholder
        return new Object();
    }

    protected String searchRegex(Pattern pattern, String text, String name) throws ExtractorException {
        return searchRegex(pattern, text, name, null);
    }

    protected String searchRegex(Pattern pattern, String text, String name, String group) {
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            if (group != null) {
                return matcher.group(group);
            } else {
                return matcher.group(1);
            }
        }
        return null;
    }

    protected <T> T getParam(String key, T defaultValue) {
        if (params != null && params.get(key) != null) {
            try {
                return (T) params.get(key);
            } catch (ClassCastException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public abstract String getIE_NAME();
    public abstract String getIE_DESC();
    public abstract Pattern getVALID_URL();
}
