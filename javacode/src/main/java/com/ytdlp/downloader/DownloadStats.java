package com.ytdlp.downloader;

/**
 * 下载统计信息类
 */
public class DownloadStats {
    
    private long downloadedBytes;
    private long totalBytes;
    private long downloadTimeMs;
    private long averageSpeedBytesPerSecond;
    private int retryCount;
    private String lastError;
    private boolean completed;
    private boolean cancelled;
    private boolean failed;
    
    public DownloadStats() {
        reset();
    }
    
    public void reset() {
        downloadedBytes = 0;
        totalBytes = 0;
        downloadTimeMs = 0;
        averageSpeedBytesPerSecond = 0;
        retryCount = 0;
        lastError = null;
        completed = false;
        cancelled = false;
        failed = false;
    }
    
    public void updateProgress(long bytesDownloaded, long totalBytes, long speed) {
        this.downloadedBytes = bytesDownloaded;
        this.totalBytes = totalBytes;
        this.averageSpeedBytesPerSecond = speed;
        
        if (downloadTimeMs == 0) {
            downloadTimeMs = System.currentTimeMillis();
        }
    }
    
    public long getDownloadedBytes() {
        return downloadedBytes;
    }
    
    public void setDownloadedBytes(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
    }
    
    public long getTotalBytes() {
        return totalBytes;
    }
    
    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }
    
    public long getDownloadTimeMs() {
        return downloadTimeMs;
    }
    
    public void setDownloadTimeMs(long downloadTimeMs) {
        this.downloadTimeMs = downloadTimeMs;
    }
    
    public long getAverageSpeedBytesPerSecond() {
        return averageSpeedBytesPerSecond;
    }
    
    public void setAverageSpeedBytesPerSecond(long averageSpeedBytesPerSecond) {
        this.averageSpeedBytesPerSecond = averageSpeedBytesPerSecond;
    }
    
    public int getRetryCount() {
        return retryCount;
    }
    
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public String getLastError() {
        return lastError;
    }
    
    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
    
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    public boolean isFailed() {
        return failed;
    }
    
    public void setFailed(boolean failed) {
        this.failed = failed;
    }
    
    public double getProgressPercentage() {
        if (totalBytes <= 0) {
            return 0.0;
        }
        return (double) downloadedBytes / totalBytes * 100.0;
    }
    
    public String getFormattedSpeed() {
        return formatBytes(averageSpeedBytesPerSecond) + "/s";
    }
    
    public String getFormattedDownloaded() {
        return formatBytes(downloadedBytes);
    }
    
    public String getFormattedTotal() {
        return formatBytes(totalBytes);
    }
    
    public String getFormattedTime() {
        if (downloadTimeMs <= 0) {
            return "00:00";
        }
        
        long seconds = downloadTimeMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        if (unitIndex == 0) {
            return String.format("%.0f %s", size, units[unitIndex]);
        } else {
            return String.format("%.2f %s", size, units[unitIndex]);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "DownloadStats{progress=%.2f%%, downloaded=%s, total=%s, speed=%s, retries=%d, completed=%s, cancelled=%s, failed=%s}",
            getProgressPercentage(),
            getFormattedDownloaded(),
            getFormattedTotal(),
            getFormattedSpeed(),
            retryCount,
            completed,
            cancelled,
            failed
        );
    }
}
