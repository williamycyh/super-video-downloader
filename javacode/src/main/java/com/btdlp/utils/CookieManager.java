package com.btdlp.utils;

import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Cookie管理器
 */
public class CookieManager {
    
    private Map<String, List<String>> cookies;
    
    public CookieManager() {
        this.cookies = new HashMap<>();
    }
    
    /**
     * 添加Cookie
     */
    public void addCookie(String url, String cookieHeader) {
        try {
            String domain = extractDomain(url);
            List<String> domainCookies = cookies.computeIfAbsent(domain, k -> new ArrayList<>());
            
            // 解析Cookie头部
            String[] cookiePairs = cookieHeader.split(";");
            if (cookiePairs.length > 0) {
                String cookiePair = cookiePairs[0].trim();
                if (!domainCookies.contains(cookiePair)) {
                    domainCookies.add(cookiePair);
                }
            }
        } catch (Exception e) {
            // 忽略Cookie解析错误
        }
    }
    
    /**
     * 获取指定URL的Cookie
     */
    public String getCookies(String url) {
        try {
            String domain = extractDomain(url);
            List<String> domainCookies = cookies.get(domain);
            
            if (domainCookies != null && !domainCookies.isEmpty()) {
                return String.join("; ", domainCookies);
            }
        } catch (Exception e) {
            // 忽略错误
        }
        
        return null;
    }
    
    /**
     * 清除所有Cookie
     */
    public void clearCookies() {
        cookies.clear();
    }
    
    /**
     * 清除指定域的Cookie
     */
    public void clearCookies(String domain) {
        cookies.remove(domain);
    }
    
    /**
     * 提取域名
     */
    private String extractDomain(String url) {
        try {
            URL urlObj = new URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            return url;
        }
    }
}
