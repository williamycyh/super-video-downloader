package com.example.tubedown.main.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.databinding.FragmentHelpDisclaimerBinding

class HelpDisclaimerFragment : Fragment() {

    private lateinit var binding: FragmentHelpDisclaimerBinding

    companion object {
        fun newInstance() = HelpDisclaimerFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHelpDisclaimerBinding.inflate(inflater, container, false)
        return binding.root
    }
}
