package com.example.util.downloaders.youtubedl_downloader

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.model.Proxy
import com.example.data.local.room.entity.VideoFormatEntity
import com.example.util.AppLogger
import com.example.util.CookieUtils
import com.example.util.FileUtil
import com.example.util.downloaders.generic_downloader.GenericDownloader
import com.example.util.downloaders.generic_downloader.models.VideoTaskItem
import com.example.util.downloaders.generic_downloader.models.VideoTaskState
import com.example.util.downloaders.generic_downloader.workers.GenericDownloadWorkerWrapper
import com.google.gson.Gson
import com.ytdlp.YtDlpJava
import com.ytdlp.core.VideoInfo
import com.ytdlp.core.VideoFormat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class YoutubeDlDownloaderWorker(appContext: Context, workerParams: WorkerParameters) :
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
    private var ytdlpJava: YtDlpJava? = null

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
                startDownload(task)
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
        task: VideoTaskItem, isContinue: Boolean = false
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
            startDownloadProcessWithCustomLibrary(url, task, taskId)
        } else {
            finishWork(task.also {
                task.mId = taskId
                task.taskState = VideoTaskState.ERROR
                task.errorMessage = "Not enough space"
            })
        }
    }

    private fun resumeDownload(task: VideoTaskItem) {
        startDownload(task, true)
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
    ) {
        downloadJobDisposable = Observable.fromCallable<YtDlpJava.DownloadResult> {
            // Initialize the custom yt-dlp library
            ytdlpJava = YtDlpJava()
            
            // Set output path to tmp directory
            val outputPath = "${tmpFile.absolutePath}/${task.title}.%(ext)s"
            ytdlpJava?.setOption("output", outputPath)
            
            // Add progress callback
            ytdlpJava?.addProgressCallback(object : YtDlpJava.ProgressCallback {
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
            
            // Start download and return result
            ytdlpJava?.download(url, outputPath) ?: throw Exception("Failed to initialize download")
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ downloadResult ->
                if (downloadResult.isSuccess()) {
                    val finalFile = File(downloadResult.getFilePath())
                    if (finalFile.exists()) {
                        val destinationFile = fileUtil.folderDir.resolve(finalFile.name).let {
                            fixFileName(it.absolutePath)
                        }.let {
                            File(it)
                        }
                        val moved = fileUtil.moveMedia(
                            this@YoutubeDlDownloaderWorker.applicationContext,
                            Uri.fromFile(finalFile),
                            Uri.fromFile(destinationFile)
                        )

                        if (moved) {
                            tmpFile.deleteRecursively()
                        }
                        finishWork(VideoTaskItem(url).also { f ->
                            f.fileName = finalFile.name
                            f.errorCode = if (moved) 0 else 1
                            f.percent = 100F
                            f.taskState = if (moved) VideoTaskState.SUCCESS else VideoTaskState.ERROR
                        })
                    } else {
                        finishWork(VideoTaskItem(url).also { f ->
                            f.errorCode = 1
                            f.taskState = VideoTaskState.ERROR
                            f.errorMessage = "Downloaded file not found"
                        })
                    }
                } else {
                    finishWork(VideoTaskItem(url).also { f ->
                        f.errorCode = 1
                        f.taskState = VideoTaskState.ERROR
                        f.errorMessage = downloadResult.getErrorMessage() ?: "Unknown error"
                    })
                }
            }, { error ->
                handleError(taskId, url, progressCached, error, task.title)
            })
    }

    // configureYoutubedlRequest method removed - no longer needed with custom library

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
            YoutubeDlDownloader.deleteHeadersStringFromSharedPreferences(applicationContext, taskId)
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
