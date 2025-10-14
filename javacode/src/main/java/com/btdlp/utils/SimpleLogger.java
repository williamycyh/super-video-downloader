package com.btdlp.utils;

/**
 * 简化的日志记录器，替代SLF4J依赖
 */
public class SimpleLogger {
    
    private final boolean debug;
    private final boolean verbose;
    private final boolean quiet;
    
    public SimpleLogger(boolean debug, boolean verbose, boolean quiet) {
        this.debug = debug;
        this.verbose = verbose;
        this.quiet = quiet;
    }
    
    public void info(String message) {
        if (!quiet) {
            System.out.println("[INFO] " + message);
        }
    }
    
    public void info(String format, Object... args) {
        if (!quiet) {
            System.out.println("[INFO] " + String.format(format, args));
        }
    }
    
    public void error(String message) {
        if (!quiet) {
            System.err.println("[ERROR] " + message);
        }
    }
    
    public void error(String format, Object... args) {
        if (!quiet) {
            System.err.println("[ERROR] " + String.format(format, args));
        }
    }
    
    public void debug(String message) {
        if (debug && !quiet) {
            System.out.println("[DEBUG] " + message);
        }
    }
    
    public void debug(String format, Object... args) {
        if (debug && !quiet) {
            System.out.println("[DEBUG] " + String.format(format, args));
        }
    }
    
    public void verbose(String message) {
        if (verbose && !quiet) {
            System.out.println("[VERBOSE] " + message);
        }
    }
    
    public void verbose(String format, Object... args) {
        if (verbose && !quiet) {
            System.out.println("[VERBOSE] " + String.format(format, args));
        }
    }
}
