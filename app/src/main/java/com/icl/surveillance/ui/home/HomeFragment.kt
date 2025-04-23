package com.icl.surveillance.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.icl.surveillance.R
import com.icl.surveillance.adapters.HomeRecyclerViewAdapter
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
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
        recyclerView.layoutManager = GridLayoutManager(context, 2)

    }

    private fun onItemClick(layout: HomeViewModel.Layout) {
        val title = context?.getString(layout.textId) ?: ""
//        when (layout.count) {
//            0 -> {
        val bundle =
            Bundle().apply { putString(QUESTIONNAIRE_FILE_PATH_KEY, "add-client.json") }

        FormatterClass()
            .saveSharedPref(
                "title", title,
                requireContext()
            )

        findNavController().navigate(
            R.id.action_navigation_home_to_single_case_fragment,
            bundle
        )

//            }
//        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
