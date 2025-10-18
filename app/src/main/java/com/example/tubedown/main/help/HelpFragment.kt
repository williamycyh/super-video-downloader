package com.example.tubedown.main.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.lifecycle.ViewModelProvider
import com.example.R
import com.example.databinding.FragmentHelpBinding
import com.example.tubedown.main.base.BaseFragment
import com.example.tubedown.main.home.MainActivity
import com.google.android.material.tabs.TabLayoutMediator
import javax.inject.Inject

class HelpFragment : BaseFragment() {

    private lateinit var dataBinding: FragmentHelpBinding
    private lateinit var helpViewModel: HelpViewModel

    @Inject
    lateinit var mainActivity: MainActivity

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    companion object {
        @JvmStatic
        fun newInstance() = HelpFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        helpViewModel = ViewModelProvider(this, viewModelFactory)[HelpViewModel::class.java]

        dataBinding = FragmentHelpBinding.inflate(inflater, container, false).apply {
            viewModel = helpViewModel
            
            // Setup toolbar
            toolbar.setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
            
                // Setup OK button
                helpOkButton.setOnClickListener {
                    parentFragmentManager.popBackStack()
                }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            parentFragmentManager.popBackStack()
        }

        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        helpViewModel.start()
        setupViewPager()
    }

    private fun setupViewPager() {
        val adapter = HelpPagerAdapter(requireActivity())
        dataBinding.helpViewpager.adapter = adapter
        
            // 设置页面指示器
            TabLayoutMediator(dataBinding.helpTabLayout, dataBinding.helpViewpager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.help_tab_detection)
                    1 -> getString(R.string.help_tab_download)
                    2 -> getString(R.string.help_tab_disclaimer)
                    3 -> getString(R.string.help_tab_tips)
                    4 -> getString(R.string.help_tab_troubleshooting)
                    else -> ""
                }
            }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        helpViewModel.stop()
    }
}