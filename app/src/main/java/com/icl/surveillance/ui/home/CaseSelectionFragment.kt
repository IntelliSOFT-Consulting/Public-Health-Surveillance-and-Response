package com.icl.surveillance.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.icl.surveillance.R
import com.icl.surveillance.adapters.CaseOptionsAdapter
import com.icl.surveillance.adapters.DiseasesRecyclerViewAdapter
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.clients.AddParentCaseActivity
import com.icl.surveillance.databinding.FragmentCaseSelectionBinding
import com.icl.surveillance.databinding.FragmentSingleCaseBinding
import com.icl.surveillance.models.CaseOption
import com.icl.surveillance.utils.FormatterClass
import kotlin.getValue

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CaseSelectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CaseSelectionFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private var _binding: FragmentCaseSelectionBinding? = null
    private val binding
        get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCaseSelectionBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val titleName = FormatterClass().getSharedPref("title", requireContext())

        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        // Let the Fragment receive menu callbacks
        setHasOptionsMenu(true)

        binding.apply {
            greeting.text = titleName
        }
//        val adapter =
//            DiseasesRecyclerViewAdapter(::onItemClick).apply { submitList(viewModel.getDiseasesList()) }
//        val recyclerView = requireView().findViewById<RecyclerView>(R.id.sdcLayoutsRecyclerView)
//        recyclerView.adapter = adapter
//        recyclerView.layoutManager = GridLayoutManager(context, 1)
        val caseOptions = listOf(
            CaseOption("Add new $titleName case"),
            CaseOption("$titleName Cases List", showCount = true, count = 38)
        )

        val recyclerView = requireView().findViewById<RecyclerView>(R.id.sdcLayoutsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = CaseOptionsAdapter(caseOptions) { option ->
            when (option.title) {
                "Add new AFP case" -> {
                    FormatterClass().saveSharedPref(
                        "title",
                        "Add $titleName Case",
                        requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "questionnaire",
                        "afp-case.json",
                        requireContext()
                    )
                    val intent = Intent(requireContext(), AddParentCaseActivity::class.java)
                    intent.putExtra("title", "Add $titleName Case")
                    intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "afp-case.json")
                    startActivity(intent)
                }

                "Add new Measles case" -> {
                    FormatterClass().saveSharedPref(
                        "title",
                        "Add $titleName Case",
                        requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "questionnaire",
                        "add-case.json",
                        requireContext()
                    )
                    val intent = Intent(requireContext(), AddParentCaseActivity::class.java)
                    intent.putExtra("title", "Add $titleName Case")
                    intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "add-case.json")
                    startActivity(intent)
                }

                "Measles Cases List" -> {
                    Toast.makeText(requireContext(), "Coming Soon", Toast.LENGTH_SHORT).show()
                }

                else -> {
                    Toast.makeText(requireContext(), "Coming Soon", Toast.LENGTH_SHORT).show()
                }
            }


//            Toast.makeText(requireContext(), "Clicked: ${option.title}", Toast.LENGTH_SHORT).show()

            // let's check clicks with when

//            when (option) {
//                'Add new measles case' -> {}
//                'Measles Cases List' -> {}
//
//            }


        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle the back button in the toolbar
                requireActivity().onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CaseSelectionFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CaseSelectionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}