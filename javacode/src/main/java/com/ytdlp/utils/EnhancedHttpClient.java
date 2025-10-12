package com.ytdlp.utils;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.HashMap;

/**
 * 增强的HTTP客户端
 */
public class EnhancedHttpClient {
    
    private static final String TAG = "EnhancedHttpClient";
    
    private CookieManager cookieManager;
    private String userAgent;
    private int connectTimeout = 30000; // 30秒
    private int readTimeout = 60000; // 60秒
    private int maxRetries = 3;
    private int retryDelay = 1000; // 1秒
    
    public EnhancedHttpClient() {
        this.cookieManager = new CookieManager();
        this.userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    }
    
    public EnhancedHttpClient(CookieManager cookieManager) {
        this.cookieManager = cookieManager;
        this.userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
    }
    
    public HttpResponse get(String url) throws Exception {
        return get(url, null);
    }
    
    public byte[] getBinary(String url) throws Exception {
        return getBinary(url, null);
    }
    
    public byte[] getBinary(String url, Map<String, String> headers) throws Exception {
        HttpURLConnection connection = createConnection(url, "GET", headers);
        
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode);
            }
            
            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            return outputStream.toByteArray();
            
        } finally {
            connection.disconnect();
        }
    }
    
    public HttpResponse get(String url, Map<String, String> headers) throws Exception {
        HttpURLConnection connection = createConnection(url, "GET", headers);
        
        try {
            int responseCode = connection.getResponseCode();
            String content = readResponseContent(connection);
            
            // 解析响应中的Cookie
            parseResponseCookies(url, connection);
            
            return new HttpResponse(responseCode, content, connection.getHeaderFields());
            
        } finally {
            connection.disconnect();
        }
    }
    
    public HttpResponse post(String url, String data, String contentType) throws Exception {
        return post(url, data, contentType, null);
    }
    
    public HttpResponse post(String url, String data, String contentType, Map<String, String> headers) throws Exception {
        HttpURLConnection connection = createConnection(url, "POST", headers);
        
        try {
            connection.setRequestProperty("Content-Type", contentType);
            connection.setDoOutput(true);
            
            // 写入数据
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(data.getBytes("UTF-8"));
            }
            
            int responseCode = connection.getResponseCode();
            String content = readResponseContent(connection);
            
            // 解析响应中的Cookie
            parseResponseCookies(url, connection);
            
            return new HttpResponse(responseCode, content, connection.getHeaderFields());
            
        } finally {
            connection.disconnect();
        }
    }
    
    private HttpURLConnection createConnection(String url, String method, Map<String, String> headers) throws Exception {
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        
        connection.setRequestMethod(method);
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setInstanceFollowRedirects(true);
        
        // 设置默认头部
        setDefaultHeaders(connection, url);
        
        // 设置自定义头部
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        
        // 设置Cookie
        if (cookieManager != null) {
            String cookies = cookieManager.getCookies(url);
            if (cookies != null && !cookies.isEmpty()) {
                connection.setRequestProperty("Cookie", cookies);
            }
        }
        
        return connection;
    }
    
    private void setDefaultHeaders(HttpURLConnection connection, String url) {
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.setRequestProperty("Connection", "keep-alive");
        connection.setRequestProperty("Upgrade-Insecure-Requests", "1");
        
        // 设置Referer
        String domain = extractDomain(url);
        connection.setRequestProperty("Referer", domain);
    }
    
    private void parseResponseCookies(String url, HttpURLConnection connection) {
        if (cookieManager == null) {
            return;
        }
        
        Map<String, java.util.List<String>> headerFields = connection.getHeaderFields();
        java.util.List<String> cookies = headerFields.get("Set-Cookie");
        
        if (cookies != null) {
            for (String cookie : cookies) {
                cookieManager.addCookie(url, cookie);
            }
        }
    }
    
    private String readResponseContent(HttpURLConnection connection) throws Exception {
        InputStream inputStream;
        
        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }
        
        if (inputStream == null) {
            return "";
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }
    
    private String extractDomain(String url) {
        try {
            URL urlObj = new URL(url);
            return urlObj.getProtocol() + "://" + urlObj.getHost();
        } catch (MalformedURLException e) {
            return "https://www.google.com/";
        }
    }
    
    public boolean downloadFile(String url, String outputPath) throws Exception {
        byte[] data = getBinaryWithRetry(url, null);
        
        try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
            outputStream.write(data);
            return true;
        }
    }
    
    public byte[] getBinaryWithRetry(String url, Map<String, String> headers) throws Exception {
        Exception lastException = null;
        
        for (int i = 0; i < maxRetries; i++) {
            try {
                return getBinary(url, headers);
            } catch (Exception e) {
                lastException = e;
                if (i < maxRetries - 1) {
                    Thread.sleep(retryDelay * (i + 1)); // 递增延迟
                }
            }
        }
        
        throw lastException;
    }
    
    // Getters and Setters
    public CookieManager getCookieManager() {
        return cookieManager;
    }
    
    public void setCookieManager(CookieManager cookieManager) {
        this.cookieManager = cookieManager;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public int getConnectTimeout() {
        return connectTimeout;
    }
    
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
    
    public int getReadTimeout() {
        return readTimeout;
    }
    
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public int getRetryDelay() {
        return retryDelay;
    }
    
    public void setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
    }
    
    /**
     * HTTP响应类
     */
    public static class HttpResponse {
        private int statusCode;
        private String content;
        private Map<String, java.util.List<String>> headers;
        
        public HttpResponse(int statusCode, String content, Map<String, java.util.List<String>> headers) {
            this.statusCode = statusCode;
            this.content = content;
            this.headers = headers != null ? headers : new HashMap<>();
        }
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public String getContent() {
            return content;
        }
        
        public Map<String, java.util.List<String>> getHeaders() {
            return headers;
        }
        
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
        
        public String getHeader(String name) {
            java.util.List<String> values = headers.get(name);
            return values != null && !values.isEmpty() ? values.get(0) : null;
        }
    }
}
