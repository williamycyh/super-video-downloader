package com.ytdlp.core;

import java.util.Map;

/**
 * Video format information class
 */
public class VideoFormat {
    private String formatId;
    private String url;
    private String ext;
    private String protocol;
    private String format;
    private String formatNote;
    private Integer width;
    private Integer height;
    private String resolution;
    private Double aspectRatio;
    private String dynamicRange;
    private Integer tbr;
    private Integer vbr;
    private Integer abr;
    private String vcodec;
    private String acodec;
    private Integer fps;
    private Integer asr;
    private Integer audioChannels;
    private Long filesize;
    private Long filesizeApprox;
    private String container;
    private Integer preference;
    private Integer quality;
    private Integer sourcePreference;
    private String language;
    private Integer languagePreference;
    private Map<String, String> httpHeaders;
    private Map<String, Object> additionalData;

    public VideoFormat() {
    }

    // Getters and Setters
    public String getFormatId() {
        return formatId;
    }

    public void setFormatId(String formatId) {
        this.formatId = formatId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormatNote() {
        return formatNote;
    }

    public void setFormatNote(String formatNote) {
        this.formatNote = formatNote;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Double getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(Double aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public String getDynamicRange() {
        return dynamicRange;
    }

    public void setDynamicRange(String dynamicRange) {
        this.dynamicRange = dynamicRange;
    }

    public Integer getTbr() {
        return tbr;
    }

    public void setTbr(Integer tbr) {
        this.tbr = tbr;
    }

    public Integer getVbr() {
        return vbr;
    }

    public void setVbr(Integer vbr) {
        this.vbr = vbr;
    }

    public Integer getAbr() {
        return abr;
    }

    public void setAbr(Integer abr) {
        this.abr = abr;
    }

    public String getVcodec() {
        return vcodec;
    }

    public void setVcodec(String vcodec) {
        this.vcodec = vcodec;
    }

    public String getAcodec() {
        return acodec;
    }

    public void setAcodec(String acodec) {
        this.acodec = acodec;
    }

    public Integer getFps() {
        return fps;
    }

    public void setFps(Integer fps) {
        this.fps = fps;
    }

    public Integer getAsr() {
        return asr;
    }

    public void setAsr(Integer asr) {
        this.asr = asr;
    }

    public Integer getAudioChannels() {
        return audioChannels;
    }

    public void setAudioChannels(Integer audioChannels) {
        this.audioChannels = audioChannels;
    }

    public Long getFilesize() {
        return filesize;
    }

    public void setFilesize(Long filesize) {
        this.filesize = filesize;
    }

    public Long getFilesizeApprox() {
        return filesizeApprox;
    }

    public void setFilesizeApprox(Long filesizeApprox) {
        this.filesizeApprox = filesizeApprox;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public Integer getPreference() {
        return preference;
    }

    public void setPreference(Integer preference) {
        this.preference = preference;
    }

    public Integer getQuality() {
        return quality;
    }

    public void setQuality(Integer quality) {
        this.quality = quality;
    }

    public Integer getSourcePreference() {
        return sourcePreference;
    }

    public void setSourcePreference(Integer sourcePreference) {
        this.sourcePreference = sourcePreference;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getLanguagePreference() {
        return languagePreference;
    }

    public void setLanguagePreference(Integer languagePreference) {
        this.languagePreference = languagePreference;
    }

    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }

    public void setHttpHeaders(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }

    @Override
    public String toString() {
        return "VideoFormat{" +
                "formatId='" + formatId + '\'' +
                ", ext='" + ext + '\'' +
                ", resolution='" + resolution + '\'' +
                ", vcodec='" + vcodec + '\'' +
                ", acodec='" + acodec + '\'' +
                '}';
    }
}
