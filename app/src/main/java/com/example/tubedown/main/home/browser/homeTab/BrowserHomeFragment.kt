package com.example.tubedown.main.home.browser.homeTab

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.model.Suggestion
import com.example.data.local.room.entity.PageInfo
import com.example.databinding.FragmentBrowserHomeBinding
import com.example.tubedown.component.adapter.SuggestionAdapter
import com.example.tubedown.component.adapter.SuggestionListener
import com.example.tubedown.component.adapter.TopPageAdapter
import com.example.tubedown.main.home.MainViewModel
import com.example.tubedown.main.home.browser.BaseWebTabFragment
import com.example.tubedown.main.home.browser.BrowserListener
import com.example.tubedown.main.home.browser.TabManagerProvider
import com.example.tubedown.main.home.browser.webTab.WebTabFactory
import com.example.tubedown.rereads.MyCommon
import com.example.util.AppUtil
import kotlinx.coroutines.launch
import javax.inject.Inject

interface BrowserHomeListener : BrowserListener {

    override fun onBrowserReloadClicked() {
    }

    override fun onTabCloseClicked() {
    }

    override fun onBrowserStopClicked() {
    }

    override fun onBrowserBackClicked() {
    }

    override fun onBrowserForwardClicked() {
    }
    
    override fun onTabsCounterClicked() {
    }
    
    override fun onSettingsClicked() {
    }
}

class BrowserHomeFragment : BaseWebTabFragment() {

    companion object {
        fun newInstance() = BrowserHomeFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var appUtil: AppUtil

    lateinit var binding: FragmentBrowserHomeBinding

    private lateinit var openPageIProvider: TabManagerProvider

    private lateinit var homeViewModel: BrowserHomeViewModel

    private lateinit var mainViewModel: MainViewModel

    private lateinit var topPageAdapter: TopPageAdapter

    private lateinit var suggestionAdapter: SuggestionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainViewModel = mainActivity.mainViewModel
        homeViewModel = ViewModelProvider(this, viewModelFactory)[BrowserHomeViewModel::class.java]
        openPageIProvider = mainActivity.mainViewModel.browserServicesProvider!!

        topPageAdapter = TopPageAdapter(requireContext(), emptyList(), itemListener)
        suggestionAdapter = SuggestionAdapter(requireContext(), emptyList(), suggestionListener)

        binding = FragmentBrowserHomeBinding.inflate(inflater, container, false).apply {
            buildWebTabMenu(this.browserHomeMenuButton, true)

            this.viewModel = homeViewModel
            this.mainVModel = mainViewModel
            this.browserMenuListener = menuListener
            this.topPagesGrid.adapter = topPageAdapter

            this.homeEtSearch.setAdapter(suggestionAdapter)
            this.homeEtSearch.addTextChangedListener(onInputHomeSearchChangeListener)
            this.homeEtSearch.imeOptions = EditorInfo.IME_ACTION_DONE
            this.homeEtSearch.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    this.homeEtSearch.clearFocus()
                    viewModel?.viewModelScope?.launch {
                        val inputText = (this@apply.homeEtSearch as EditText).text.toString()
                        this@apply.homeEtSearch.text.clear()
                        openNewTab(inputText)
                    }
                    false
                } else false
            }
            
            // Setup button click listeners
            this.btnPaste.setOnClickListener {
                pasteFromClipboard()
            }
            
            this.btnSearch.setOnClickListener {
                performSearch()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleFirstStartGuide()

        homeViewModel.start()
        val openingUrl = mainViewModel.openedUrl.get()
        val openingText = mainViewModel.openedText.get()

        if (openingUrl != null) {
            openNewTab(openingUrl)
            mainViewModel.openedUrl.set(null)
        }

        if (openingText != null) {
            openNewTab(openingText)
            mainViewModel.openedText.set(null)
        }

        // Setup tabs counter
        setupTabsCounter()

        loadAd()
    }

    // Bug fix for not updating home page grid after adding new bookmark
    override fun onResume() {
        super.onResume()
        val bookmarksList = mainViewModel.bookmarksList.get()?.toMutableList()
        mainViewModel.bookmarksList.set(bookmarksList)
    }

    private val suggestionListener = object : SuggestionListener {
        override fun onItemClicked(suggestion: Suggestion) {
            openNewTab(suggestion.content)
        }
    }

    private fun openNewTab(input: String) {
        if (input.isNotEmpty()) {
            openPageIProvider.getOpenTabEvent().value = WebTabFactory.createWebTabFromInput(input)
        }
    }

    private val onInputHomeSearchChangeListener = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            val input = s.toString()
            homeViewModel.searchTextInput.set(input)
            if (!(input.startsWith("http://") || input.startsWith("https://"))) {
                homeViewModel.showSuggestions()
            }
            homeViewModel.homePublishSubject.onNext(input)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
    }

    private val itemListener = object : TopPageAdapter.TopPagesListener {
        override fun onItemClicked(pageInfo: PageInfo) {
            openNewTab(pageInfo.link)
        }
    }

    private val menuListener = object : BrowserHomeListener {
        override fun onBrowserMenuClicked() {
            showPopupMenu()
        }
        
        override fun onTabsCounterClicked() {
            // Open the navigation drawer to show tabs
            mainViewModel.openNavDrawerEvent.call()
        }
        
        override fun onSettingsClicked() {
            navigateToSettings()
        }
    }

    private fun handleFirstStartGuide() {
        if (mainActivity.sharedPrefHelper.getIsFirstStart()) {
            mainActivity.settingsViewModel.setIsFirstStart(false)
            navigateToHelp()
        }
    }

    override fun shareWebLink() {}

    override fun bookmarkCurrentUrl() {}
    
    private fun pasteFromClipboard() {
        try {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                val text = item.text?.toString()
                
                if (!text.isNullOrEmpty()) {
                    binding.homeEtSearch.setText(text)
                    binding.homeEtSearch.setSelection(text.length) // Move cursor to end
                } else {
                    Toast.makeText(requireContext(), "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to paste from clipboard", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun performSearch() {
        val inputText = binding.homeEtSearch.text.toString()
        if (inputText.isNotEmpty()) {
            binding.homeEtSearch.clearFocus()
            homeViewModel.viewModelScope.launch {
                binding.homeEtSearch.text.clear()
                openNewTab(inputText)
            }
        } else {
            Toast.makeText(requireContext(), "Please enter search text", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupTabsCounter() {
        // Initial update
        updateTabsCounter()
        
        // Listen for tabs changes
        openPageIProvider.getTabsListChangeEvent().addOnPropertyChangedCallback(object :
            androidx.databinding.Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: androidx.databinding.Observable?, propertyId: Int) {
                updateTabsCounter()
            }
        })
    }
    
    private fun updateTabsCounter() {
        val tabsList = openPageIProvider.getTabsListChangeEvent().get()
        val tabsCount = tabsList?.size ?: 1 // At least 1 tab (home tab)
        binding.tabsCounterText.text = tabsCount.toString()
    }


    private fun loadAd() {
//        AdCenter.Companion.initInmobi(this);

//        MyCommon.loadFullScreen(this);
        binding.nativeAdFrame.post {
            if (!MyCommon.showFullScreen(activity)) {
                MyCommon.loadFullScreenAndShow(activity)
            }

            val myCommon = MyCommon()
            myCommon.loadBigNative(activity, binding.nativeAdFrame);
        }
    }
}
