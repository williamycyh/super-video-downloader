package com.example.util.downloaders.Bubedl_downloader

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.util.AppLogger
import com.example.util.FileUtil
import com.example.util.downloaders.generic_downloader.GenericDownloader
import com.example.util.downloaders.generic_downloader.models.VideoTaskItem
import com.example.util.downloaders.generic_downloader.models.VideoTaskState
import com.example.util.downloaders.generic_downloader.workers.GenericDownloadWorkerWrapper
import com.btdlp.BtdJava
import com.btdlp.BubeDLRequest
import com.btdlp.BubeDLResponse
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class BubeDlDownloaderWorker(appContext: Context, workerParams: WorkerParameters) :
    GenericDownloadWorkerWrapper(appContext, workerParams) {
    companion object {
        var isCanceled = false

        const val IS_FINISHED_DOWNLOAD_ACTION_ERROR_KEY = "IS_FINISHED_DOWNLOAD_ACTION_ERROR_KEY"
        const val STOP_SAVE_ACTION = "STOP_AND_SAVE"
        const val DOWNLOAD_FILENAME_KEY = "download_filename"
        const val IS_FINISHED_DOWNLOAD_ACTION_KEY = "action"
        private const val UPDATE_INTERVAL = 1000
    }

    private lateinit var tmpFile: File
    private var isLiveCounter: Int = 0
    private var isDownloadOk: Boolean = false
    private var isDownloadJustStarted: Boolean = false
    private var monitorProcessDisposable: Disposable? = null
    private var progressCached = 0
    private var downloadJobDisposable: Disposable? = null
    private var cookieFile: File? = null
    private var lastTmpDirSize = 0L
    private var btdJava: BtdJava? = null

    @Volatile
    var time = 0L

    override fun afterDone() {
        monitorProcessDisposable?.dispose()
    }

    override fun handleAction(
        action: String, task: VideoTaskItem, headers: Map<String, String>, isFileRemove: Boolean
    ) {
        when (action) {
            GenericDownloader.DownloaderActions.DOWNLOAD -> {
                isCanceled = false
                startDownload(task, headers)
            }

            GenericDownloader.DownloaderActions.CANCEL -> {
                isCanceled = true
                cancelDownload(task)
            }

            GenericDownloader.DownloaderActions.PAUSE -> {
                isCanceled = false
                pauseDownload(task)
            }

            GenericDownloader.DownloaderActions.RESUME -> {
                isCanceled = false
                resumeDownload(task)
            }

            STOP_SAVE_ACTION -> {
                stopAndSave(task)
            }
        }
    }

    private fun stopAndSave(task: VideoTaskItem) {
        val taskId = inputData.getString(GenericDownloader.Constants.TASK_ID_KEY)

        if (taskId != null) {
            // For the custom library, we don't need to destroy processes
            // Just move the downloaded file if it exists
            
            val partsFolder = fileUtil.tmpDir.resolve(taskId)
            val firstPart = partsFolder.listFiles()?.firstOrNull()

            val dist = File(fileUtil.folderDir.absolutePath, "${task.title}.mp4")

            if (firstPart != null && firstPart.exists()) {
                try {
                    val moved =
                        fileUtil.moveMedia(applicationContext, firstPart.toUri(), dist.toUri())
                    if (moved) {
                        finishWork(task.also { it.taskState = VideoTaskState.SUCCESS })
                    } else {
                        finishWork(task.also { it.taskState = VideoTaskState.ERROR })
                    }
                } catch (e: Throwable) {
                    finishWork(task.also { it.taskState = VideoTaskState.ERROR })
                }
            } else {
                finishWork(task.also { it.taskState = VideoTaskState.ERROR })
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun startDownload(
        task: VideoTaskItem, headers: Map<String, String> = emptyMap(), isContinue: Boolean = false
    ) {
        val taskId = inputData.getString(GenericDownloader.Constants.TASK_ID_KEY)!!

        /** URL FROM INPUT NOT FROM FORMAT BECAUSE FORMAT URL MAY NOT BE APPROPRIATE IN SOME CASES,
        FOR EXAMPLE SOUND SEPARATE FROM FORMAT IN MASTER LIST
         **/
        val url = inputData.getString(
            GenericDownloader.Constants.ORIGIN_KEY
        ) ?: throw Throwable("URL is NULL")

        AppLogger.d("Start download with custom yt-dlp: $url $task")

        val taskTitle = task.title

        hideNotifications(taskId)

        tmpFile = File(
            "${fileUtil.tmpDir}/$taskId"
        )

        if (!tmpFile.exists()) {
            tmpFile.mkdir()
        }

        showProgress(taskId, taskTitle, 0, "Starting...", tmpFile)
        saveProgress(
            taskId,
            line = LineInfo(taskId, 0.0, 0.0, sourceLine = "Starting..."),
            task.also { it.taskState = VideoTaskState.DOWNLOADING }).blockingFirst(Unit)

        downloadJobDisposable?.dispose()

        if (fileUtil.isFreeSpaceAvailable()) {
            startDownloadProcessWithCustomLibrary(url, task, taskId, headers)
        } else {
            finishWork(task.also {
                task.mId = taskId
                task.taskState = VideoTaskState.ERROR
                task.errorMessage = "Not enough space"
            })
        }
    }

    private fun resumeDownload(task: VideoTaskItem) {
        startDownload(task, emptyMap(), true)
    }

    private fun pauseDownload(task: VideoTaskItem) {
        if (getDone()) return

        val id = inputData.getString(GenericDownloader.Constants.TASK_ID_KEY)
        if (id != null) {
            // For custom library, we can't easily pause downloads
            // Just mark as paused for now
            WorkManager.getInstance(applicationContext).cancelAllWorkByTag(id)

            if (task.taskState != VideoTaskState.DOWNLOADING) {
                finishWork(task.also {
                    it.mId = id.toString()
                    it.taskState = VideoTaskState.PAUSE
                })
            }
        }
    }

    private fun cancelDownload(task: VideoTaskItem) {
        val taskId = inputData.getString(GenericDownloader.Constants.TASK_ID_KEY)
        val isFileRemove =
            inputData.getBoolean(GenericDownloader.Constants.IS_FILE_REMOVE_KEY, false)

        if (taskId != null) {
            // For custom library, we can't easily cancel downloads
            // Just clean up files if requested
            
            val fileToRemove = File("${fileUtil.tmpDir}/$taskId")

            if (isFileRemove) {
                fileToRemove.deleteRecursively()
            }

            if (task.taskState != VideoTaskState.DOWNLOADING) {
                finishWork(task.also {
                    it.mId = taskId.toString()
                    it.taskState = VideoTaskState.CANCELED
                })
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun startDownloadProcessWithCustomLibrary(
        url: String,
        task: VideoTaskItem,
        taskId: String,
        headers: Map<String, String> = emptyMap()
    ) {
        downloadJobDisposable = Observable.fromCallable<BubeDLResponse> {
            // Initialize the custom yt-dlp library
            btdJava = BtdJava()
            
            // Clean the title to remove any existing extensions
            val cleanTitle = task.title.replace(Regex("\\.(m3u8|mp4|ts|webm)$"), "")
            val outputPath = "${tmpFile.absolutePath}/${cleanTitle}.mp4"
            
            AppLogger.d("Android下载 - 使用Python兼容方式")
            AppLogger.d("Android下载 - URL: $url")
            AppLogger.d("Android下载 - 输出路径: $outputPath")
            
            // 🆕 使用Python兼容的BubeDLRequest方式
            val request = BubeDLRequest(url)
            
            // 🆕 参考configureBubedlRequest方法设置完整参数
            configureBubedlRequest(request, task, headers, outputPath)
            
            // Add progress callback
            btdJava?.addProgressCallback(object : BtdJava.ProgressCallback {
                override fun onProgress(percentage: Int, bytesDownloaded: Long, totalBytes: Long) {
                    if (Date().time - time > UPDATE_INTERVAL && !getDone()) {
                        time = Date().time
                        
                        progressCached = percentage
                        
                        val downloadBytesFixed = if (bytesDownloaded > 0) {
                            bytesDownloaded
                        } else {
                            (totalBytes * (percentage / 100.0)).toLong()
                        }
                        
                        task.also {
                            it.percent = percentage.toFloat()
                            it.totalSize = totalBytes
                            it.downloadSize = downloadBytesFixed
                            it.taskState = VideoTaskState.DOWNLOADING
                        }

                        val lineInfo = LineInfo("download", downloadBytesFixed.toDouble(), totalBytes.toDouble(), sourceLine = "Downloading...")
                        saveProgress(taskId, lineInfo, task).blockingFirst(Unit)
                        showProgress(taskId, task.title, percentage, "Downloading...", tmpFile)

                        if (!fileUtil.isFreeSpaceAvailable()) {
                            finishWork(task.also {
                                it.mId = taskId
                                it.taskState = VideoTaskState.ERROR
                                it.errorMessage = "Not enough space"
                            })
                        }
                    }
                }
                
                override fun onComplete(filePath: String) {
                    AppLogger.d("Download completed: $filePath")
                }
                
                override fun onError(error: String) {
                    AppLogger.e("Download error: $error")
                }
            })
            
            // 🆕 使用Python兼容的execute方法
            AppLogger.d("Android下载 - 执行Python兼容下载")
            btdJava?.execute(request, taskId, null) ?: throw Exception("Failed to initialize download")
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                AppLogger.d("Android下载结果 - 退出码: ${response.getExitCode()}")
                AppLogger.d("Android下载结果 - 成功: ${response.isSuccess()}")
                AppLogger.d("Android下载结果 - 执行时间: ${response.getElapsedTime()}ms")
                AppLogger.d("Android下载结果 - 输出: ${response.getOut()}")
                
                if (response.isSuccess()) {
                    // 🔧 修复：使用与原始版本相同的文件查找逻辑
                    val outputText = response.getOut()
                    AppLogger.d("Android下载响应输出: $outputText")
                    
                    // 在tmpFile目录中查找下载的文件（与原始版本保持一致）
                    val list = tmpFile.listFiles()
                    val finalFile = if (!list.isNullOrEmpty()) {
                        tmpFile.walkTopDown()
                            .filter {
                                it.isFile && (it.extension.equals("mp4", ignoreCase = true) || 
                                            it.extension.equals("mp3", ignoreCase = true))
                            }
                            .firstOrNull()
                    } else {
                        null
                    }
                    
                    AppLogger.d("Android下载完成 - 找到文件: ${finalFile?.absolutePath}")
                    AppLogger.d("Android下载完成 - 文件大小: ${finalFile?.length()} bytes")
                    
                    if (finalFile != null && finalFile.exists() && finalFile.length() > 0) {
                        val destinationFile = fileUtil.folderDir.resolve(finalFile.name).let {
                            fixFileName(it.absolutePath)
                        }.let {
                            File(it)
                        }
                        
                        AppLogger.d("Android文件移动 - 源文件: ${finalFile.absolutePath}")
                        AppLogger.d("Android文件移动 - 目标文件: ${destinationFile.absolutePath}")
                        
                        // 🔧 修复：使用标准Java文件复制，避免Android特定的moveMedia
                        try {
                            finalFile.copyTo(destinationFile, overwrite = true)
                            
                            // 验证复制后的文件
                            if (destinationFile.exists() && destinationFile.length() > 0) {
                                AppLogger.d("Android文件复制成功 - 目标文件大小: ${destinationFile.length()} bytes")
                                tmpFile.deleteRecursively()
                                finishWork(VideoTaskItem(url).also { f ->
                                    f.fileName = destinationFile.name
                                    f.errorCode = 0
                                    f.percent = 100F
                                    f.taskState = VideoTaskState.SUCCESS
                                })
                            } else {
                                AppLogger.e("Android文件复制失败 - 目标文件无效")
                                finishWork(VideoTaskItem(url).also { f ->
                                    f.errorCode = 1
                                    f.taskState = VideoTaskState.ERROR
                                    f.errorMessage = "文件复制失败"
                                })
                            }
                        } catch (e: Exception) {
                            AppLogger.e("Android文件复制异常: ${e.message}")
                            finishWork(VideoTaskItem(url).also { f ->
                                f.errorCode = 1
                                f.taskState = VideoTaskState.ERROR
                                f.errorMessage = "文件复制异常: ${e.message}"
                            })
                        }
                    } else {
                        // 🔧 修复：与原始版本保持一致的错误处理逻辑
                        val fixedList = tmpFile.listFiles()?.filter { !it.name.contains("part") }
                        AppLogger.e("Android下载失败 - 未找到有效文件，tmpFile文件列表: ${fixedList?.map { it.name }}")
                        
                        fixedList?.firstOrNull()?.let { fallbackFile ->
                            AppLogger.d("Android使用fallback文件: ${fallbackFile.absolutePath}")
                            finishWork(VideoTaskItem(url).also { f ->
                                f.fileName = fallbackFile.name
                                f.errorCode = 0
                                f.percent = 100F
                                f.taskState = VideoTaskState.SUCCESS
                            })
                        } ?: run {
                            AppLogger.e("Android下载完全失败 - 无任何文件")
                            finishWork(VideoTaskItem(url).also { f ->
                                f.errorCode = 1
                                f.taskState = VideoTaskState.ERROR
                                f.errorMessage = "下载失败，未找到任何文件"
                            })
                        }
                    }
                } else {
                    AppLogger.e("Android下载失败 - 错误输出: ${response.getErr()}")
                    finishWork(VideoTaskItem(url).also { f ->
                        f.errorCode = 1
                        f.taskState = VideoTaskState.ERROR
                        f.errorMessage = response.getErr().ifEmpty { "下载失败" }
                    })
                }
            }, { error ->
                handleError(taskId, url, progressCached, error, task.title)
            })
    }

    /**
     * 配置BubeDLRequest参数（参考原版本实现）
     */
    private fun configureBubedlRequest(
        request: BubeDLRequest, 
        task: VideoTaskItem, 
        headers: Map<String, String>,
        outputPath: String
    ) {
        AppLogger.d("配置下载参数...")
        
        // 基本选项
        request.addOption("--progress")
        
        // 线程数配置（模拟原版本的M3U8下载器线程数）
        val threadsCount = 4  // 默认4个线程
        request.addOption("-N", threadsCount)
        
        // 格式选择 - 使用最佳格式
        request.addOption("-f", "best")
        
        // 视频转码选项
        request.addOption("--recode-video", "mp4")
        request.addOption("--merge-output-format", "mp4")
        
        // HLS选项
        request.addOption("--hls-prefer-native")
        request.addOption("--hls-use-mpegts")
        
        // 输出路径
        request.addOption("-o", outputPath)
        
        // 重试和超时
        request.addOption("--retries", "3")
        request.addOption("--timeout", "30")
        request.addOption("--ignore-errors")
        
        // HTTP头部处理
        headers.forEach { (key, value) ->
            if (key != "Cookie") {  // Cookie通过其他方式处理
                request.addOption("--add-header", "$key:$value")
                AppLogger.d("添加HTTP头部: $key: $value")
            }
        }
        
        AppLogger.d("下载参数配置完成")
    }

    @SuppressLint("CheckResult")
    private fun monitorDownloadProcess(taskId: String, task: VideoTaskItem) {
        monitorProcessDisposable =
            Observable.interval(0, 1, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
                .map { FileUtil.calculateFolderSize(tmpFile) }.onErrorReturn { -1 }
                .subscribe { folderSize ->
                    if (folderSize > 0 && folderSize != lastTmpDirSize) {
                        val downloadedTmpFolderSize =
                            FileUtil.getFileSizeReadable(folderSize.toDouble())
                        lastTmpDirSize = folderSize

                        if (progressCached > 0) {
                            isDownloadOk = true
                            monitorProcessDisposable?.dispose()
                            return@subscribe
                        }

                        if (isDownloadJustStarted && !isDownloadOk) {
                            ++isLiveCounter
                            if (isLiveCounter > 2) {
                                isLiveCounter = 3

                                val downloaded = lastTmpDirSize
                                saveProgress(
                                    taskId, LineInfo(
                                        "LIVE",
                                        downloaded.toDouble(),
                                        downloaded.toDouble(),
                                        sourceLine = "Downloading live stream...downloaded: $downloadedTmpFolderSize, press stop and save, to stop downloading and save downloaded at any time...!"
                                    ), task.also { item ->
                                        item.taskState = VideoTaskState.DOWNLOADING
                                        item.lineInfo = downloadedTmpFolderSize
                                        item.downloadSize = downloaded
                                        item.totalSize = downloaded
                                    }).blockingFirst(Unit)
                                showProgress(
                                    taskId,
                                    task.title,
                                    99,
                                    "Downloading Live Stream... $downloadedTmpFolderSize",
                                    tmpFile
                                )
                            }
                        }
                    }
                }
    }

    private fun handleError(
        taskId: String, url: String, progressCached: Int, throwable: Throwable, name: String
    ) {
        AppLogger.d("Download Error: $throwable \ntaskId: $taskId")

        finishWork(VideoTaskItem(url).also { f ->
            if (isCanceled) {
                f.taskState = VideoTaskState.CANCELED
                f.errorCode = 0
            } else {
                f.taskState = VideoTaskState.ERROR
                f.errorCode = 1
                f.errorMessage = throwable.message?.replace(Regex("WARNING:.+\n"), "") ?: ""
            }
            f.fileName = name
            f.percent = progressCached.toFloat()
        })
    }

    //[download]   0.3% of ~  49.94MiB at  438.62KiB/s ETA 04:41 (frag 2/201)
    private fun parseInfoFromLine(line: String?): LineInfo? {
        if (line == null || !line.startsWith("[download]")) {
            return if (line != null) LineInfo("download", 0.0, 0.0, sourceLine = line) else null
        }

        val parts = line.split(Regex(" +"))
        val percent = parts[1].replace("%", "").trim().toDoubleOrNull() ?: return null

        val totalStrIndex = if (line.contains("~")) 4 else 3
        val totalStr = parts[totalStrIndex]

        val unitMatcher = Regex("\\p{L}").find(totalStr) ?: return null
        val totalValue =
            totalStr.substring(0, unitMatcher.range.first).toDoubleOrNull() ?: return null
        val totalUnit = totalStr.substring(unitMatcher.range.first)
        val totalParsed = LineInfo.parse("$totalValue $totalUnit")

        val fragInfo = parts.last().let {
            if (it.contains(")")) {
                val (downloadedFragStr, totalFragStr) = it.split("/")
                val downloadedFrag = downloadedFragStr.replace("(frag ", "").toIntOrNull()
                val totalFrag = totalFragStr.replace(") ", "").toIntOrNull()
                downloadedFrag to totalFrag
            } else {
                null to null
            }
        }

        return LineInfo(
            "download",
            totalParsed * percent / 100,
            totalParsed,
            fragInfo.first,
            fragInfo.second,
            sourceLine = line
        )
    }

    private class LineInfo(
        val id: String,
        val progress: Double,
        val total: Double,
        val fragDownloaded: Int? = null,
        val fragTotal: Int? = null,
        val sourceLine: String
    ) {
        companion object {
            private const val KB_FACTOR: Long = 1000
            private const val KIB_FACTOR: Long = 1024
            private const val MB_FACTOR = 1000 * KB_FACTOR
            private const val MIB_FACTOR = 1024 * KIB_FACTOR
            private const val GB_FACTOR = 1000 * MB_FACTOR
            private const val GIB_FACTOR = 1024 * MIB_FACTOR

            fun parse(arg0: String): Double {
                val spaceNdx = arg0.indexOf(" ")
                val ret = arg0.substring(0, spaceNdx).toDouble()
                when (arg0.substring(spaceNdx + 1)) {
                    "GB" -> return ret * GB_FACTOR
                    "GiB" -> return ret * GIB_FACTOR
                    "MB" -> return ret * MB_FACTOR
                    "MiB" -> return ret * MIB_FACTOR
                    "KB" -> return ret * KB_FACTOR
                    "KiB" -> return ret * KIB_FACTOR
                    "B" -> return ret
                }
                return (-1).toDouble()
            }
        }

        override fun toString(): String {
            return "${FileUtil.getFileSizeReadable(progress)} / ${
                FileUtil.getFileSizeReadable(
                    total
                )
            }  frag: $fragDownloaded / $fragTotal"
        }
    }

    private fun showProgress(
        taskId: String, name: String, progress: Int, line: String, tmpFile: File
    ) {
        val text = line.replace(tmpFile.toString(), "")

        val taskItem = VideoTaskItem("").also {
            it.mId = taskId
            it.fileName = name
            it.taskState = VideoTaskState.DOWNLOADING
            it.percent = progress.toFloat()
            it.lineInfo = text
        }
        val data = notificationsHelper.createNotificationBuilder(taskItem)

        showLongRunningNotificationAsync(data.first, data.second)
    }


    @SuppressLint("CheckResult")
    override fun finishWork(item: VideoTaskItem?) {
        if (getDone()) {
            try {
                getContinuation().resume(Result.success())
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            return
        }

        val taskId = inputData.getString(GenericDownloader.Constants.TASK_ID_KEY)

        if (taskId != null) {
            BubeDlDownloader.deleteHeadersStringFromSharedPreferences(applicationContext, taskId)
        }

        notificationsHelper.hideNotification(taskId.hashCode())
        if (item != null) {
            showNotificationFinal(
                taskId.hashCode(), notificationsHelper.createNotificationBuilder(item.also {
                    it.mId = taskId
                }).second
            )
        }

        downloadJobDisposable?.dispose()
        downloadJobDisposable = null
        cookieFile?.delete()

        if (taskId == null || item == null) {
            try {
                getContinuation().resume(Result.failure())
            } catch (e: Throwable) {
                e.printStackTrace()
            }

            return
        }

        saveProgress(
            taskId, line = LineInfo(taskId, 0.0, 0.0, sourceLine = item.errorMessage ?: ""), item
        ).blockingFirst(Unit)
        setDone()

        try {
            if (item.taskState == VideoTaskState.ERROR) {
                getContinuation().resume(Result.failure())
            } else {
                getContinuation().resume(Result.success())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveProgress(
        taskId: String, line: LineInfo? = null, task: VideoTaskItem
    ): Observable<Unit> {
        if (getDone() && task.taskState == VideoTaskState.DOWNLOADING) {
            AppLogger.d(
                "saveProgress task returned cause DONE!!!"
            )
            return Observable.empty()
        }
        val isBytesNoTouch = line?.total == null || line.total == 0.0
        val iProgressUpdate = task.downloadSize.toInt() > 0

        return progressRepository.getProgressInfos().take(1).toObservable()
            .flatMap { progressList ->
                val dbTask = progressList.find { it.id == taskId }

                if (!isBytesNoTouch) {
                    dbTask?.progressTotal = (line?.total ?: task.totalSize).toLong()
                }

                if (task.taskState != VideoTaskState.SUCCESS) {
                    if (!isBytesNoTouch && iProgressUpdate) {
                        dbTask?.progressDownloaded = task.downloadSize
                    }
                } else {
                    dbTask?.progressDownloaded = dbTask?.progressTotal ?: -1
                }

                dbTask?.fragmentsTotal = line?.fragTotal ?: 1
                dbTask?.fragmentsDownloaded = line?.fragDownloaded ?: 0
                dbTask?.downloadStatus = task.taskState

                dbTask?.infoLine = line?.sourceLine ?: ""

                if (line?.id == "LIVE" && dbTask?.isLive != true) {
                    dbTask?.isLive = true
                }

                if (dbTask != null) {
                    if (getDone() && task.taskState == VideoTaskState.DOWNLOADING) {
                        AppLogger.d(
                            "saveProgress task returned cause DONE!!!"
                        )
                    } else {
                        progressRepository.saveProgressInfo(dbTask)
                    }
                }
                Observable.empty()
            }
    }

    // deserializeVideoFormat method removed - no longer needed with custom library

    private fun hideNotifications(taskId: String) {
        notificationsHelper.hideNotification(taskId.hashCode())
        notificationsHelper.hideNotification(taskId.hashCode() + 1)
    }
    
}
