package com.btdlp.utils;

import android.util.Log;

/**
 * Android Logger class - equivalent to the logging functionality in Python yt-dlp
 * Uses Android's official Log class with YOUTUBE_DL_DEBUG_TAG
 */
public class Logger {
    
    private static final String TAG = "YOUTUBE_DL_DEBUG_TAG";
    private static boolean quiet = false;
    
    /**
     * 默认构造函数 - 不需要初始化参数
     */
    public Logger() {
        // 使用默认设置，quiet默认为false
    }
    
    /**
     * 兼容性构造函数 - 保留原有接口但不使用参数
     */
    public Logger(boolean verbose, boolean quiet, boolean noWarnings) {
        // 只保留quiet标志，忽略其他参数
        Logger.quiet = quiet;
    }
    
    /**
     * 设置quiet模式
     */
    public static void setQuiet(boolean quiet) {
        Logger.quiet = quiet;
    }
    
    /**
     * 获取quiet状态
     */
    public static boolean isQuiet() {
        return quiet;
    }
    
    /**
     * Print info message using Android Log.i
     */
    public void info(String message) {
        if (!quiet) {
            Log.i(TAG, message);
        }
    }
    
    /**
     * Print info message with format arguments using Android Log.i
     */
    public void info(String format, Object... args) {
        if (!quiet) {
            if (args.length == 0) {
                Log.i(TAG, format);
            } else {
                // 将{}占位符转换为%s，以兼容String.format
                String convertedFormat = format.replace("{}", "%s");
                Log.i(TAG, String.format(convertedFormat, args));
            }
        }
    }
    
    /**
     * Print debug message using Android Log.d
     */
    public void debug(String message) {
        if (!quiet) {
            Log.d(TAG, message);
        }
    }
    
    /**
     * Print debug message with format arguments using Android Log.d
     */
    public void debug(String format, Object... args) {
        if (!quiet) {
            // 将{}占位符转换为%s，以兼容String.format
            String convertedFormat = format.replace("{}", "%s");
            Log.d(TAG, String.format(convertedFormat, args));
        }
    }
    
    /**
     * Print warning message using Android Log.w
     */
    public void warning(String message) {
        if (!quiet) {
            Log.w(TAG, message);
        }
    }
    
    /**
     * Print warning message with format arguments using Android Log.w
     */
    public void warning(String format, Object... args) {
        if (!quiet) {
            // 将{}占位符转换为%s，以兼容String.format
            String convertedFormat = format.replace("{}", "%s");
            Log.w(TAG, String.format(convertedFormat, args));
        }
    }
    
    /**
     * Print error message using Android Log.e
     */
    public void error(String message) {
        if (!quiet) {
            Log.e(TAG, message);
        }
    }
    
    /**
     * Print error message with format arguments using Android Log.e
     */
    public void error(String format, Object... args) {
        if (!quiet) {
            // 将{}占位符转换为%s，以兼容String.format
            String convertedFormat = format.replace("{}", "%s");
            Log.e(TAG, String.format(convertedFormat, args));
        }
    }
    
    /**
     * Print to screen (equivalent to to_screen in Python) using Android Log.i
     */
    public void toScreen(String message) {
        info(message);
    }
    
    /**
     * Print to stdout (equivalent to to_stdout in Python) using Android Log.i
     */
    public void toStdout(String message) {
        info(message);
    }
    
    /**
     * Print to stderr (equivalent to to_stderr in Python) using Android Log.e
     */
    public void toStderr(String message) {
        error(message);
    }
    
    
    /**
     * Static method to log info message
     */
    public static void logInfo(String message) {
        if (!quiet) {
            Log.i(TAG, message);
        }
    }
    
    /**
     * Static method to log debug message
     */
    public static void logDebug(String message) {
        if (!quiet) {
            Log.d(TAG, message);
        }
    }
    
    /**
     * Static method to log warning message
     */
    public static void logWarning(String message) {
        if (!quiet) {
            Log.w(TAG, message);
        }
    }
    
    /**
     * Static method to log error message
     */
    public static void logError(String message) {
        if (!quiet) {
            Log.e(TAG, message);
        }
    }
}
