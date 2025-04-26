package com.icl.surveillance.ui.home

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.icl.surveillance.R
import com.icl.surveillance.adapters.HomeRecyclerViewAdapter
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.clients.AddParentCaseActivity
import com.icl.surveillance.databinding.FragmentHomeBinding
import com.icl.surveillance.utils.FormatterClass

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val viewModel: HomeViewModel by viewModels()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter =
            HomeRecyclerViewAdapter(::onItemClick).apply { submitList(viewModel.getLayoutList()) }
        val recyclerView = requireView().findViewById<RecyclerView>(R.id.sdcLayoutsRecyclerView)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(context, 1)

        handleUser()

    }

    private fun handleUser() {
        val name = getUserNameFromDetails()
        val time = FormatterClass().getTimeOfDay()
        val fullText = "$time, \n\n$name"

//        val spannable = SpannableString(fullText)
//        val start = fullText.indexOf(name)
//        val end = start + name.length
//        spannable.setSpan(
//            StyleSpan(Typeface.ITALIC),
//            start,
//            end,
//            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//        )
//
//        spannable.setSpan(
//            RelativeSizeSpan(0.8f),
//            start,
//            end,
//            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//        )

        binding.apply {
            greetingText.text = time
            usernameText.text = name
        }
    }

    private fun getUserNameFromDetails(): String {
        val user = FormatterClass().getSharedPref("fullNames", requireContext())
        return user ?: "John Mdoe"
    }

    private fun onItemClick(layout: HomeViewModel.Layout) {
        val title = context?.getString(layout.textId) ?: ""
        when (layout.count) {
            5 -> {
                val bundle =
                    Bundle().apply { putString(QUESTIONNAIRE_FILE_PATH_KEY, "add-vl.json") }

                FormatterClass()
                    .saveSharedPref(
                        "stage", "isMonthly",
                        requireContext()
                    )
                FormatterClass()
                    .saveSharedPref(
                        "title", title,
                        requireContext()
                    )

                findNavController().navigate(
                    R.id.action_navigation_home_to_single_case_fragment,
                    bundle
                )
            }

            4 -> {
                FormatterClass().saveSharedPref(
                    "currentCase",
                    "Rumor Case Information",
                    requireContext()
                )
                FormatterClass()
                    .saveSharedPref(
                        "title", "Rumor Tracking Tool",
                        requireContext()
                    )

                FormatterClass().saveSharedPref(
                    "questionnaire",
                    "rumor-tracking-case.json",
                    requireContext()
                )
                val intent = Intent(requireContext(), AddParentCaseActivity::class.java)
                intent.putExtra("title", "Add Rumor Case")
                intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "rumor-tracking-case.json")
                startActivity(intent)

            }

            0 -> {
                val bundle =
                    Bundle().apply { putString(QUESTIONNAIRE_FILE_PATH_KEY, "add-case.json") }

                FormatterClass()
                    .saveSharedPref(
                        "title", title,
                        requireContext()
                    )
                FormatterClass()
                    .saveSharedPref(
                        "stage", "isCurrent",
                        requireContext()
                    )
                findNavController().navigate(
                    R.id.action_navigation_home_to_single_case_fragment,
                    bundle
                )

            }

            else -> {
                Toast.makeText(requireContext(), "Coming soon!!", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

