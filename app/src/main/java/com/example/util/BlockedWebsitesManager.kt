package com.example.util

import android.content.Context
import android.net.Uri
import com.example.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 网站拦截管理类
 * 用于管理被拦截的网站列表和相关的拦截逻辑
 */
object BlockedWebsitesManager {
    
    // 被拦截的网站域名列表
    private val blockedDomains = mutableSetOf(
        // YouTube 相关域名
        "youtube.com",
        "www.youtube.com", 
        "youtu.be",
        "m.youtube.com",
        "*.youtube.com",      // 拦截所有YouTube子域名
        "*.googlevideo.com",  // 拦截YouTube视频CDN
        "*.ytimg.com",        // 拦截YouTube图片CDN
        
        // SoundCloud 相关域名
        "soundcloud.com",
        "www.soundcloud.com",
        "m.soundcloud.com"
    )
    
    // 支持的网站列表（用于提示用户）
//    private val supportedWebsites = listOf(
//        "TikTok",
//        "Twitter",
//        "Dailymotion",
//        "Pinterest"
//    )
    
    /**
     * 添加新的被拦截域名
     * @param domain 要添加的域名
     */
    fun addBlockedDomain(domain: String) {
        blockedDomains.add(domain.lowercase())
    }
    
    /**
     * 添加多个被拦截域名
     * @param domains 要添加的域名列表
     */
    fun addBlockedDomains(domains: List<String>) {
        domains.forEach { domain ->
            blockedDomains.add(domain.lowercase())
        }
    }
    
    /**
     * 移除被拦截的域名
     * @param domain 要移除的域名
     */
    fun removeBlockedDomain(domain: String) {
        blockedDomains.remove(domain.lowercase())
    }
    
    /**
     * 获取所有被拦截的域名列表
     * @return 被拦截域名列表的副本
     */
    fun getBlockedDomains(): List<String> {
        return blockedDomains.toList()
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
            
            return blockedDomains.any { blockedDomain ->
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
     */
    fun showBlockedWebsiteDialog(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.blocked_website_title))
            .setMessage(context.getString(R.string.blocked_website_message))
            .setPositiveButton(context.getString(R.string.blocked_website_ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
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
    /**
     * 添加新的支持网站
     * @param website 要添加的支持网站名称
     */
    fun addSupportedWebsite(website: String) {
        // 这里使用反射来修改不可变列表，或者重新创建列表
        // 为了简单起见，我们提供一个方法来更新支持网站列表
        throw UnsupportedOperationException("Use updateSupportedWebsites() to modify supported websites list")
    }
    
    /**
     * 更新支持的网站列表
     * @param websites 新的支持网站列表
     */
    fun updateSupportedWebsites(websites: List<String>) {
        // 注意：由于supportedWebsites是private val，这里需要重新设计
        // 或者提供一个方法来获取更新后的消息
        throw UnsupportedOperationException("Supported websites list is currently immutable. Consider redesigning this class.")
    }
    
//    /**
//     * 获取自定义的被拦截网站弹窗消息
//     * @param customSupportedWebsites 自定义的支持网站列表
//     */
//    fun getCustomBlockedWebsiteMessage(customSupportedWebsites: List<String>): String {
//        val supportedList = customSupportedWebsites.joinToString("\n• ", "• ")
//        return "此网站不支持视频下载功能。\n\n支持的网站包括：\n$supportedList\n\n请访问其他支持下载的网站。"
//    }
}
