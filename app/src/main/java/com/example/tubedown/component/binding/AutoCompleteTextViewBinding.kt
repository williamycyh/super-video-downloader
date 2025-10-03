package com.example.tubedown.component.binding

import android.widget.AutoCompleteTextView
import androidx.databinding.BindingAdapter
import com.example.data.local.model.Suggestion
import com.example.data.local.room.entity.HistoryItem
import com.example.tubedown.component.adapter.SuggestionAdapter
import com.example.tubedown.component.adapter.TabSuggestionAdapter

object AutoCompleteTextViewBinding {

    @BindingAdapter("app:items")
    @JvmStatic
    fun AutoCompleteTextView.setSuggestions(items: List<Suggestion>?) {
        with(adapter as SuggestionAdapter?) {
            if (items != null) {
                this?.setData(items)
            } else {
                this?.setData(emptyList())
            }
        }
    }

    @BindingAdapter("app:items")
    @JvmStatic
    fun AutoCompleteTextView.setTabSuggestions(items: List<HistoryItem>?) {
        with(adapter as TabSuggestionAdapter?) {
            if (items != null) {
                this?.setData(items)
            } else {
                this?.setData(emptyList())
            }
        }
    }
}