package com.btdlp.downloader;

/**
 * 下载进度回调接口
 */
public interface ProgressCallback {
    
    /**
     * 下载开始回调
     * @param totalBytes 总字节数
     */
    void onDownloadStart(long totalBytes);
    
    /**
     * 进度更新回调
     * @param bytesDownloaded 已下载字节数
     * @param totalBytes 总字节数
     * @param speed 当前速度（字节/秒）
     */
    void onProgress(long bytesDownloaded, long totalBytes, long speed);
    
    /**
     * 下载完成回调
     * @param bytesDownloaded 已下载字节数
     * @param totalBytes 总字节数
     */
    void onDownloadComplete(long bytesDownloaded, long totalBytes);
    
    /**
     * 下载错误回调
     * @param errorMessage 错误信息
     * @param exception 异常对象
     */
    void onDownloadError(String errorMessage, Exception exception);
    
    /**
     * 下载取消回调
     * @param bytesDownloaded 已下载字节数
     */
    void onDownloadCancelled(long bytesDownloaded);
}
