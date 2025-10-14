package com.ytdlp.options;

import java.util.HashMap;
import java.util.Map;

/**
 * 下载选项类，支持Python版本yt-dlp的所有选项
 */
public class DownloadOptions {
    
    // 基本选项
    private String format;
    private String output;
    private String outputTemplate;
    
    // HTTP选项
    private String httpHeaders;
    private String cookiesFile;
    private String proxy;
    private String userAgent;
    private String referer;
    
    // 文件选项
    private Long maxFileSize;
    private Long minFileSize;
    private String fileExtension;
    
    // 重试选项
    private Integer retries;
    private Integer fragmentRetries;
    private Integer timeout;
    
    // 其他选项
    private Boolean noCacheDir;
    private String cacheDir;
    private Boolean ignoreErrors;
    private Boolean extractAudio;
    private String audioFormat;
    private String videoFormat;
    
    // 自定义选项
    private Map<String, String> customOptions;
    
    public DownloadOptions() {
        this.customOptions = new HashMap<>();
    }
    
    // 构建器模式
    public static class Builder {
        private DownloadOptions options;
        
        public Builder() {
            this.options = new DownloadOptions();
        }
        
        public Builder format(String format) {
            options.format = format;
            return this;
        }
        
        public Builder output(String output) {
            options.output = output;
            return this;
        }
        
        public Builder outputTemplate(String template) {
            options.outputTemplate = template;
            return this;
        }
        
        public Builder httpHeaders(String headers) {
            options.httpHeaders = headers;
            return this;
        }
        
        public Builder cookiesFile(String cookiesFile) {
            options.cookiesFile = cookiesFile;
            return this;
        }
        
        public Builder proxy(String proxy) {
            options.proxy = proxy;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            options.userAgent = userAgent;
            return this;
        }
        
        public Builder referer(String referer) {
            options.referer = referer;
            return this;
        }
        
        public Builder maxFileSize(Long maxFileSize) {
            options.maxFileSize = maxFileSize;
            return this;
        }
        
        public Builder minFileSize(Long minFileSize) {
            options.minFileSize = minFileSize;
            return this;
        }
        
        public Builder retries(Integer retries) {
            options.retries = retries;
            return this;
        }
        
        public Builder fragmentRetries(Integer fragmentRetries) {
            options.fragmentRetries = fragmentRetries;
            return this;
        }
        
        public Builder timeout(Integer timeout) {
            options.timeout = timeout;
            return this;
        }
        
        public Builder noCacheDir(Boolean noCacheDir) {
            options.noCacheDir = noCacheDir;
            return this;
        }
        
        public Builder cacheDir(String cacheDir) {
            options.cacheDir = cacheDir;
            return this;
        }
        
        public Builder ignoreErrors(Boolean ignoreErrors) {
            options.ignoreErrors = ignoreErrors;
            return this;
        }
        
        public Builder extractAudio(Boolean extractAudio) {
            options.extractAudio = extractAudio;
            return this;
        }
        
        public Builder audioFormat(String audioFormat) {
            options.audioFormat = audioFormat;
            return this;
        }
        
        public Builder videoFormat(String videoFormat) {
            options.videoFormat = videoFormat;
            return this;
        }
        
        public Builder addOption(String key, String value) {
            options.customOptions.put(key, value);
            return this;
        }
        
        public DownloadOptions build() {
            return options;
        }
    }
    
    // Getters and Setters
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    
    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }
    
    public String getOutputTemplate() { return outputTemplate; }
    public void setOutputTemplate(String outputTemplate) { this.outputTemplate = outputTemplate; }
    
    public String getHttpHeaders() { return httpHeaders; }
    public void setHttpHeaders(String httpHeaders) { this.httpHeaders = httpHeaders; }
    
    public String getCookiesFile() { return cookiesFile; }
    public void setCookiesFile(String cookiesFile) { this.cookiesFile = cookiesFile; }
    
    public String getProxy() { return proxy; }
    public void setProxy(String proxy) { this.proxy = proxy; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }
    
    public Long getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(Long maxFileSize) { this.maxFileSize = maxFileSize; }
    
    public Long getMinFileSize() { return minFileSize; }
    public void setMinFileSize(Long minFileSize) { this.minFileSize = minFileSize; }
    
    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }
    
    public Integer getRetries() { return retries; }
    public void setRetries(Integer retries) { this.retries = retries; }
    
    public Integer getFragmentRetries() { return fragmentRetries; }
    public void setFragmentRetries(Integer fragmentRetries) { this.fragmentRetries = fragmentRetries; }
    
    public Integer getTimeout() { return timeout; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }
    
    public Boolean getNoCacheDir() { return noCacheDir; }
    public void setNoCacheDir(Boolean noCacheDir) { this.noCacheDir = noCacheDir; }
    
    public String getCacheDir() { return cacheDir; }
    public void setCacheDir(String cacheDir) { this.cacheDir = cacheDir; }
    
    public Boolean getIgnoreErrors() { return ignoreErrors; }
    public void setIgnoreErrors(Boolean ignoreErrors) { this.ignoreErrors = ignoreErrors; }
    
    public Boolean getExtractAudio() { return extractAudio; }
    public void setExtractAudio(Boolean extractAudio) { this.extractAudio = extractAudio; }
    
    public String getAudioFormat() { return audioFormat; }
    public void setAudioFormat(String audioFormat) { this.audioFormat = audioFormat; }
    
    public String getVideoFormat() { return videoFormat; }
    public void setVideoFormat(String videoFormat) { this.videoFormat = videoFormat; }
    
    public Map<String, String> getCustomOptions() { return customOptions; }
    public void setCustomOptions(Map<String, String> customOptions) { this.customOptions = customOptions; }
    
    /**
     * 检查是否有某个选项
     */
    public boolean hasOption(String key) {
        return customOptions.containsKey(key);
    }
    
    /**
     * 获取自定义选项的值
     */
    public String getOption(String key) {
        return customOptions.get(key);
    }
    
    /**
     * 添加自定义选项
     */
    public void addOption(String key, String value) {
        customOptions.put(key, value);
    }
}
