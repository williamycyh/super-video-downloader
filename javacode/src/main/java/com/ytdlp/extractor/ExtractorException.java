package com.ytdlp.extractor;

/**
 * Exception thrown when video extraction fails
 */
public class ExtractorException extends Exception {
    
    public ExtractorException(String message) {
        super(message);
    }
    
    public ExtractorException(String message, Throwable cause) {
        super(message, cause);
    }
}
