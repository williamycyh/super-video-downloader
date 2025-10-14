package com.example.util.downloaders.tubedl_downloader

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
import com.example.util.downloaders.tubedl_downloader.TubeDlDownloaderWorker.Companion.STOP_SAVE_ACTION
import com.google.gson.Gson
import java.util.concurrent.TimeUnit


object TubeDlDownloader : GenericDownloader() {

    fun stopAndSaveDownload(context: Context, progressInfo: ProgressInfo) {
        val downloadWork = getWorkRequest(progressInfo.videoInfo.id)
        val downloaderData = getDownloadDataFromVideoInfo(progressInfo.videoInfo)
        downloaderData.putString(Constants.ACTION_KEY, STOP_SAVE_ACTION)
        downloadWork.setInputData(downloaderData.build())

        runWorkerTask(
            context, progressInfo.videoInfo, downloadWork.build()
        )
    }

    override fun getDownloadDataFromVideoInfo(
        videoInfo: VideoInfo
    ): Data.Builder {
        val videoUrl: String = if (videoInfo.downloadUrls.isNotEmpty()) {
            videoInfo.originalUrl
        } else {
            videoInfo.formats.formats.firstOrNull()?.url.toString()
        }

        val data = Data.Builder()

        data.putString(Constants.URL_KEY, videoUrl)
        data.putString(Constants.TITLE_KEY, videoInfo.title)
        data.putString(Constants.FILENAME_KEY, videoInfo.name)

        data.putString(Constants.ORIGIN_KEY, videoInfo.originalUrl)
        data.putString(Constants.TASK_ID_KEY, videoInfo.id)

        if (videoInfo.formats.formats.firstOrNull() != null && videoInfo.formats.formats.isNotEmpty()) {
            val stringifiedFormatEntity =
                Gson().toJson(videoInfo.formats.formats.firstOrNull()).toString()
            val encodedEntity =
                Base64.encodeToString(stringifiedFormatEntity.toByteArray(), Base64.DEFAULT)

            val compressedEntity = compressString(encodedEntity)
            AppLogger.d("superZip ${compressedEntity.toByteArray().size}  ---- ${encodedEntity.toByteArray().size}")

            saveStringToSharedPreferences(
                ContextUtils.getApplicationContext(), videoInfo.id, compressedEntity
            )
        }

        return data
    }

    override fun getWorkRequest(id: String): OneTimeWorkRequest.Builder {
        return OneTimeWorkRequest.Builder(TubeDlDownloaderWorker::class.java).addTag(id)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
    }
}

