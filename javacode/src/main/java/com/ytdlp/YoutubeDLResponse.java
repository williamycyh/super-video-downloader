package com.ytdlp;

import java.util.List;

/**
 * 兼容Python版本的YoutubeDLResponse类
 */
public class YoutubeDLResponse {
    private List<String> command;
    private int exitCode;
    private long elapsedTime;
    private String out;
    private String err;
    
    public YoutubeDLResponse(List<String> command, int exitCode, long elapsedTime, String out, String err) {
        this.command = command;
        this.exitCode = exitCode;
        this.elapsedTime = elapsedTime;
        this.out = out;
        this.err = err;
    }
    
    /**
     * 获取执行的命令
     */
    public List<String> getCommand() {
        return command;
    }
    
    /**
     * 获取退出码（0表示成功）
     */
    public int getExitCode() {
        return exitCode;
    }
    
    /**
     * 获取执行时间（毫秒）
     */
    public long getElapsedTime() {
        return elapsedTime;
    }
    
    /**
     * 获取标准输出
     */
    public String getOut() {
        return out;
    }
    
    /**
     * 获取错误输出
     */
    public String getErr() {
        return err;
    }
    
    /**
     * 检查是否成功（exitCode == 0）
     */
    public boolean isSuccess() {
        return exitCode == 0;
    }
    
    /**
     * 检查是否失败
     */
    public boolean isFailure() {
        return exitCode != 0;
    }
    
    /**
     * 获取完整的输出（out + err）
     */
    public String getFullOutput() {
        StringBuilder sb = new StringBuilder();
        if (out != null && !out.isEmpty()) {
            sb.append("STDOUT:\n").append(out);
        }
        if (err != null && !err.isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("STDERR:\n").append(err);
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("YoutubeDLResponse{exitCode=%d, elapsedTime=%dms, success=%s}", 
            exitCode, elapsedTime, isSuccess());
    }
}
