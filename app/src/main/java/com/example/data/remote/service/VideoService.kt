package com.example.data.remote.service

import com.example.data.local.model.Proxy
import com.example.data.local.model.VideoInfoWrapper
import com.example.data.local.room.entity.VideFormatEntityList
import com.example.data.local.room.entity.VideoFormatEntity
import com.example.data.local.room.entity.VideoInfo
import com.example.util.AppLogger
import com.example.util.CookieUtils
import com.example.util.proxy_utils.CustomProxyController
import com.btdlp.BtdJava
import com.btdlp.core.VideoInfo as BtdVideoInfo
import com.btdlp.core.VideoFormat as BtdVideoFormat
import okhttp3.Request
import java.util.Locale

interface VideoService {
    fun getVideoInfo(
        url: Request,
        isM3u8OrMpd: Boolean = false,
        isAudioCheck: Boolean
    ): VideoInfoWrapper?
}

open class VideoServiceLocal(
    private val proxyController: CustomProxyController
) : VideoService {
    companion object {
        const val MP4_EXT = "mp4"
        private const val FACEBOOK_HOST = ".facebook."
        private const val COOKIE_HEADER = "Cookie"
    }

    override fun getVideoInfo(
        url: Request,
        isM3u8OrMpd: Boolean,
        isAudioCheck: Boolean
    ): VideoInfoWrapper? {
        AppLogger.d("Getting info url...:  $url  ${url.headers["Cookie"]}")

        var result: VideoInfoWrapper? = null

        try {
            result = handleYoutubeDlUrl(url, isM3u8OrMpd, isAudioCheck)
        } catch (e: Throwable) {
            AppLogger.d("BubeDL Error: $e")
        }

        return result
    }

    private fun handleYoutubeDlUrl(
        url: Request,
        isM3u8OrMpd: Boolean = false,
        isAudioCheck: Boolean
    ): VideoInfoWrapper {
        try {
            AppLogger.d("VideoService: Starting video info extraction for URL: ${url.url}")
            val btdJava = BtdJava()
            
            AppLogger.d("VideoService: BtdJava instance created successfully")
            
            // Extract video info using custom library
            val btdVideoInfo = btdJava.extractInfo(url.url.toString())
            
            if (btdVideoInfo == null) {
                AppLogger.e("VideoService: Failed to extract video info for URL: ${url.url}")
                throw Exception("Failed to extract video info")
            }
            
            AppLogger.d("VideoService: Successfully extracted video info: ${btdVideoInfo.getTitle()}")
            AppLogger.d("VideoService: Found ${btdVideoInfo.getFormats().size} formats")
            
            // 打印前几个格式的信息用于调试
            btdVideoInfo.getFormats().take(3).forEachIndexed { index, format ->
                AppLogger.d("VideoService: Format $index - ID: ${format.getFormatId()}, Ext: ${format.getExt()}, Protocol: ${format.getProtocol()}")
            }
            
            // Convert custom library formats to our format
            val formats = btdVideoInfo.getFormats().map { videoEntityFromYtDlpFormat(it) }
            val filtered = if (url.url.toString().contains(FACEBOOK_HOST)) {
                formats.filter {
                    it.formatId?.lowercase(Locale.ROOT)?.contains(Regex("hd|sd")) == true
                }
            } else {
                emptyList()
            }

            val listFormats = VideFormatEntityList(filtered.ifEmpty {
                if (isAudioCheck) {
                    formats
                } else {
                    formats.filter { it.vcodec != "none" || it.acodec == "none" }
                }
            })

            return VideoInfoWrapper(
                VideoInfo(
                    title = btdVideoInfo.getTitle() ?: "no title", formats = listFormats
                ).apply {
                    ext = MP4_EXT
                    thumbnail = btdVideoInfo.getThumbnail() ?: ""
                    duration = btdVideoInfo.getDuration()?.toLong() ?: 0L
                    originalUrl = url.url.toString()
                    downloadUrls = if (isM3u8OrMpd) emptyList() else listOf(url)
                    isRegularDownload = false
                })
        } catch (e: Throwable) {
            AppLogger.e("VideoService: Exception during extraction: ${e.message}")
            AppLogger.e("VideoService: Exception stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }

    // attachProxyToRequest method removed - no longer needed with custom library

    private fun videoEntityFromYtDlpFormat(ytdlpFormat: BtdVideoFormat): VideoFormatEntity {
        return VideoFormatEntity(
            asr = ytdlpFormat.getAsr() ?: 0,
            tbr = ytdlpFormat.getTbr() ?: 0,
            abr = ytdlpFormat.getAbr() ?: 0,
            formatId = ytdlpFormat.getFormatId(),
            formatNote = ytdlpFormat.getFormatNote(),
            ext = ytdlpFormat.getExt(),
            httpHeaders = ytdlpFormat.getHttpHeaders(),
            acodec = ytdlpFormat.getAcodec(),
            vcodec = ytdlpFormat.getVcodec(),
            url = ytdlpFormat.getUrl(),
            width = ytdlpFormat.getWidth() ?: 0,
            height = ytdlpFormat.getHeight() ?: 0,
            fps = ytdlpFormat.getFps() ?: 0,
            fileSize = ytdlpFormat.getFilesize() ?: 0
        )
    }

    private fun videoEntityFromFormat(videoFormat: BtdVideoFormat): VideoFormatEntity {
        return VideoFormatEntity(
            asr = videoFormat.getAsr() ?: 0,
            tbr = videoFormat.getTbr() ?: 0,
            abr = videoFormat.getAbr() ?: 0,
            format = videoFormat.getFormat(),
            formatId = videoFormat.getFormatId(),
            formatNote = videoFormat.getFormatNote(),
            ext = videoFormat.getExt(),
            preference = videoFormat.getPreference() ?: 0,
            vcodec = videoFormat.getVcodec(),
            acodec = videoFormat.getAcodec(),
            width = videoFormat.getWidth() ?: 0,
            height = videoFormat.getHeight() ?: 0,
            fileSize = videoFormat.getFilesize() ?: 0,
            fileSizeApproximate = videoFormat.getFilesizeApprox() ?: 0,
            fps = videoFormat.getFps() ?: 0,
            url = videoFormat.getUrl(),
            manifestUrl = null, // Not available in our VideoFormat class
            httpHeaders = videoFormat.getHttpHeaders()
        )
    }
}
