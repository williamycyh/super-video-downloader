package com.example.tubedown.component.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.data.local.room.entity.VideoFormatEntity
import com.example.data.local.room.entity.VideoInfo
import com.example.databinding.DownloadCandidateItemBinding
import com.example.util.FileUtil
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors


interface DownloadVideoListener {
    fun onPreviewVideo(
        videoInfo: VideoInfo,
        dialog: BottomSheetDialog?,
        format: String,
        isForce: Boolean
    )

    fun onDownloadVideo(
        videoInfo: VideoInfo,
        dialog: BottomSheetDialog?,
        format: String,
        videoTitle: String
    )
}

interface DownloadTabVideoListener {
    fun onPreviewVideo(
        videoInfo: VideoInfo,
        format: String,
        isForce: Boolean
    )

    fun onDownloadVideo(
        videoInfo: VideoInfo,
        format: String,
        videoTitle: String
    )
}

interface DownloadDialogListener : DownloadVideoListener, CandidateFormatListener {
    fun onCancel(dialog: BottomSheetDialog?)
}

interface DownloadTabListener : DownloadTabVideoListener, CandidateFormatListener {
    fun onCancel()
}

interface CandidateFormatListener {
    fun onSelectFormat(videoInfo: VideoInfo, format: String)

    fun onFormatUrlShare(videoInfo: VideoInfo, format: String): Boolean
}

class CandidatesListRecyclerViewAdapter(
    private val downloadCandidates: VideoInfo,
    private val selectedFormat: ObservableField<Map<String, String>>,
    private val downloadDialogListener: CandidateFormatListener
) : RecyclerView.Adapter<CandidatesListRecyclerViewAdapter.CandidatesViewHolder>() {

    private var formats: List<VideoFormatEntity> = arrayListOf()

    init {
        val allFormats = downloadCandidates.formats.formats
        formats = getShortenFormats(allFormats)
    }

    class CandidatesViewHolder(val binding: DownloadCandidateItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidatesViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DownloadCandidateItemBinding.inflate(inflater, parent, false)
        return CandidatesViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: CandidatesViewHolder, position: Int) {
        with(holder.binding) {
            val candidate = formats[position].format ?: "error"

            listener = object : CandidateFormatListener {
                override fun onSelectFormat(videoInfo: VideoInfo, format: String) {
                    downloadDialogListener.onSelectFormat(videoInfo, format)
                    notifyDataSetChanged()
                }

                override fun onFormatUrlShare(videoInfo: VideoInfo, format: String): Boolean {
                    return downloadDialogListener.onFormatUrlShare(videoInfo, format)
                }
            }
            val selected = selectedFormat.get()?.get(downloadCandidates.id)

            val color = MaterialColors.getColor(
                this.root.context,
                R.attr.colorSurfaceVariant,
                Color.YELLOW
            )
            this.cardItem.setCardBackgroundColor(color)

            this.videoInfo = downloadCandidates
            this.downloadCandidate = candidate
            this.isCandidateSelected = candidate == selected
            this.tvTitle.text = getShortOfFormat(candidate)
            val frmt = formats[position]
            val formatSize = if (frmt.fileSizeApproximate > 0) {
                FileUtil.getFileSizeReadable(frmt.fileSizeApproximate.toDouble())
            } else if (frmt.fileSize > 0) {
                FileUtil.getFileSizeReadable(frmt.fileSize.toDouble())
            } else {
                "Unknown"
            }
            val fileSizeLine = "File size: $formatSize"
            this.tvData.text =
                "vcodec: ${frmt.vcodec ?: "unknown"} \nacodec: ${frmt.acodec ?: "unknown"} \n $fileSizeLine \n${frmt.formatNote ?: ""}"

            this.executePendingBindings()
        }
    }

    override fun getItemCount(): Int = formats.size

    fun setData(formats: List<VideoFormatEntity>) {
        this.formats = formats
        notifyDataSetChanged()
    }

    private fun makeVideoFormatHumanReadable(input: String): String {
        return input.replace(Regex("-\\w+"), "")
    }

    private fun getShortenFormats(allFormats: List<VideoFormatEntity>): List<VideoFormatEntity> {
        val formatsMap = mutableMapOf<String, VideoFormatEntity>()
        for (format in allFormats) {
            formatsMap[format.format.toString()] = format
        }

        formatsMap.remove("")

        formatsMap.toSortedMap()

        return formatsMap.toSortedMap().values.toList().sortedBy { it.formatNote }
    }

    private fun getShortOfFormat(format: String?): String {
        val formattedFormat = makeVideoFormatHumanReadable(format ?: "error")
        if (formattedFormat != "error") {
            return if (formattedFormat.contains("x")) {
                "${parseHeight(formattedFormat)}P"
            } else if (!formattedFormat.contains("x") && !formattedFormat.contains("audio only")
                && formattedFormat.contains("-")
            ) {
                val leftSide = formattedFormat.split("-").first()
                if (leftSide.lowercase().contains("hd") || leftSide.contains("sd")) {
                    return leftSide.trim()
                }
                val rightSide = formattedFormat.split("-").last()
                rightSide.replace("p", "P").trim()
            } else if (formattedFormat.contains("audio only")) {
                ""
            } else {
                formattedFormat
            }
        }

        return "Error"
    }

    private fun parseHeight(input: String): Int? {
        val regex = Regex("""\d+x(\d+)""")
        val matchResult = regex.find(input)

        return if (matchResult != null) {
            matchResult.groupValues[1].toIntOrNull()
        } else {
            null
        }
    }
}
