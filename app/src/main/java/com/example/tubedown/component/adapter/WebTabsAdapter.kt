package com.example.tubedown.component.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.databinding.ItemWebTabButtonBinding
import com.example.tubedown.main.home.browser.webTab.WebTab

interface WebTabsListener {
    fun onCloseTabClicked(webTab: WebTab)
    fun onSelectTabClicked(webTab: WebTab)
}

class WebTabsAdapter(
    private var webTabs: List<WebTab>,
    private var webTabsListener: WebTabsListener
) : RecyclerView.Adapter<WebTabsAdapter.WebTabsViewHolder>() {

    class WebTabsViewHolder(val binding: ItemWebTabButtonBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(webTab: WebTab, webTabsListener: WebTabsListener) {
            with(binding)
            {
                val context = this.root.context

                this.webTab = webTab
                this.tabListener = webTabsListener

                this.closeTab.visibility = if (webTab.isHome()) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
                if (webTab.getFavicon() == null && !webTab.isHome()) {
                    val bm =
                        AppCompatResources.getDrawable(
                            context,
                            R.drawable.public_24px
                        )

                    this.faviconTab.setImageDrawable(bm)
                }

                if (webTab.isHome()) {
                    val bm =
                        AppCompatResources.getDrawable(
                            context,
                            R.drawable.home_48px
                        )

                    this.faviconTab.setImageDrawable(bm)
                }

                if (!webTab.isHome()) {
                    if (webTab.getTitle().isEmpty()) {
                        this.tabTitle.text = webTab.getUrl()
                    } else {
                        this.tabTitle.text = webTab.getTitle()
                    }
                }

                executePendingBindings()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebTabsViewHolder {
        val binding = DataBindingUtil.inflate<ItemWebTabButtonBinding>(
            LayoutInflater.from(parent.context),
            R.layout.item_web_tab_button, parent, false
        )

        return WebTabsViewHolder(binding)
    }

    override fun getItemCount() = webTabs.size

    override fun onBindViewHolder(holder: WebTabsViewHolder, position: Int) =
        holder.bind(webTabs[position], webTabsListener)

    fun setData(webTabs: List<WebTab>) {
        this.webTabs = webTabs
        notifyDataSetChanged()
    }
}
