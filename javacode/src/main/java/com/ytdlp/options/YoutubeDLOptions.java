package com.ytdlp.options;

import java.util.HashMap;
import java.util.Map;

/**
 * Options for YoutubeDL operations
 */
public class YoutubeDLOptions {
    private Map<String, Object> options;

    public YoutubeDLOptions() {
        this.options = new HashMap<>();
        setDefaultOptions();
    }

    private void setDefaultOptions() {
        // Set default options
        options.put("format", "best");
        options.put("output", "%(title)s.%(ext)s");
        options.put("no_warnings", false);
        options.put("ignore_errors", false);
    }

    public Object get(String key) {
        return options.get(key);
    }

    public void set(String key, Object value) {
        options.put(key, value);
    }

    public String getString(String key) {
        Object value = options.get(key);
        return value != null ? value.toString() : null;
    }

    public Integer getInt(String key) {
        Object value = options.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public Boolean getBoolean(String key) {
        Object value = options.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    public Map<String, Object> getAll() {
        return new HashMap<>(options);
    }
    
    /**
     * 设置HTTP头部信息
     */
    public void setHttpHeaders(String httpHeaders) {
        options.put("http_headers", httpHeaders);
    }
    
    /**
     * 获取HTTP头部信息
     */
    public String getHttpHeaders() {
        return getString("http_headers");
    }
}
