package com.example.util.downloaders.custom_downloader

import android.content.Context
import android.util.Base64
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import com.example.data.local.room.entity.ProgressInfo
import com.example.data.local.room.entity.VideoInfo
import com.example.util.AppLogger
import com.example.util.ContextUtils
import com.example.util.downloaders.generic_downloader.GenericDownloader
import com.example.util.downloaders.Bubedl_downloader.BubeDlDownloader
import com.example.util.downloaders.Bubedl_downloader.BubeDlDownloaderWorker.Companion.STOP_SAVE_ACTION
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CustomRegularDownloader : GenericDownloader() {

    fun stopAndSaveDownload(context: Context, progressInfo: ProgressInfo) {
        val downloadWork = BubeDlDownloader.getWorkRequest(progressInfo.videoInfo.id)
        val downloaderData =
            BubeDlDownloader.getDownloadDataFromVideoInfo(progressInfo.videoInfo)
        downloaderData.putString(Constants.ACTION_KEY, STOP_SAVE_ACTION)
        downloadWork.setInputData(downloaderData.build())

        runWorkerTask(
            context, progressInfo.videoInfo, downloadWork.build()
        )
    }

    override fun getDownloadDataFromVideoInfo(videoInfo: VideoInfo): Data.Builder {
        val videoUrl = videoInfo.firstUrlToString
        val headers = videoInfo.downloadUrls.firstOrNull()?.headers
        val headersMap = mutableMapOf<String, String>()

        for (name in headers?.names() ?: emptySet()) {
            headersMap[name] = headers?.get(name) ?: ""
        }

        var fileName = videoInfo.name

        val cookie = headersMap["Cookie"]
        if (cookie != null) {
            headersMap["Cookie"] =
                Base64.encodeToString(cookie.toString().toByteArray(), Base64.DEFAULT)
        }

        val headersForClean = (headersMap as Map<*, *>?)?.let { JSONObject(it).toString() }
        val headersVal = try {
            Base64.encodeToString(headersForClean?.toByteArray(), Base64.DEFAULT)
        } catch (_: Exception) {
            "{}"
        }

        val data = Data.Builder()
        data.putString(Constants.URL_KEY, videoUrl)
        data.putString(Constants.TASK_ID_KEY, videoInfo.id)

        val zipHeaders = compressString(headersVal)
        AppLogger.d("superZip ${zipHeaders.toByteArray().size}  ---- ${headersVal.toByteArray().size}")

        saveStringToSharedPreferences(
            ContextUtils.getApplicationContext(), videoInfo.id, zipHeaders
        )

        data.putString(Constants.TITLE_KEY, videoInfo.title)
        data.putString(Constants.FILENAME_KEY, fileName)

        return data
    }

    override fun getWorkRequest(id: String): OneTimeWorkRequest.Builder {
        return OneTimeWorkRequest.Builder(CustomRegularDownloaderWorker::class.java)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS).addTag(id)
    }
}

