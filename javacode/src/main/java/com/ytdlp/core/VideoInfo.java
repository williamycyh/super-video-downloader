package com.ytdlp.core;

import java.util.List;
import java.util.Map;

/**
 * Video information class containing metadata about a video
 */
public class VideoInfo {
    private String id;
    private String title;
    private String url;
    private String webpageUrl;
    private String description;
    private String uploader;
    private String uploaderId;
    private String channel;
    private String channelId;
    private Long duration;
    private Long viewCount;
    private Long likeCount;
    private String uploadDate;
    private Long timestamp;
    private List<VideoFormat> formats;
    private String thumbnail;
    private List<Thumbnail> thumbnails;
    private List<String> tags;
    private List<String> categories;
    private String ageLimit;
    private boolean isLive;
    private boolean wasLive;
    private String availability;
    private Map<String, List<Subtitle>> subtitles;
    private Map<String, List<Subtitle>> automaticCaptions;
    private List<Comment> comments;
    private Long commentCount;
    private List<Chapter> chapters;

    public VideoInfo() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getWebpageUrl() {
        return webpageUrl;
    }

    public void setWebpageUrl(String webpageUrl) {
        this.webpageUrl = webpageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUploader() {
        return uploader;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public List<VideoFormat> getFormats() {
        return formats;
    }

    public void setFormats(List<VideoFormat> formats) {
        this.formats = formats;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public List<Thumbnail> getThumbnails() {
        return thumbnails;
    }

    public void setThumbnails(List<Thumbnail> thumbnails) {
        this.thumbnails = thumbnails;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public String getAgeLimit() {
        return ageLimit;
    }

    public void setAgeLimit(String ageLimit) {
        this.ageLimit = ageLimit;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public boolean isWasLive() {
        return wasLive;
    }

    public void setWasLive(boolean wasLive) {
        this.wasLive = wasLive;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public Map<String, List<Subtitle>> getSubtitles() {
        return subtitles;
    }

    public void setSubtitles(Map<String, List<Subtitle>> subtitles) {
        this.subtitles = subtitles;
    }

    public Map<String, List<Subtitle>> getAutomaticCaptions() {
        return automaticCaptions;
    }

    public void setAutomaticCaptions(Map<String, List<Subtitle>> automaticCaptions) {
        this.automaticCaptions = automaticCaptions;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public Long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
    }

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }

    @Override
    public String toString() {
        return "VideoInfo{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", uploader='" + uploader + '\'' +
                ", duration=" + duration +
                '}';
    }
}
