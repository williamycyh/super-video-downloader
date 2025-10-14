package com.ytdlp;

import com.ytdlp.options.DownloadOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 兼容Python版本的YoutubeDLRequest类
 */
public class YoutubeDLRequest {
    private List<String> urls;
    private DownloadOptions options;
    private List<String> customCommands;
    
    public YoutubeDLRequest(String url) {
        this.urls = new ArrayList<>();
        this.urls.add(url);
        this.options = new DownloadOptions();
        this.customCommands = new ArrayList<>();
    }
    
    public YoutubeDLRequest(List<String> urls) {
        this.urls = new ArrayList<>(urls);
        this.options = new DownloadOptions();
        this.customCommands = new ArrayList<>();
    }
    
    /**
     * 添加选项（兼容Python版本的方法名）
     */
    public YoutubeDLRequest addOption(String option, String argument) {
        switch (option) {
            case "--format":
            case "-f":
                options.setFormat(argument);
                break;
            case "--output":
            case "-o":
                options.setOutput(argument);
                break;
            case "--output-template":
                options.setOutputTemplate(argument);
                break;
            case "--http-headers":
                options.setHttpHeaders(argument);
                break;
            case "--cookies":
                options.setCookiesFile(argument);
                break;
            case "--proxy":
                options.setProxy(argument);
                break;
            case "--user-agent":
                options.setUserAgent(argument);
                break;
            case "--referer":
                options.setReferer(argument);
                break;
            case "--max-filesize":
                options.setMaxFileSize(parseFileSize(argument));
                break;
            case "--min-filesize":
                options.setMinFileSize(parseFileSize(argument));
                break;
            case "--retries":
                options.setRetries(Integer.parseInt(argument));
                break;
            case "--fragment-retries":
                options.setFragmentRetries(Integer.parseInt(argument));
                break;
            case "--timeout":
                options.setTimeout(Integer.parseInt(argument));
                break;
            case "-N":
                // 线程数选项，添加到自定义选项
                options.addOption(option, argument);
                break;
            case "--no-cache-dir":
                options.setNoCacheDir(true);
                break;
            case "--cache-dir":
                options.setCacheDir(argument);
                break;
            case "--ignore-errors":
                options.setIgnoreErrors(true);
                break;
            case "--extract-audio":
                options.setExtractAudio(true);
                break;
            case "--audio-format":
                options.setAudioFormat(argument);
                break;
            case "--video-format":
                options.setVideoFormat(argument);
                break;
            default:
                options.addOption(option, argument);
                break;
        }
        return this;
    }
    
    /**
     * 添加选项（数字参数）
     */
    public YoutubeDLRequest addOption(String option, Number argument) {
        return addOption(option, argument.toString());
    }
    
    /**
     * 添加选项（无参数）
     */
    public YoutubeDLRequest addOption(String option) {
        switch (option) {
            case "--no-cache-dir":
                options.setNoCacheDir(true);
                break;
            case "--ignore-errors":
                options.setIgnoreErrors(true);
                break;
            case "--extract-audio":
                options.setExtractAudio(true);
                break;
            default:
                options.addOption(option, "");
                break;
        }
        return this;
    }
    
    /**
     * 添加自定义命令
     */
    public YoutubeDLRequest addCommands(List<String> commands) {
        this.customCommands.addAll(commands);
        return this;
    }
    
    /**
     * 获取选项值
     */
    public String getOption(String option) {
        switch (option) {
            case "--format":
            case "-f":
                return options.getFormat();
            case "--output":
            case "-o":
                return options.getOutput();
            case "--output-template":
                return options.getOutputTemplate();
            case "--http-headers":
                return options.getHttpHeaders();
            case "--cookies":
                return options.getCookiesFile();
            case "--proxy":
                return options.getProxy();
            case "--user-agent":
                return options.getUserAgent();
            case "--referer":
                return options.getReferer();
            case "--max-filesize":
                return options.getMaxFileSize() != null ? options.getMaxFileSize().toString() : null;
            case "--min-filesize":
                return options.getMinFileSize() != null ? options.getMinFileSize().toString() : null;
            case "--retries":
                return options.getRetries() != null ? options.getRetries().toString() : null;
            case "--fragment-retries":
                return options.getFragmentRetries() != null ? options.getFragmentRetries().toString() : null;
            case "--timeout":
                return options.getTimeout() != null ? options.getTimeout().toString() : null;
            case "--cache-dir":
                return options.getCacheDir();
            case "--audio-format":
                return options.getAudioFormat();
            case "--video-format":
                return options.getVideoFormat();
            default:
                return options.getOption(option);
        }
    }
    
    /**
     * 检查是否有某个选项
     */
    public boolean hasOption(String option) {
        String value = getOption(option);
        if (value != null) return true;
        
        switch (option) {
            case "--no-cache-dir":
                return Boolean.TRUE.equals(options.getNoCacheDir());
            case "--ignore-errors":
                return Boolean.TRUE.equals(options.getIgnoreErrors());
            case "--extract-audio":
                return Boolean.TRUE.equals(options.getExtractAudio());
            default:
                return options.hasOption(option);
        }
    }
    
    /**
     * 构建命令列表（兼容Python版本）
     */
    public List<String> buildCommand() {
        List<String> command = new ArrayList<>();
        
        // 添加选项
        if (options.getFormat() != null) {
            command.add("-f");
            command.add(options.getFormat());
        }
        if (options.getOutput() != null) {
            command.add("-o");
            command.add(options.getOutput());
        }
        if (options.getOutputTemplate() != null) {
            command.add("--output-template");
            command.add(options.getOutputTemplate());
        }
        if (options.getHttpHeaders() != null) {
            command.add("--http-headers");
            command.add(options.getHttpHeaders());
        }
        if (options.getCookiesFile() != null) {
            command.add("--cookies");
            command.add(options.getCookiesFile());
        }
        if (options.getProxy() != null) {
            command.add("--proxy");
            command.add(options.getProxy());
        }
        if (options.getUserAgent() != null) {
            command.add("--user-agent");
            command.add(options.getUserAgent());
        }
        if (options.getReferer() != null) {
            command.add("--referer");
            command.add(options.getReferer());
        }
        if (options.getMaxFileSize() != null) {
            command.add("--max-filesize");
            command.add(options.getMaxFileSize().toString());
        }
        if (options.getMinFileSize() != null) {
            command.add("--min-filesize");
            command.add(options.getMinFileSize().toString());
        }
        if (options.getRetries() != null) {
            command.add("--retries");
            command.add(options.getRetries().toString());
        }
        if (options.getFragmentRetries() != null) {
            command.add("--fragment-retries");
            command.add(options.getFragmentRetries().toString());
        }
        if (options.getTimeout() != null) {
            command.add("--timeout");
            command.add(options.getTimeout().toString());
        }
        if (Boolean.TRUE.equals(options.getNoCacheDir())) {
            command.add("--no-cache-dir");
        }
        if (options.getCacheDir() != null) {
            command.add("--cache-dir");
            command.add(options.getCacheDir());
        }
        if (Boolean.TRUE.equals(options.getIgnoreErrors())) {
            command.add("--ignore-errors");
        }
        if (Boolean.TRUE.equals(options.getExtractAudio())) {
            command.add("--extract-audio");
        }
        if (options.getAudioFormat() != null) {
            command.add("--audio-format");
            command.add(options.getAudioFormat());
        }
        if (options.getVideoFormat() != null) {
            command.add("--video-format");
            command.add(options.getVideoFormat());
        }
        
        // 添加自定义选项
        for (Map.Entry<String, String> entry : options.getCustomOptions().entrySet()) {
            command.add(entry.getKey());
            if (!entry.getValue().isEmpty()) {
                command.add(entry.getValue());
            }
        }
        
        // 添加自定义命令
        command.addAll(customCommands);
        
        // 添加URL
        command.addAll(urls);
        
        return command;
    }
    
    /**
     * 获取URL列表
     */
    public List<String> getUrls() {
        return urls;
    }
    
    /**
     * 获取下载选项
     */
    public DownloadOptions getOptions() {
        return options;
    }
    
    /**
     * 解析文件大小字符串（如 "100M", "1G" 等）
     */
    private Long parseFileSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isEmpty()) return null;
        
        sizeStr = sizeStr.trim().toUpperCase();
        long multiplier = 1;
        
        if (sizeStr.endsWith("K")) {
            multiplier = 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 1);
        } else if (sizeStr.endsWith("M")) {
            multiplier = 1024 * 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 1);
        } else if (sizeStr.endsWith("G")) {
            multiplier = 1024 * 1024 * 1024;
            sizeStr = sizeStr.substring(0, sizeStr.length() - 1);
        }
        
        try {
            double value = Double.parseDouble(sizeStr);
            return (long) (value * multiplier);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
