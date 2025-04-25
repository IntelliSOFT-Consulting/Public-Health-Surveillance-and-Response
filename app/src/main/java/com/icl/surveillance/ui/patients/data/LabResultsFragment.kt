package com.icl.surveillance.ui.patients.data

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.icl.surveillance.R
import com.icl.surveillance.adapters.LabRecyclerViewAdapter
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.databinding.FragmentLabInformationBinding
import com.icl.surveillance.databinding.FragmentLabResultsBinding
import com.icl.surveillance.fhir.FhirApplication
import com.icl.surveillance.ui.patients.AddCaseActivity
import com.icl.surveillance.ui.patients.PatientListViewModel
import com.icl.surveillance.utils.FormatterClass
import com.icl.surveillance.utils.toSlug
import com.icl.surveillance.viewmodels.ClientDetailsViewModel
import com.icl.surveillance.viewmodels.factories.PatientDetailsViewModelFactory

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass. Use the [LabResultsFragment.newInstance] factory method to create
 * an instance of this fragment.
 */
class LabResultsFragment : Fragment() {
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


    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientDetailsViewModel: ClientDetailsViewModel
    private var _binding: FragmentLabResultsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLabResultsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }

    override fun onResume() {
        super.onResume()
        try {
            val encounterId = FormatterClass().getSharedPref("encounterId", requireContext())
            patientDetailsViewModel.getPatientDiseaseData(
                "Measles Lab Information", "$encounterId", false
            )
        } catch (e: Exception) {
            println(e.message)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val patientId = FormatterClass().getSharedPref("resourceId", requireContext())
        val encounterId = FormatterClass().getSharedPref("encounterId", requireContext())
        val currentCase = FormatterClass().getSharedPref("currentCase", requireContext())

        println("Current Parent Encounter $encounterId")
        fhirEngine = FhirApplication.fhirEngine(requireContext())
        patientDetailsViewModel =
            ViewModelProvider(
                this,
                PatientDetailsViewModelFactory(
                    requireActivity().application, fhirEngine, "$patientId"
                ),
            )
                .get(ClientDetailsViewModel::class.java)

        val adapter = LabRecyclerViewAdapter(this::onItemClicked)
        binding.patientList.adapter = adapter

        patientDetailsViewModel.liveLabData.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                binding.tvNoCase.visibility = View.VISIBLE
            } else {
                it.forEach { k ->
                    println("Lab Information Here **** ${k.dateSpecimenReceived}")
                }
                binding.tvNoCase.visibility = View.GONE
//        binding.fab.visibility = View.GONE
                adapter.submitList(it)
            }
        }
        if (currentCase != null) {
            val slug = currentCase.toSlug()
            when (slug) {
                "measles-case-information" -> {
                    patientDetailsViewModel.getPatientDiseaseData(
                        "Measles Lab Information",
                        "$encounterId",
                        false
                    )
                }

                "afp-case-information" -> {
                    patientDetailsViewModel.getPatientDiseaseData(
                        "AFP Lab Information",
                        "$encounterId",
                        false
                    )
                }
            }
        }

        binding.apply {
            fab.setOnClickListener {
                if (currentCase != null) {
                    val slug = currentCase.toSlug()
                    when (slug) {
                        "measles-case-information" -> {
                            FormatterClass()
                                .saveSharedPref(
                                    "questionnaire",
                                    "measles-lab-results.json",
                                    requireContext()
                                )
                            FormatterClass().saveSharedPref(
                                "title",
                                "Measles Lab Results",
                                requireContext()
                            )
                            val intent = Intent(requireContext(), AddCaseActivity::class.java)
                            intent.putExtra(
                                QUESTIONNAIRE_FILE_PATH_KEY,
                                "measles-lab-results.json"
                            )
                            startActivity(intent)
                        }

                        "afp-case-information" -> {
                            FormatterClass()
                                .saveSharedPref(
                                    "questionnaire",
                                    "afp-case-lab-results.json",
                                    requireContext()
                                )
                            FormatterClass().saveSharedPref(
                                "title",
                                "AFP Lab Results",
                                requireContext()
                            )
                            val intent = Intent(requireContext(), AddCaseActivity::class.java)
                            intent.putExtra(
                                QUESTIONNAIRE_FILE_PATH_KEY,
                                "measles-lab-results.json"
                            )
                            startActivity(intent)
                        }

                    }
                }


            }
        }
    }

    private fun onItemClicked(encounterItem: PatientListViewModel.CaseLabResultsData) {}

    companion object {
        /**
         * Use this factory method to create a new instance of this fragment using the provided
         * parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LabResultsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LabResultsFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
            }
    }
}
