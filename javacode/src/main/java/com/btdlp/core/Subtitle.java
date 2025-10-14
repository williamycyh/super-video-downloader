package com.btdlp.core;

/**
 * Subtitle information class
 */
public class Subtitle {
    private String url;
    private String ext;
    private String language;
    private String languageCode;
    private String name;
    private boolean isAutomatic;

    public Subtitle() {
    }

    public Subtitle(String url, String ext, String language) {
        this.url = url;
        this.ext = ext;
        this.language = language;
    }

    // Getters and Setters
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAutomatic() {
        return isAutomatic;
    }

    public void setAutomatic(boolean automatic) {
        isAutomatic = automatic;
    }
}
