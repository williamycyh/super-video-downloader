package com.example.tubedown.component.binding

import android.widget.GridView
import androidx.databinding.BindingAdapter
import com.example.data.local.room.entity.PageInfo
import com.example.tubedown.component.adapter.*

object GridViewBinding {
    @BindingAdapter("app:items")
    @JvmStatic
    fun GridView.setTopPages(items: List<PageInfo>) {
        with(adapter as TopPageAdapter?) {
            this?.let { setData(items) }
        }
    }
}