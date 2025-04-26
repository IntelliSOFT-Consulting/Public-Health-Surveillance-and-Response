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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import com.icl.surveillance.R
import com.icl.surveillance.adapters.CaseOptionsAdapter
import com.icl.surveillance.cases.CaseListingActivity
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.clients.AddParentCaseActivity
import com.icl.surveillance.databinding.FragmentCaseSelectionBinding
import com.icl.surveillance.fhir.FhirApplication
import com.icl.surveillance.models.CaseOption
import com.icl.surveillance.ui.patients.PatientListViewModel
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

    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientListViewModel: PatientListViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCaseSelectionBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onResume() {
        super.onResume()
        val titleName = FormatterClass().getSharedPref("title", requireContext())
//
//        try {
//            if (titleName != null) {
//                val title = when (titleName) {
//                    "Visceral Leishmaniasis (Kala-azar) Case Management Form" -> "VL"
//                    else -> titleName
//                }
//
//                setupRecyclerView()
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }


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

        fhirEngine = FhirApplication.fhirEngine(requireContext())
        patientListViewModel =
            ViewModelProvider(
                this,
                PatientListViewModel.PatientListViewModelFactory(
                    requireActivity().application, fhirEngine
                ),
            )
                .get(PatientListViewModel::class.java)

        binding.apply {
            greeting.text = titleName
        }
        if (titleName != null) {
            setupRecyclerView()
        }

    }

    private fun setupRecyclerView() {
        val titleName = FormatterClass().getSharedPref("title", requireContext())

        val title = when (titleName) {
            "Visceral Leishmaniasis (Kala-azar) Case Management Form" -> "VL"
            else -> titleName
        }
        val caseOptions = mutableListOf(
            CaseOption("Add New $title Case"),
            CaseOption(
                "$title Case List",
                showCount = true,
                count = 0
            )
        )


        val recyclerView = requireView().findViewById<RecyclerView>(R.id.sdcLayoutsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = CaseOptionsAdapter(caseOptions) { option ->
            when (option.title) {
                "Add New VL Case" -> {
                    FormatterClass().saveSharedPref(
                        "currentCase",
                        "VL Case Information",
                        requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "title",
                        "Visceral Leishmaniasis (Kala-azar) Case Management Form",
                        requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "questionnaire",
                        "vl-case.json",
                        requireContext()
                    )
                    val intent = Intent(requireContext(), AddParentCaseActivity::class.java)
                    intent.putExtra("title", " $titleName")
                    intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "vl-case.json")
                    startActivity(intent)
                }

                "VL Case List" -> {
                    FormatterClass().saveSharedPref(
                        "title",
                        " ${option.title}",
                        requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "currentCase",
                        "VL Case Information",
                        requireContext()
                    )
                    val intent = Intent(requireContext(), CaseListingActivity::class.java)
                    startActivity(intent)
                }

                "Add New AFP Case" -> {

                    FormatterClass().saveSharedPref(
                        "currentCase",
                        "AFP Case Information",
                        requireContext()
                    )
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

                "Add New Measles Case" -> {

                    FormatterClass().saveSharedPref(
                        "currentCase",
                        "Measles Case Information",
                        requireContext()
                    )
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

                "Measles Case List" -> {
                    FormatterClass().saveSharedPref(
                        "title",
                        " ${option.title}",
                        requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "currentCase",
                        "Measles Case Information",
                        requireContext()
                    )
                    val intent = Intent(requireContext(), CaseListingActivity::class.java)
                    startActivity(intent)
                }

                "AFP Case List" -> {
                    FormatterClass().saveSharedPref(
                        "title",
                        " ${option.title}",
                        requireContext()
                    )
                    FormatterClass().saveSharedPref(
                        "currentCase",
                        "AFP Case Information",
                        requireContext()
                    )
                    val intent = Intent(requireContext(), CaseListingActivity::class.java)
                    startActivity(intent)
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

        val caseType = when (title?.trim()) {
            "Measles" -> "measles-case-information"
            "AFP" -> "afp-case-information"
            "VL" -> "vl-case-information"
            else -> null
        }

        caseType?.let {
            try {
                patientListViewModel.handleCurrentCaseListing(it)

                patientListViewModel.liveSearchedCases.observe(viewLifecycleOwner) { cases ->
                    caseOptions[1] = caseOptions[1].copy(count = cases.size)
                    recyclerView.adapter?.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
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