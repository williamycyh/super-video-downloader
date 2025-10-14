package com.btdlp.core;

/**
 * Thumbnail information class
 */
public class Thumbnail {
    private String url;
    private Integer width;
    private Integer height;
    private String id;

    public Thumbnail() {
    }

    public Thumbnail(String url, Integer width, Integer height) {
        this.url = url;
        this.width = width;
        this.height = height;
    }

    // Getters and Setters
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
