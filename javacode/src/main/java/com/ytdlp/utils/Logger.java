package com.ytdlp.utils;

/**
 * Logger class - equivalent to the logging functionality in Python yt-dlp
 * Provides different levels of logging based on verbosity settings
 */
public class Logger {
    
    private final boolean verbose;
    private final boolean quiet;
    private final boolean noWarnings;
    
    public Logger(boolean verbose, boolean quiet, boolean noWarnings) {
        this.verbose = verbose;
        this.quiet = quiet;
        this.noWarnings = noWarnings;
    }
    
    /**
     * Print info message to stdout
     */
    public void info(String message) {
        if (!quiet) {
            System.out.println(message);
        }
    }
    
    /**
     * Print info message with format arguments
     */
    public void info(String format, Object... args) {
        if (!quiet) {
            if (args.length == 0) {
                System.out.println(format);
            } else {
                System.out.printf(format + "%n", args);
            }
        }
    }
    
    /**
     * Print debug message to stdout (only if verbose)
     */
    public void debug(String message) {
        if (verbose && !quiet) {
            System.out.println("[debug] " + message);
        }
    }
    
    /**
     * Print debug message with format arguments
     */
    public void debug(String format, Object... args) {
        if (verbose && !quiet) {
            System.out.printf("[debug] " + format + "%n", args);
        }
    }
    
    /**
     * Print warning message to stderr
     */
    public void warning(String message) {
        if (!noWarnings && !quiet) {
            System.err.println("WARNING: " + message);
        }
    }
    
    /**
     * Print warning message with format arguments
     */
    public void warning(String format, Object... args) {
        if (!noWarnings && !quiet) {
            System.err.printf("WARNING: " + format + "%n", args);
        }
    }
    
    /**
     * Print error message to stderr
     */
    public void error(String message) {
        if (!quiet) {
            System.err.println("ERROR: " + message);
        }
    }
    
    /**
     * Print error message with format arguments
     */
    public void error(String format, Object... args) {
        if (!quiet) {
            System.err.printf("ERROR: " + format + "%n", args);
        }
    }
    
    /**
     * Print to stdout (equivalent to to_screen in Python)
     */
    public void toScreen(String message) {
        info(message);
    }
    
    /**
     * Print to stdout (equivalent to to_stdout in Python)
     */
    public void toStdout(String message) {
        info(message);
    }
    
    /**
     * Print to stderr (equivalent to to_stderr in Python)
     */
    public void toStderr(String message) {
        error(message);
    }
    
    /**
     * Check if verbose logging is enabled
     */
    public boolean isVerbose() {
        return verbose;
    }
    
    /**
     * Check if quiet mode is enabled
     */
    public boolean isQuiet() {
        return quiet;
    }
    
    /**
     * Check if warnings are disabled
     */
    public boolean isNoWarnings() {
        return noWarnings;
    }
}
