package com.icl.surveillance.ui.patients.data

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.icl.surveillance.adapters.PatientDetailsRecyclerViewAdapter
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.databinding.FragmentClinicalInformationBinding
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
 * A simple [Fragment] subclass. Use the [ClinicalInformationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ClinicalInformationFragment : Fragment() {
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
  private var _binding: FragmentClinicalInformationBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding
    get() = _binding!!

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {

    _binding = FragmentClinicalInformationBinding.inflate(inflater, container, false)
    val root: View = binding.root

    return root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val patientId = FormatterClass().getSharedPref("resourceId", requireContext())
    val encounterId = FormatterClass().getSharedPref("encounterId", requireContext())

    fhirEngine = FhirApplication.fhirEngine(requireContext())
    patientDetailsViewModel =
        ViewModelProvider(
                this,
                PatientDetailsViewModelFactory(
                    requireActivity().application, fhirEngine, "$patientId"),
            )
            .get(ClientDetailsViewModel::class.java)

    val adapter = PatientDetailsRecyclerViewAdapter(this::onItemClicked)
    val currentCase = FormatterClass().getSharedPref("currentCase", requireContext())
    if (currentCase != null) {
      val slug = currentCase.toSlug()
      patientDetailsViewModel.getPatientInfo(slug)
    }
    // getPatientDetailData("Measles Case", null)
    patientDetailsViewModel.livecaseData.observe(viewLifecycleOwner) {
      binding.apply {
        tvFirstSeen.text = it.dateFirstSeen
        tvStatus.text = it.inPatientOutPatient
        tvAdmissionDate.text = it.admissionDate
        tvIpOpNo.text = it.ipNo
        tvOutcome.text = it.patientOutcome
        tvSampleCollected.text = it.sampleCollected
        
        //        tvVaccinated.text = it.wasPatientVaccinated
        //        tvTwoMonths.text = it.twoMonthsVaccination
        //        tvStatus.text = it.patientStatus
      }
    }
    binding.apply {
      fab.setOnClickListener {
        FormatterClass().saveSharedPref("questionnaire", "measles-case.json", requireContext())
        FormatterClass().saveSharedPref("title", "Measles Case", requireContext())
        val intent = Intent(requireContext(), AddCaseActivity::class.java)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "measles-case.json")
        startActivity(intent)
      }
    }
  }

  private fun onItemClicked(encounterItem: PatientListViewModel.EncounterItem) {}

  companion object {
    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ClinicalInformationFragment.
     */
    // TODO: Rename and change types and number of parameters
    @JvmStatic
    fun newInstance(param1: String, param2: String) =
        ClinicalInformationFragment().apply {
          arguments =
              Bundle().apply {
                putString(ARG_PARAM1, param1)
                putString(ARG_PARAM2, param2)
              }
        }
  }
}
