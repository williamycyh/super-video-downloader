package com.ytdlp.downloader.hls;

import com.ytdlp.core.VideoInfo;
import com.ytdlp.core.VideoFormat;
import com.ytdlp.downloader.ProgressCallback;
import com.ytdlp.utils.Logger;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于FFmpeg的HLS下载器
 * 参考Python版本的yt-dlp实现
 */
public class FfmpegHlsDownloader {
    
    private Logger logger;
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * 下载HLS流并转换为MP4
     * 参考Python版本：yt_dlp/downloader/ffmpeg.py
     */
    public boolean download(VideoInfo videoInfo, VideoFormat format, String outputPath) {
        try {
            logger.info("=== 使用FFmpeg下载HLS流 ===");
            logger.info("视频标题: %s", videoInfo.getTitle());
            logger.info("格式ID: %s", format.getFormatId());
            logger.info("HLS URL: %s", format.getUrl());
            logger.info("输出路径: %s", outputPath);
            
            String hlsUrl = format.getUrl();
            
            // 构建FFmpeg命令
            List<String> command = buildFfmpegCommand(hlsUrl, outputPath);
            
            logger.info("FFmpeg命令: %s", String.join(" ", command));
            
            // 执行FFmpeg命令
            return executeFfmpegCommand(command, outputPath);
            
        } catch (Exception e) {
            logger.error("FFmpeg下载失败: %s", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 构建FFmpeg命令
     * 参考Python版本：FFmpegFD._call_downloader
     */
    private List<String> buildFfmpegCommand(String hlsUrl, String outputPath) {
        List<String> command = new ArrayList<>();
        
        // FFmpeg可执行文件
        command.add("ffmpeg");
        
        // 覆盖输出文件
        command.add("-y");
        
        // 输入URL
        command.add("-i");
        command.add(hlsUrl);
        
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
        command.add(outputPath);
        
        return command;
    }
    
    /**
     * 执行FFmpeg命令
     * 参考Python版本：FFmpegFD._call_downloader
     */
    private boolean executeFfmpegCommand(List<String> command, String outputPath) {
        try {
            // 创建临时文件用于进度跟踪
            String tempOutputPath = outputPath + ".tmp";
            
            // 修改命令使用临时文件
            List<String> finalCommand = new ArrayList<>(command);
            finalCommand.set(finalCommand.size() - 1, tempOutputPath);
            
            logger.info("执行FFmpeg命令: %s", String.join(" ", finalCommand));
            
            // 使用ProcessBuilder执行FFmpeg命令
            ProcessBuilder processBuilder = new ProcessBuilder(finalCommand);
            processBuilder.redirectErrorStream(true); // 合并错误流和输出流
            
            Process process = processBuilder.start();
            
            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("FFmpeg输出: %s", line);
                
                // 解析进度信息
                if (line.contains("time=") && line.contains("bitrate=")) {
                    // 简单的进度解析
                    logger.info("FFmpeg进度: %s", line);
                }
            }
            
            // 等待进程完成
            int exitCode = process.waitFor();
            reader.close();
            
            boolean success = (exitCode == 0);
            
            if (success) {
                logger.info("FFmpeg执行成功");
                
                // 移动临时文件到最终位置
                Path tempFile = Paths.get(tempOutputPath);
                Path finalFile = Paths.get(outputPath);
                
                if (Files.exists(tempFile)) {
                    Files.move(tempFile, finalFile);
                    logger.info("文件移动完成: %s", outputPath);
                    
                    // 验证输出文件
                    if (Files.exists(finalFile)) {
                        long fileSize = Files.size(finalFile);
                        logger.info("输出文件大小: %d bytes", fileSize);
                        return fileSize > 0;
                    }
                }
            } else {
                logger.error("FFmpeg执行失败，退出码: %d", exitCode);
            }
            
            return false;
            
        } catch (InterruptedException e) {
            logger.error("FFmpeg执行被中断: %s", e.getMessage());
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            logger.error("FFmpeg执行IO异常: %s", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 检查FFmpeg是否可用
     */
    public boolean isAvailable() {
        try {
            // 检查FFmpeg版本
            ProcessBuilder processBuilder = new ProcessBuilder("ffmpeg", "-version");
            Process process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();
            
            int exitCode = process.waitFor();
            boolean available = (exitCode == 0);
            
            if (available) {
                logger.info("FFmpeg可用: %s", line);
            } else {
                logger.warning("FFmpeg不可用，退出码: %d", exitCode);
            }
            
            return available;
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
        formats.add("hls");
        formats.add("m3u8");
        return formats;
    }
}
