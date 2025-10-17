package com.example.tubedown.main.home.browser.detectedVideos

import android.webkit.CookieManager
import androidx.core.net.toUri
import androidx.databinding.Observable
import androidx.databinding.Observable.OnPropertyChangedCallback
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.model.VideoInfoWrapper
import com.example.data.local.room.entity.VideFormatEntityList
import com.example.data.local.room.entity.VideoFormatEntity
import com.example.data.local.room.entity.VideoInfo
import com.example.data.repository.VideoRepository
import com.example.tubedown.main.base.BaseViewModel
import com.example.tubedown.main.home.browser.BrowserFragment
import com.example.tubedown.main.home.browser.DownloadButtonState
import com.example.tubedown.main.home.browser.DownloadButtonStateCanDownload
import com.example.tubedown.main.home.browser.DownloadButtonStateCanNotDownload
import com.example.tubedown.main.home.browser.DownloadButtonStateLoading
import com.example.tubedown.main.home.browser.webTab.WebTabViewModel
import com.example.tubedown.main.settings.SettingsViewModel
import com.example.util.AppLogger
import com.example.util.ContextUtils
import com.example.util.CookieUtils
import com.example.util.SingleLiveEvent
import com.example.util.proxy_utils.OkHttpProxyClient
import com.example.util.scheduler.BaseSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Headers.Companion.toHeaders
import okhttp3.Request
import java.net.HttpCookie
import java.net.URL
import java.util.concurrent.Executors
import javax.inject.Inject

open class VideoDetectionTabViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val baseSchedulers: BaseSchedulers,
    private val okHttpProxyClient: OkHttpProxyClient,
) : BaseViewModel(), IVideoDetector {
    // key: videoInfo.id, value: format - string
    val selectedFormats = ObservableField<Map<String, String>>()

    // key: videoInfo.id, value: title - string
    val formatsTitles = ObservableField<Map<String, String>>()

    val selectedFormatUrl = ObservableField<String>()

    @Volatile
    var m3u8LoadingList = ObservableField<MutableSet<String>>()

    @Volatile
    var regularLoadingList = ObservableField<MutableSet<String>>()

    val showDetectedVideosEvent = SingleLiveEvent<Void?>()

    val videoPushedEvent = SingleLiveEvent<Void?>()

    @Volatile
    var downloadButtonState =
        ObservableField<DownloadButtonState>(DownloadButtonStateCanNotDownload())

    val executorReload = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    var webTabModel: WebTabViewModel? = null
    lateinit var settingsModel: SettingsViewModel
    val detectedVideosList = ObservableField(setOf<VideoInfo>())

    val filterRegex =
        Regex("^(.*\\.(apk|html|xml|ico|css|js|png|gif|json|jpg|jpeg|svg|woff|woff2|m3u8|mpd|ts|php|ttf|otf|eot|cur|webp|bmp|tif|tiff|psd|ai|eps|pdf|doc|docx|xls|xlsx|ppt|pptx|csv|md|rtf|vtt|srt|swf|jar|log|txt))?$")
    val downloadButtonIcon = ObservableInt(R.drawable.invisible_24px)

    @Volatile
    var verifyVideoLinkJobStorage = mutableMapOf<String, Disposable>()

    private val hasCheckLoadingsM3u8 = ObservableBoolean(false)
    private val hasCheckLoadingsRegular = ObservableBoolean(false)

    private val executorRegular = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    override fun start() {
        AppLogger.d("START")
        regularLoadingList.addOnPropertyChangedCallback(object : OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                val notEmpty = regularLoadingList.get()?.isNotEmpty() == true
                hasCheckLoadingsRegular.set(notEmpty)
                if (notEmpty) {
                    setButtonState(DownloadButtonStateCanNotDownload())
                }
            }
        })
        m3u8LoadingList.addOnPropertyChangedCallback(object : OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                val notEmpty = m3u8LoadingList.get()?.isNotEmpty() == true
                hasCheckLoadingsM3u8.set(notEmpty)
                if (notEmpty) {
                    setButtonState(DownloadButtonStateCanNotDownload())
                }
            }
        })
        downloadButtonState.addOnPropertyChangedCallback(object : OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                when (downloadButtonState.get()) {
                    is DownloadButtonStateCanNotDownload -> downloadButtonIcon.set(R.drawable.refresh_24px)
                    is DownloadButtonStateCanDownload -> downloadButtonIcon.set(R.drawable.ic_download_24dp)
                    is DownloadButtonStateLoading -> {
                        downloadButtonIcon.set(R.drawable.invisible_24px)
                    }
                }
            }
        })
    }

    override fun stop() {
        AppLogger.d("STOP")
        cancelAllCheckJobs()
    }

    override fun onStartPage(url: String, userAgentString: String) {
        downloadButtonState.set(DownloadButtonStateCanNotDownload())

        detectedVideosList.set(mutableSetOf())
        cancelAllCheckJobs()

        val req = getRequestWithHeadersForUrl(
            url, url, userAgentString
        )?.build()

        if (req != null) {
            verifyLinkStatus(req)
        }
    }

    override fun hasCheckLoadingsRegular(): ObservableBoolean {
        return hasCheckLoadingsRegular
    }

    override fun hasCheckLoadingsM3u8(): ObservableBoolean {
        return hasCheckLoadingsM3u8
    }

    override fun showVideoInfo() {
        AppLogger.d("VideoDetectionTabViewModel: showVideoInfo() called")
        val state = downloadButtonState.get()
        AppLogger.d("VideoDetectionTabViewModel: Current button state: $state")
        AppLogger.d("VideoDetectionTabViewModel: Detected videos list size: ${detectedVideosList.get()?.size ?: 0}")

        if (state is DownloadButtonStateCanNotDownload) {
            webTabModel?.getTabTextInput()?.get()?.let { url ->
                if (url.startsWith("http")) {
                    // 检查是否为被拦截的网站
                    if (com.example.util.BlockedWebsitesManager.isBlockedWebsite(url)) {
                        AppLogger.d("VideoDetectionTabViewModel: Blocked website detected, skipping video analysis: $url")
                        return
                    }
                    
                    AppLogger.d("VideoDetectionTabViewModel: Starting page analysis for: $url")
                    viewModelScope.launch(executorRegular) {
                        onStartPage(
                            url.trim(),
                            webTabModel?.userAgent?.get() ?: BrowserFragment.MOBILE_USER_AGENT
                        )
                    }
                } else {
                    AppLogger.d("VideoDetectionTabViewModel: URL doesn't start with http: $url")
                }
            } ?: run {
                AppLogger.d("VideoDetectionTabViewModel: No URL available in tab text input")
            }
        }

        if (detectedVideosList.get()?.isNotEmpty() == true) {
            AppLogger.d("VideoDetectionTabViewModel: Showing detected videos dialog")
            showDetectedVideosEvent.call()
        } else {
            AppLogger.d("VideoDetectionTabViewModel: No detected videos to show")
        }
    }

    override fun verifyLinkStatus(resourceRequest: Request, hlsTitle: String?, isM3u8: Boolean) {
        if (resourceRequest.url.toString().contains("tiktok.")) {
            return
        }

        val urlToVerify = resourceRequest.url.toString()
        if (isM3u8) {
            startVerifyProcess(resourceRequest, true, hlsTitle)
        } else {
            if (urlToVerify.contains(
                    ".txt"
                )
            ) {
                return
            }
            if (settingsModel.getIsFindVideoByUrl().get()) {
                startVerifyProcess(resourceRequest, false)
            }
        }
    }

    open fun startVerifyProcess(
        resourceRequest: Request, isM3u8: Boolean, hlsTitle: String? = null
    ) {
        val taskUrlCleaned = resourceRequest.url.toString().split("?").firstOrNull()?.trim() ?: ""

        val job = verifyVideoLinkJobStorage[taskUrlCleaned]
        if (job != null && !job.isDisposed || taskUrlCleaned.isEmpty()) {
            return
        }

        val loadings = m3u8LoadingList.get()?.toMutableSet()
        loadings?.add(resourceRequest.url.toString())
        m3u8LoadingList.set(loadings?.toMutableSet())
        setButtonState(DownloadButtonStateLoading())

        verifyVideoLinkJobStorage[taskUrlCleaned] =
            io.reactivex.rxjava3.core.Observable.create { emitter ->
                AppLogger.d("VideoDetectionTabViewModel: Starting video info extraction for: $taskUrlCleaned")
                val info = try {
                    videoRepository.getVideoInfo(
                        resourceRequest,
                        isM3u8,
                        settingsModel.isCheckOnAudio.get()
                    )
                } catch (e: Throwable) {
                    AppLogger.e("VideoDetectionTabViewModel: Error extracting video info: ${e.message}")
                    e.printStackTrace()
                    null
                }
                if (info != null) {
                    AppLogger.d("VideoDetectionTabViewModel: Successfully extracted video info: ${info.title}")
                    emitter.onNext(info)
                } else {
                    AppLogger.d("VideoDetectionTabViewModel: No video info found, returning empty")
                    emitter.onNext(VideoInfo(id = ""))
                }
                emitter.onComplete()
            }.doOnTerminate {
                val loadings2 = m3u8LoadingList.get()?.toMutableSet()
                loadings2?.remove(resourceRequest.url.toString())
                m3u8LoadingList.set(loadings2?.toMutableSet())
                verifyVideoLinkJobStorage.remove(taskUrlCleaned)
            }.observeOn(baseSchedulers.computation).subscribeOn(baseSchedulers.videoService)
                .subscribe { info ->
                    if (info.id.isNotEmpty()) {
                        if (info.isM3u8 && !hlsTitle.isNullOrEmpty()) {
                            info.title = hlsTitle
                        }
                        pushNewVideoInfoToAll(info)
                    } else if (info.id.isEmpty()) {
                        setButtonState(DownloadButtonStateCanNotDownload())
                    }
                }
    }

    open fun pushNewVideoInfoToAll(newInfo: VideoInfo) {
        if (newInfo.formats.formats.isEmpty()) {
            return
        }

        if (newInfo.id.isEmpty()) {
            return
        }

        val currentTabUrl = webTabModel?.getTabTextInput()?.get()
        val isTwitch = currentTabUrl?.contains(".twitch.") == true

        if (isTwitch && !newInfo.isMaster) {
            AppLogger.d("SKIP TWICH DUPLICATED VIDEO INFO: $newInfo")
            return
        }

        val detectedVideos = detectedVideosList.get() ?: emptySet()

        if (detectedVideos.any { isVideoInfoDuplicate(it, newInfo) }) {
            AppLogger.d("SKIP DUPLICATED VIDEO INFO: $newInfo")
            return
        }

        AppLogger.d("PUSHING $newInfo to list: \n  $detectedVideos")
        detectedVideosList.set(detectedVideos + newInfo)

        viewModelScope.launch(Dispatchers.Main) {
            videoPushedEvent.call()
        }
        setButtonState(DownloadButtonStateCanDownload(newInfo))
    }

    private fun isVideoInfoDuplicate(existing: VideoInfo, newInfo: VideoInfo): Boolean {
        return if (newInfo.isRegularDownload) {
            existing.firstUrlToString == newInfo.firstUrlToString
        } else {
            existing.formats.formats.any { existingFormat ->
                newInfo.formats.formats.any { newFormat ->
                    existingFormat.url == newFormat.url
                }
            } || existing.originalUrl == newInfo.originalUrl
        }
    }

    override fun getDownloadBtnIcon(): ObservableInt {
        return downloadButtonIcon
    }

    override fun checkRegularVideoOrAudio(
        request: Request?,
        isCheckOnAudio: Boolean,
        isCheckOnVideo: Boolean
    ): Disposable? {
        if (request == null) {
            return null
        }

        val uriString = request.url.toString()

        val isAd = webTabModel?.isAd(uriString) == true
        if (!uriString.startsWith("http") || isAd) {
            return null
        }

        val clearedUrl = uriString.split("?").first().trim()

        if (clearedUrl.contains(filterRegex)) {
            return null
        }

        val headers = try {
            request.headers.toMap().toMutableMap()
        } catch (_: Throwable) {
            mutableMapOf()
        }

        val disposable = io.reactivex.rxjava3.core.Observable.create<Unit> {
            if (request.url.toString().contains(".mp4")) {
                setButtonState(DownloadButtonStateLoading())
            }
            val loadings = regularLoadingList.get()
            loadings?.add(request.url.toString())
            regularLoadingList.set(loadings?.toMutableSet())
            propagateCheckJob(uriString, headers, isCheckOnAudio, isCheckOnVideo)
            it.onComplete()
        }.subscribeOn(baseSchedulers.io).doOnComplete {
            val loadings = regularLoadingList.get()
            loadings?.remove(request.url.toString())
            regularLoadingList.set(loadings?.toMutableSet())
        }.onErrorComplete().doOnError {
            AppLogger.d("Checking ERROR... $clearedUrl")
        }.subscribe()

        return disposable
    }

    override fun cancelAllCheckJobs() {
        regularLoadingList.set(mutableSetOf())
        m3u8LoadingList.set(mutableSetOf())
        executorRegular.cancel()
        verifyVideoLinkJobStorage.forEach { (_, process) ->
            process.dispose()
        }
        verifyVideoLinkJobStorage.clear()
    }

    fun setButtonState(state: DownloadButtonState) {
        // 检查当前网站是否被拦截
        val currentUrl = webTabModel?.getTabTextInput()?.get()
        val isBlocked = currentUrl?.let { com.example.util.BlockedWebsitesManager.isBlockedWebsite(it) } ?: false
        
        when (state) {
            is DownloadButtonStateCanDownload -> {
                // 如果网站被拦截，不设置为可下载状态
                if (!isBlocked) {
                    downloadButtonState.set(state)
                } else {
                    AppLogger.d("VideoDetectionTabViewModel: Blocked website detected, preventing download button activation")
                    downloadButtonState.set(DownloadButtonStateCanNotDownload())
                }
            }

            is DownloadButtonStateCanNotDownload -> {
                val detectedSize = detectedVideosList.get()?.size
                if (detectedSize == null || detectedSize == 0) {
                    val impEl = regularLoadingList.get()?.find { it.contains(".mp4") }
                    if (m3u8LoadingList.get()?.isEmpty() != true || (m3u8LoadingList.get()
                            ?.isEmpty() == true && impEl != null)
                    ) {
                        downloadButtonState.set(DownloadButtonStateLoading())
                    } else {
                        downloadButtonState.set(DownloadButtonStateCanNotDownload())
                    }
                } else {
                    // 如果网站被拦截，即使检测到视频也不设置为可下载状态
                    if (!isBlocked) {
                        downloadButtonState.set(
                            DownloadButtonStateCanDownload(
                                detectedVideosList.get()?.first()
                            )
                        )
                    } else {
                        AppLogger.d("VideoDetectionTabViewModel: Blocked website detected, preventing download button activation despite detected videos")
                        downloadButtonState.set(DownloadButtonStateCanNotDownload())
                    }
                }
            }

            is DownloadButtonStateLoading -> {
                val list = detectedVideosList.get() ?: emptySet()
                if (list.isEmpty()) {
                    downloadButtonState.set(DownloadButtonStateLoading())
                } else {
                    downloadButtonState.set(DownloadButtonStateCanDownload(list.first()))
                }
            }
        }
    }

    private fun getRequestWithHeadersForUrl(
        url: String,
        originalUrl: String,
        userAgent: String,
        alternativeHeaders: Map<String, String> = emptyMap()
    ): Request.Builder? {
        try {
            val cookies = try {
                CookieManager.getInstance().getCookie(url) ?: CookieManager.getInstance()
                    .getCookie(originalUrl) ?: ""
            } catch (_: Throwable) {
                ""
            }
            val stringBuilder = StringBuilder()
            if (cookies.isNotEmpty()) {
                for (cookie in cookies.split(";")) {
                    val parsedCookies = HttpCookie.parse(cookie)

                    for (httpCookie in parsedCookies) {
                        stringBuilder.append("${httpCookie.name}=${httpCookie.value};")
                    }
                }
            }

            if (alternativeHeaders.isEmpty()) {
                val builder = try {
                    Request.Builder().url(url.trim())
                } catch (_: Exception) {
                    null
                }
                builder?.addHeader("Referer", "https://${originalUrl.toUri().host}/")

                builder?.addHeader("User-Agent", userAgent)

                try {
                    if (cookies.isNotEmpty()) {
                        builder?.addHeader("Cookie", stringBuilder.toString())
                    }
                } catch (e: Exception) {
                    AppLogger.d("Url parse error ${e.message}")
                }
                return builder

            } else {
                val builder = try {
                    Request.Builder().url(url.trim())
                } catch (_: Exception) {
                    null
                }
                builder?.headers(alternativeHeaders.toHeaders())
                if (cookies.isNotEmpty() && alternativeHeaders["Cookie"] == null) {
                    builder?.addHeader("Cookie", stringBuilder.toString())
                }

                return builder
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return null
    }

    fun propagateCheckJob(
        url: String,
        headersMap: Map<String, String>,
        isCheckOnAudio: Boolean,
        isCheckOnVideo: Boolean
    ) {
        val threshold = settingsModel.videoDetectionTreshold.get()

        val finalUrlPair = runCatching {
            CookieUtils.getFinalRedirectURL(URL(url.toUri().toString()), headersMap)
        }.getOrNull() ?: return

        val cookies = runCatching {
            CookieManager.getInstance().getCookie(finalUrlPair.first.toString())
                ?: CookieManager.getInstance().getCookie(url) ?: ""
        }.getOrNull() ?: ""

        val headers = headersMap.toMutableMap().apply {
            if (cookies.isNotEmpty()) {
                put("Cookie", cookies)
            }
        }

        runCatching {
            val request =
                Request.Builder().url(finalUrlPair.first).headers(headers.toHeaders()).build()

            okHttpProxyClient.getProxyOkHttpClient().newCall(request).execute().use { response ->
                val contentType = response.body.contentType().toString()
                val contentLength = response.body.contentLength()

                if (response.code == 403 || response.code == 401) {
                    handleUnauthorizedResponse(url, threshold, isCheckOnAudio, isCheckOnVideo)
                    return
                }

                val isTikTok = url.contains(".tiktok.com/")
                when {
                    contentType.contains(
                        "video",
                        true
                    ) && isCheckOnVideo && (contentLength > threshold || (isTikTok && contentLength > 1024 * 1024 / 3)) -> {
                        setMediaInfoWrapperFromUrl(
                            finalUrlPair.first,
                            webTabModel?.getTabTextInput()?.get(),
                            finalUrlPair.second.toMap(),
                            contentLength
                        )
                    }

                    contentType.contains("audio", true) && isCheckOnAudio -> {
                        setMediaInfoWrapperFromUrl(
                            finalUrlPair.first,
                            webTabModel?.getTabTextInput()?.get(),
                            finalUrlPair.second.toMap(),
                            contentLength,
                            true
                        )
                    }
                }
            }
        }.onFailure { e ->
            e.printStackTrace()
        }
    }

    // THIS BULLSHIT NEEDED FOR SOME INDIAN WEB-SITES
    private fun handleUnauthorizedResponse(
        url: String,
        threshold: Int,
        isCheckOnAudio: Boolean,
        isCheckOnVideo: Boolean
    ) {
        val finalUrlPairEmpty = runCatching {
            CookieUtils.getFinalRedirectURL(URL(url.toUri().toString()), emptyMap())
        }.getOrNull() ?: return

        runCatching {
            val request = Request.Builder().url(finalUrlPairEmpty.first).build()
            okHttpProxyClient.getProxyOkHttpClient().newCall(request).execute().use { response ->
                val contentType = response.body.contentType().toString()
                val contentLength = response.body.contentLength()

                when {
                    contentType.contains(
                        "video",
                        true
                    ) && isCheckOnVideo && contentLength > threshold.toLong() -> {
                        setMediaInfoWrapperFromUrl(
                            finalUrlPairEmpty.first,
                            webTabModel?.getTabTextInput()?.get(),
                            finalUrlPairEmpty.second.toMap(),
                            contentLength
                        )
                    }

                    contentType.contains("audio", true) && isCheckOnAudio -> {
                        setMediaInfoWrapperFromUrl(
                            finalUrlPairEmpty.first,
                            webTabModel?.getTabTextInput()?.get(),
                            finalUrlPairEmpty.second.toMap(),
                            contentLength,
                            true
                        )
                    }
                }
            }
        }
    }

    private fun setMediaInfoWrapperFromUrl(
        url: URL,
        originalUrl: String?,
        alternativeHeaders: Map<String, String> = emptyMap(),
        contentLength: Long,
        isAudio: Boolean = false
    ) {
        try {
            if (!url.toString().startsWith("http")) {
                return
            }

            val builder = if (originalUrl != null) {
                Request.Builder().url(url.toString()).headers(alternativeHeaders.toHeaders())
            } else {
                null
            }

            val downloadUrls = listOfNotNull(
                builder?.build()
            )

            val video = VideoInfoWrapper(
                VideoInfo(
                    downloadUrls = downloadUrls,
                    title = webTabModel?.currentTitle?.get() ?: "no_title",
                    ext = if (isAudio) "mp3" else "mp4",
                    originalUrl = webTabModel?.getTabTextInput()?.get() ?: "",
                    // TODO format regular file link
                    formats = VideFormatEntityList(
                        mutableListOf(
                            VideoFormatEntity(
                                formatId = "0",
                                format = if (isAudio) "audio" else ContextUtils.getApplicationContext()
                                    .getString(R.string.player_resolution),
                                ext = if (isAudio) "mp3" else "mp4",
                                url = downloadUrls.first().url.toString(),
                                httpHeaders = downloadUrls.first().headers.toMap(),
                                fileSize = contentLength
                            )
                        )
                    ),
                    isRegularDownload = true
                )
            )
            video.videoInfo?.let { pushNewVideoInfoToAll(it) }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
