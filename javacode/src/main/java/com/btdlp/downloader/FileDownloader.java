package com.btdlp.downloader;

import com.btdlp.core.VideoInfo;
import com.btdlp.core.VideoFormat;

/**
 * 文件下载器接口
 */
public interface FileDownloader {
    
    /**
     * 下载视频文件
     * @param videoInfo 视频信息
     * @param format 视频格式
     * @param outputPath 输出路径
     * @return 下载是否成功
     */
    boolean download(VideoInfo videoInfo, VideoFormat format, String outputPath);
    
    /**
     * 取消下载
     */
    void cancel();
    
    /**
     * 是否正在下载
     * @return 是否正在下载
     */
    boolean isDownloading();
    
    /**
     * 获取下载统计信息
     * @return 下载统计信息
     */
    DownloadStats getDownloadStats();
}
