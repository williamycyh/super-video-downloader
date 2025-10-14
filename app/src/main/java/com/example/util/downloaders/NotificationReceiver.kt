package com.example.util.downloaders

import android.content.Context
import android.content.Intent
import com.example.data.local.room.entity.ProgressInfo
import com.example.data.local.room.entity.VideoInfo
import com.example.data.repository.ProgressRepository
import com.example.util.AppLogger
import com.example.util.downloaders.custom_downloader.CustomRegularDownloader
import com.example.util.downloaders.tubedl_downloader.TubeDlDownloader
import dagger.android.DaggerBroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

class NotificationReceiver : DaggerBroadcastReceiver() {
    @Inject
    lateinit var progressRepository: ProgressRepository

    private val receiverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val taskId = intent.extras?.getString(TASK_ID)
        receiverScope.launch {
            val progressInfo = progressRepository.getProgressInfos().blockingFirst()
                .firstOrNull { it.id == taskId }

            AppLogger.d("-----------------------------------   $taskId  $progressInfo")

            if (progressInfo == null) {
                return@launch
            }

            when (intent.action) {
                ACTION_PAUSE -> {
                    handlePause(context, progressInfo)
                }

                ACTION_RESUME -> {
                    handleResume(context, progressInfo)
                }

                ACTION_CANCEL -> {
                    handleCancel(context, progressInfo)
                }

                else -> {
                    AppLogger.d("ACTION NOT SUPPORTED ${intent.action}")
                }
            }
        }
    }

    private fun handleCancel(context: Context, task: ProgressInfo) {
        AppLogger.d("HANDLE CANCEL $task")
        when (val downloaderType = getTaskType(task.videoInfo)) {
            DOWNLOADER_YOUTUBE_DL -> {
                TubeDlDownloader.cancelDownload(context, task, true)
                progressRepository.deleteProgressInfo(task)
            }

            DOWNLOADER_REGULAR -> {
                CustomRegularDownloader.cancelDownload(context, task, true)
                progressRepository.deleteProgressInfo(task)
            }

            else -> {
                AppLogger.d("Unexpected downloader type: $downloaderType")
            }
        }
    }

    private fun handleResume(context: Context, task: ProgressInfo) {
        AppLogger.d("HANDLE RESUME $task")
        when (val downloaderType = getTaskType(task.videoInfo)) {
            DOWNLOADER_YOUTUBE_DL -> {
                TubeDlDownloader.resumeDownload(context, task)
            }

            DOWNLOADER_REGULAR -> {
                CustomRegularDownloader.resumeDownload(context, task)
            }

            else -> {
                AppLogger.d("Unexpected downloader type: $downloaderType")
            }
        }
    }

    private fun handlePause(context: Context, task: ProgressInfo) {
        AppLogger.d("HANDLE PAUSE $task")
        when (val downloaderType = getTaskType(task.videoInfo)) {
            DOWNLOADER_YOUTUBE_DL -> {
                TubeDlDownloader.pauseDownload(context, task)
            }

            DOWNLOADER_REGULAR -> {
                CustomRegularDownloader.pauseDownload(context, task)
            }

            else -> {
                AppLogger.d("Unexpected downloader type: $downloaderType")
            }
        }
    }

    private fun getTaskType(videoInfo: VideoInfo): String {
        return if (videoInfo.isRegularDownload) {
            DOWNLOADER_REGULAR
        } else {
            DOWNLOADER_YOUTUBE_DL
        }
    }

    companion object {
        const val DOWNLOADER_YOUTUBE_DL = "DOWNLOADER_YOUTUBE_DL"
        const val DOWNLOADER_REGULAR = "DOWNLOADER_REGULAR"
        const val TASK_ID = "TASK_ID"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_CANCEL = "ACTION_CANCEL"
    }
}
