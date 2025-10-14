package com.ytdlp.downloader.hls;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.downloader.ProgressCallback;
import com.ytdlp.utils.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

// FFmpegKit imports
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.Statistics;
import com.arthenica.ffmpegkit.StatisticsCallback;
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback;

/**
 * Android版本的FFmpeg HLS下载器
 * 使用FFmpegKit进行Android兼容的视频处理
 */
public class AndroidFfmpegHlsDownloader {
    
    private Logger logger;
    private boolean isInitialized = false;
    private ProgressCallback progressCallback;
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    public void setProgressCallback(ProgressCallback progressCallback) {
        this.progressCallback = progressCallback;
    }
    
    /**
     * 初始化FFmpegKit
     * 在Android环境中需要先初始化FFmpegKit
     */
    public void initialize() {
        try {
            // 检查是否在Android环境中
            boolean isAndroid = isAndroidEnvironment();
            
            if (isAndroid) {
                // 在Android环境中，FFmpegKit会自动初始化
                logger.info("Android环境检测到，FFmpegKit将自动初始化");
            } else {
                // 在非Android环境中，使用系统FFmpeg
                logger.info("非Android环境，将使用系统FFmpeg");
            }
            
            isInitialized = true;
            logger.info("FFmpeg下载器初始化完成");
            
        } catch (Exception e) {
            logger.error("FFmpeg下载器初始化失败: %s", e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查是否在Android环境中
     */
    private boolean isAndroidEnvironment() {
        try {
            // 尝试加载Android特定的类
            Class.forName("android.os.Build");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 下载HLS流并转换为MP4
     * 参考Python版本：yt_dlp/downloader/ffmpeg.py
     */
    public boolean download(VideoInfo videoInfo, VideoFormat format, String outputPath) {
        try {
            if (!isInitialized) {
                initialize();
            }
            
            logger.info("=== 使用Android FFmpeg下载HLS流 ===");
            logger.info("AndroidFfmpegHlsDownloader.download调用 - 线程: %s", Thread.currentThread().getName());
            logger.info("视频标题: %s", videoInfo.getTitle());
            logger.info("格式ID: %s", format.getFormatId());
            logger.info("HLS URL: %s", format.getUrl());
            logger.info("输出路径: %s", outputPath);
            logger.info("输出路径长度: %d", outputPath.length());
            logger.info("输出路径是否为绝对路径: %s", new File(outputPath).isAbsolute());
            
            String hlsUrl = format.getUrl();
            
            // 检查环境并选择相应的下载方法
            if (isAndroidEnvironment()) {
                return downloadWithFFmpegKit(hlsUrl, outputPath);
            } else {
                return downloadWithSystemFFmpeg(hlsUrl, outputPath);
            }
            
        } catch (Exception e) {
            logger.error("Android FFmpeg下载失败: %s", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 使用FFmpegKit下载（Android环境）
     */
    private boolean downloadWithFFmpegKit(String hlsUrl, String outputPath) {
        try {
            logger.info("使用FFmpegKit下载...");
            
            // 构建FFmpeg命令
            List<String> command = buildFfmpegCommand(hlsUrl, outputPath);
            
            logger.info("FFmpegKit命令: %s", String.join(" ", command));
            
            // 使用FFmpegKit执行命令
            // ✅ 用 CompletableFuture 来等待异步执行结束
            CompletableFuture<Boolean> future = new CompletableFuture<>();

            // 记录执行开始时间（用于计算百分比）
            long startTime = System.currentTimeMillis();
            
            // 调用下载开始回调
            if (progressCallback != null) {
                progressCallback.onDownloadStart(10000); // 估算10MB
            }

            FFmpegKit.executeAsync(
                    String.join(" ", command),
                    session -> {
                        // ✅ 任务结束时回调
                        ReturnCode returnCode = session.getReturnCode();
                        boolean success = ReturnCode.isSuccess(returnCode);

                        if (success) {
                            logger.info("✅ FFmpegKit 执行成功");
                        } else {
                            logger.error("❌ FFmpegKit 执行失败，返回码: %s", returnCode);
                            logger.error("错误输出: %s", session.getFailStackTrace());
                            logger.error("输出: %s", session.getOutput());
                        }

                        // 通知 future 结束
                        future.complete(success);
                    },
                    log -> {
                        // 🧾 日志输出（可选）
                        logger.debug("[FFmpegLog] %s", log.getMessage());
                    },
                    statistics -> {
                        // 📈 进度信息（单位：毫秒）
                        double timeMs = statistics.getTime();
                        if (progressCallback != null && timeMs > 0) {
                            // 适配 ProgressCallback 接口
                            // 估算下载进度：基于时间或文件大小
                            double seconds = timeMs / 1000.0;
                            double bitrate = statistics.getBitrate(); // bps
                            long speed = (long) (bitrate / 8); // 转换为 bytes/sec
                            long size = statistics.getSize(); // bytes
                            
                            // 简单估算：假设总时长60秒，或者基于当前下载速度估算
                            long estimatedTotalBytes = size * 2; // 简单估算
                            if (estimatedTotalBytes == 0) {
                                estimatedTotalBytes = 10000; // 默认10MB
                            }
                            
                            progressCallback.onProgress(size, estimatedTotalBytes, speed);
                        }
                    }
            );

            // ✅ 阻塞等待执行完成（同步）
            boolean success = future.get(); // 会阻塞直到 complete()
            
            if (success) {
                // 验证输出文件
                File outputFile = new File(outputPath);
                if (outputFile.exists() && outputFile.length() > 0) {
                    logger.info("FFmpegKit下载完成: %s (大小: %d bytes)", outputPath, outputFile.length());
                    
                    // 调用完成回调
                    if (progressCallback != null) {
                        progressCallback.onDownloadComplete(outputFile.length(), outputFile.length());
                    }
                    
                    return true;
                } else {
                    logger.error("FFmpegKit下载完成但文件不存在或为空: %s", outputPath);
                    
                    // 调用错误回调
                    if (progressCallback != null) {
                        progressCallback.onDownloadError("文件不存在或为空", null);
                    }
                    
                    return false;
                }
            } else {
                // 调用错误回调
                if (progressCallback != null) {
                    progressCallback.onDownloadError("FFmpeg执行失败", null);
                }
            }
            
            return false;
            
        } catch (InterruptedException | ExecutionException e) {
            logger.error("FFmpegKit执行被中断: %s", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("FFmpegKit执行异常: %s", e.getMessage());
            return false;
        }
    }
    
    /**
     * 使用系统FFmpeg下载（非Android环境）
     */
    private boolean downloadWithSystemFFmpeg(String hlsUrl, String outputPath) {
        try {
            logger.info("使用系统FFmpeg下载...");
            
            // 构建FFmpeg命令
            List<String> command = buildFfmpegCommand(hlsUrl, outputPath);
            
            logger.info("系统FFmpeg命令: %s", String.join(" ", command));
            
            // 使用ProcessBuilder执行FFmpeg命令
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("FFmpeg输出: %s", line);
            }
            reader.close();
            
            // 等待进程完成
            int exitCode = process.waitFor();
            boolean success = (exitCode == 0);
            
            if (success) {
                logger.info("系统FFmpeg下载完成: %s", outputPath);
                return true;
            } else {
                logger.error("系统FFmpeg执行失败，退出码: %d", exitCode);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("系统FFmpeg执行异常: %s", e.getMessage());
            return false;
        }
    }
    
    /**
     * 构建FFmpeg命令
     * 参考Python版本：FFmpegFD._call_downloader
     * 支持多种流媒体格式：HLS、DASH、RTMP、RTSP等
     */
    private List<String> buildFfmpegCommand(String streamUrl, String outputPath) {
        List<String> command = new ArrayList<>();
        
        // FFmpeg可执行文件（在Android中由FFmpegKit处理）
        if (!isAndroidEnvironment()) {
            command.add("ffmpeg");
        }
        
        // 覆盖输出文件
        command.add("-y");
        
        // 根据流媒体类型设置不同的参数
        String streamType = detectStreamType(streamUrl);
        logger.info("检测到流媒体类型: %s", streamType);
        
        switch (streamType.toLowerCase()) {
            case "hls":
            case "m3u8":
                // HLS流参数
                command.add("-protocol_whitelist");
                command.add("file,http,https,tcp,tls");
                break;
                
            case "dash":
            case "mpd":
                // DASH流参数
                command.add("-f");
                command.add("dash");
                break;
                
            case "rtmp":
                // RTMP流参数
                command.add("-f");
                command.add("flv");
                break;
                
            case "rtsp":
                // RTSP流参数
                command.add("-rtsp_transport");
                command.add("tcp");
                break;
                
            case "mms":
                // MMS流参数
                command.add("-f");
                command.add("asf");
                break;
                
            default:
                // 默认参数，让FFmpeg自动检测
                logger.info("使用默认FFmpeg参数");
                break;
        }
        
        // 输入URL
        command.add("-i");
        command.add(streamUrl);
        
        // 编码设置 - 参考Python版本
        command.add("-c");
        command.add("copy");  // 直接复制流，不重新编码
        
        // 输出格式
        command.add("-f");
        command.add("mp4");
        
        // 音频设置 - 参考Python版本
        command.add("-bsf:a");
        command.add("aac_adtstoasc");  // AAC音频比特流过滤器
        
        // 视频设置
        command.add("-bsf:v");
        command.add("h264_mp4toannexb");  // H.264比特流过滤器
        
        // 输出文件
        logger.info("buildFfmpegCommand - 添加输出路径: %s", outputPath);
        logger.info("buildFfmpegCommand - 输出路径长度: %d", outputPath.length());
        command.add(outputPath);
        
        logger.info("buildFfmpegCommand - 完整命令: %s", String.join(" ", command));
        return command;
    }
    
    /**
     * 检测流媒体类型
     */
    private String detectStreamType(String url) {
        if (url == null) return "unknown";
        
        String lowerUrl = url.toLowerCase();
        
        if (lowerUrl.contains(".m3u8") || lowerUrl.contains("hls")) {
            return "hls";
        } else if (lowerUrl.contains(".mpd") || lowerUrl.contains("dash")) {
            return "dash";
        } else if (lowerUrl.contains("rtmp://")) {
            return "rtmp";
        } else if (lowerUrl.contains("rtsp://")) {
            return "rtsp";
        } else if (lowerUrl.contains("mms://")) {
            return "mms";
        } else if (lowerUrl.contains(".txt") || lowerUrl.contains("manifest")) {
            return "manifest";
        } else {
            return "auto";
        }
    }
    
    /**
     * 检查FFmpeg是否可用
     */
    public boolean isAvailable() {
        try {
            if (!isInitialized) {
                initialize();
            }
            
            if (isAndroidEnvironment()) {
                // 在Android环境中，FFmpegKit应该总是可用的
                logger.info("Android环境：FFmpegKit可用");
                return true;
            } else {
                // 在非Android环境中检查系统FFmpeg
                ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-version");
                Process process = processBuilder.start();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = reader.readLine();
                reader.close();
                
                int exitCode = process.waitFor();
                boolean available = (exitCode == 0);
                
                if (available) {
                    logger.info("系统FFmpeg可用: %s", line);
                } else {
                    logger.warning("系统FFmpeg不可用，退出码: %d", exitCode);
                }
                
                return available;
            }
            
        } catch (Exception e) {
            logger.error("检查FFmpeg可用性失败: %s", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取支持的格式列表
     */
    public List<String> getSupportedFormats() {
        List<String> formats = new ArrayList<>();
        
        // HLS格式
        formats.add("hls");
        formats.add("m3u8");
        
        // DASH格式
        formats.add("dash");
        formats.add("mpd");
        
        // 流媒体协议
        formats.add("rtmp");
        formats.add("rtsp");
        formats.add("mms");
        
        // 清单文件
        formats.add("manifest");
        formats.add("playlist");
        
        // 文本格式
        formats.add("txt");
        
        return formats;
    }
}
