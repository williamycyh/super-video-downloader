package com.example.util

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.tubedown.rereads.MyCommon
import com.example.tubedown.rereads.Utils
import com.example.util.AppLogger
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 网站拦截管理类
 * 用于管理被拦截的网站列表和相关的拦截逻辑
 */
object BlockedWebsitesManager {

    /**
     * 获取所有被拦截的域名列表
     * @return 被拦截域名列表的副本
     */
    fun getBlockedDomains(): List<String> {
        val baseDomains = when {
            Utils.appCon.f_type <= 0 -> blockedDomains_0.toList()
            Utils.appCon.f_type == 1L -> blockedDomains_strict_1.toList()
            Utils.appCon.f_type == 2L -> blockedDomains_strict_2.toList()
            else -> blockedDomains_0.toList()
        }
        
        // 添加云端下发的被拦截域名
        val cloudBlockedDomains = getCloudBlockedDomains()
        
        return baseDomains + cloudBlockedDomains
    }
    
    /**
     * 从云端配置中解析被拦截的域名列表
     * @return 云端配置的域名列表
     */
    private fun getCloudBlockedDomains(): List<String> {
        val blockUrls = Utils.appCon.block_urls
        if (blockUrls.isNullOrEmpty()) {
            return emptyList()
        }
        
        return try {
            // 按逗号分隔并清理每个域名
            val domains = blockUrls.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { domain ->
                    // 清理域名，移除协议和路径
                    extractHostFromString(domain) ?: domain.lowercase()
                }
                .filter { it.isNotEmpty() }
            
            // 调试日志
            if (domains.isNotEmpty()) {
                AppLogger.d("BlockedWebsitesManager: Cloud blocked domains loaded: ${domains.joinToString(", ")}")
            }
            
            domains
        } catch (e: Exception) {
            AppLogger.e("BlockedWebsitesManager: Error parsing cloud blocked domains: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 获取云端配置的原始字符串（用于调试）
     * @return 云端配置的原始字符串
     */
    fun getCloudBlockedUrlsRaw(): String {
        return Utils.appCon.block_urls ?: ""
    }
    
    /**
     * 获取云端配置解析后的域名列表（用于调试）
     * @return 云端配置解析后的域名列表
     */
    fun getCloudBlockedDomainsDebug(): List<String> {
        return getCloudBlockedDomains()
    }
    
    /**
     * 测试云端配置功能（用于调试）
     * @param testUrls 测试用的URL字符串，格式：domain1,domain2,domain3
     * @return 解析后的域名列表
     */
    fun testCloudBlockedDomains(testUrls: String): List<String> {
        if (testUrls.isEmpty()) {
            return emptyList()
        }
        
        return try {
            testUrls.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { domain ->
                    extractHostFromString(domain) ?: domain.lowercase()
                }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 检查URL是否属于被拦截的网站
     * @param url 要检查的URL
     * @return true如果网站被拦截，false否则
     */
    fun isBlockedWebsite(url: String?): Boolean {
        if (url == null) return false
        
        try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase()
            
            return getBlockedDomains().any { blockedDomain ->
                isHostBlocked(host, blockedDomain.lowercase())
            }
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * 检查主机名是否匹配被拦截的域名（支持通配符）
     * @param host 要检查的主机名
     * @param blockedDomain 被拦截的域名（可能包含通配符）
     * @return true如果匹配，false否则
     */
    private fun isHostBlocked(host: String?, blockedDomain: String): Boolean {
        if (host == null) return false
        
        // 确保host是纯域名格式，不包含路径、协议等
        val cleanHost = extractHostFromString(host)
        if (cleanHost == null) return false
        
        return when {
            // 精确匹配
            cleanHost == blockedDomain -> true
            
            // 通配符匹配 (*.example.com)
            blockedDomain.startsWith("*.") -> {
                val baseDomain = blockedDomain.substring(2) // 移除 "*."
                cleanHost == baseDomain || cleanHost.endsWith(".$baseDomain")
            }
            
            // 普通子域名匹配 (example.com 匹配 www.example.com)
            else -> cleanHost.endsWith(".$blockedDomain")
        }
    }
    
    /**
     * 从字符串中提取纯域名部分
     * 处理可能的完整URL格式，确保只返回域名部分
     * @param input 可能包含完整URL的字符串
     * @return 纯域名，如果无法提取则返回null
     */
    private fun extractHostFromString(input: String): String? {
        return try {
            // 如果输入包含协议，使用Uri解析
            if (input.contains("://")) {
                Uri.parse(input).host?.lowercase()
            } else {
                // 如果输入包含路径，提取域名部分
                val hostPart = input.split("/")[0].split("?")[0].split("#")[0]
                // 验证是否为有效的域名格式（包含点号且不包含特殊字符）
                if (hostPart.contains(".") && !hostPart.contains(" ") && !hostPart.contains("\\")) {
                    hostPart.lowercase()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 显示被拦截网站的提示弹窗
     * @param context 上下文
     * @param onPositiveClick 点击确定按钮时的回调函数，可选
     */
    fun showBlockedWebsiteDialog(context: Context, onPositiveClick: (() -> Unit)? = null) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.blocked_website_title))
            .setMessage(context.getString(R.string.blocked_website_message))
            .setPositiveButton(context.getString(R.string.blocked_website_ok)) { dialog, _ ->
                onPositiveClick?.invoke()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    
    /**
     * 获取支持的网站列表
     * @return 支持的网站列表
//     */
//    fun getSupportedWebsites(): List<String> {
//        return supportedWebsites.toList()
//    }
//


    // 被拦截的网站域名列表
    /**
     * youtube,soundcloud
     */
    private val blockedDomains_0 = mutableSetOf(
        // YouTube 相关域名
        "youtube.com",
        "www.youtube.com",
        "youtu.be",
        "m.youtube.com",
        "*.youtube.com",      // 拦截所有YouTube子域名
        "*.googlevideo.com",  // 拦截YouTube视频CDN
        "*.ytimg.com",        // 拦截YouTube图片CDN
        "s.ytimg.com",
        "ytimg.l.google.com",
        "youtube.l.google.com",
        "youtube-ui.l.google.com",
        "youtubei.googleapis.com",
        "youtube.googleapis.com",
        "googlevideo.com",
        "*.youtubei.googleapis.com",
        "*.youtube.googleapis.com",
        "*.googleusercontent.com",
        "*.gstatic.com",
        "*.googleapis.com",
        "*.youtube-nocookie.com",

        // SoundCloud 相关域名
        "soundcloud.com",
        "www.soundcloud.com",
        "m.soundcloud.com",
        "*.soundcloud.com",
        "*.sc-static.net",
        "*.sndcdn.com",

        "netflix.com",
        "disneyplus.com",
        "disney.com",
        "tv.apple.com",
        "spotify.com",
    )

    /**
     * youtube,soundcloud, fb, instagram
     */
    private val blockedDomains_strict_1 = mutableSetOf(
        // YouTube 相关域名
        "youtube.com",
        "www.youtube.com",
        "youtu.be",
        "m.youtube.com",
        "*.youtube.com",      // 拦截所有YouTube子域名
        "*.googlevideo.com",  // 拦截YouTube视频CDN
        "*.ytimg.com",        // 拦截YouTube图片CDN
        "s.ytimg.com",
        "ytimg.l.google.com",
        "youtube.l.google.com",
        "youtube-ui.l.google.com",
        "youtubei.googleapis.com",
        "youtube.googleapis.com",
        "googlevideo.com",
        "*.youtubei.googleapis.com",
        "*.youtube.googleapis.com",
        "*.googleusercontent.com",
        "*.gstatic.com",
        "*.googleapis.com",
        "*.youtube-nocookie.com",

        // SoundCloud 相关域名
        "soundcloud.com",
        "www.soundcloud.com",
        "m.soundcloud.com",
        "*.soundcloud.com",
        "*.sc-static.net",
        "*.sndcdn.com",

        "netflix.com",
        "disneyplus.com",
        "disney.com",
        "tv.apple.com",
        "spotify.com",

        //fb, instagram
        "facebook.com",
        "fb.watch",
        "instagram.com",
    )

    private val blockedDomains_strict_2 = mutableSetOf(
        // YouTube 相关域名
        "youtube.com",
        "www.youtube.com",
        "youtu.be",
        "m.youtube.com",
        "*.youtube.com",      // 拦截所有YouTube子域名
        "*.googlevideo.com",  // 拦截YouTube视频CDN
        "*.ytimg.com",        // 拦截YouTube图片CDN
        "s.ytimg.com",
        "ytimg.l.google.com",
        "youtube.l.google.com",
        "youtube-ui.l.google.com",
        "youtubei.googleapis.com",
        "youtube.googleapis.com",
        "googlevideo.com",
        "*.youtubei.googleapis.com",
        "*.youtube.googleapis.com",
        "*.googleusercontent.com",
        "*.gstatic.com",
        "*.googleapis.com",
        "*.youtube-nocookie.com",

        // SoundCloud 相关域名
        "soundcloud.com",
        "www.soundcloud.com",
        "m.soundcloud.com",
        "*.soundcloud.com",
        "*.sc-static.net",
        "*.sndcdn.com",

        "netflix.com",
        "disneyplus.com",
        "disney.com",
        "tv.apple.com",
        "spotify.com",

        //fb, instagram,tiktok, twitter,x
        "facebook.com",
        "fb.watch",
        "instagram.com",
        "tiktok.com",
        "twitter.com",
        "x.com"
    )
}
