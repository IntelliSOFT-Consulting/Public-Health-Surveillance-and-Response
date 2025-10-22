package com.icl.nphi.monitor

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngineProvider
import com.icl.nphi.R
import com.icl.nphi.databinding.FragmentResourceListBinding
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ResourceListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ResourceListFragment : Fragment() {

    private lateinit var binding: FragmentResourceListBinding
    private lateinit var viewModel: PaginatedViewModel
    private lateinit var adapter: ResourceAdapter


    // onCreateView is where you inflate the layout
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using ViewBinding
        binding = FragmentResourceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fhirEngine = FhirEngineProvider.getInstance(requireContext())
        viewModel = PaginatedViewModel(fhirEngine)

        setupRecyclerView()
        setupObservers()
        setupUI()

        // Load first page
        viewModel.loadFirstPage("Patient")
    }

    private fun setupRecyclerView() {

        adapter = ResourceAdapter(onUploadClick = { resourceId ->
            viewModel.uploadSingleResource(resourceId)
        }, onRetryClick = { resourceId ->
            viewModel.retryUpload(resourceId)
        })
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Add scroll listener for pagination
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                // Load more when we're near the end
                if (!viewModel.isLoading.value &&
                    viewModel.hasMore.value &&
                    (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                ) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.resources.collect { resources ->
                adapter.submitList(resources)
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.hasMore.collect { hasMore ->
                // Show/hide load more button or indicator
                if (!hasMore && viewModel.resources.value.isNotEmpty()) {
                    showNoMoreItems()
                }
            }
        }
    }

    private fun setupUI() {
        val resourceTypes =
            arrayOf("Patient", "QuestionnaireResponse", "MeasureReport", "Observation", "Encounter")

        // Create custom adapter with forced black text
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.spinner_item,
            resourceTypes
        ) {
            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent)
                // Force black text for dropdown items
                (view as TextView).setTextColor(ContextCompat.getColor(context, R.color.black))
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinner.adapter = adapter

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedType = resourceTypes[position]
                viewModel.loadFirstPage(selectedType)

                // Force black text on selected item
                (parent.getChildAt(0) as? TextView)?.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.black)
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Set initial selection
        binding.spinner.setSelection(0)

        // Force initial text color
        binding.spinner.post {
            (binding.spinner.selectedView as? TextView)?.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.black)
            )
        }
    }

    private fun setupUIOld() {
        // Resource type selector
        val resourceTypes =
            arrayOf("Patient", "QuestionnaireResponse", "MeasureReport", "Observation", "Encounter")
        binding.spinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, resourceTypes)

        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedType = resourceTypes[position]
                viewModel.changeResourceType(selectedType)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Manual load more button (optional)
        binding.loadMoreButton.setOnClickListener {
            viewModel.loadNextPage()
        }

        // Refresh button
        binding.refreshButton.setOnClickListener {
            viewModel.loadFirstPage(binding.spinner.selectedItem as String)
        }
    }

    private fun showNoMoreItems() {
        Toast.makeText(requireContext(), "All items loaded", Toast.LENGTH_SHORT).show()
    }
}