package com.icl.nphi.ui.patients

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.icl.nphi.adapters.PatientDetailsRecyclerViewAdapter
import com.icl.nphi.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.nphi.databinding.ActivityClientDetailsBinding
import com.icl.nphi.fhir.FhirApplication
import com.icl.nphi.utils.FormatterClass
import com.icl.nphi.viewmodels.ClientDetailsViewModel
import com.icl.nphi.viewmodels.factories.PatientDetailsViewModelFactory

class ClientDetailsActivity : AppCompatActivity() {
  private lateinit var binding:
      ActivityClientDetailsBinding // Binding class name is based on layout file name
  private lateinit var fhirEngine: FhirEngine
  private lateinit var patientDetailsViewModel: ClientDetailsViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    binding = ActivityClientDetailsBinding.inflate(layoutInflater)
    setContentView(binding.root)

    // Access and set click listener on the FAB
    binding.fab.setOnClickListener {
      FormatterClass().saveSharedPref("questionnaire", "measles-case.json", this)
      val intent = Intent(this@ClientDetailsActivity, AddCaseActivity::class.java)
      intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "measles-case.json")
      startActivity(intent)
    }

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    val patientId = FormatterClass().getSharedPref("resourceId", this@ClientDetailsActivity)

    fhirEngine = FhirApplication.fhirEngine(this@ClientDetailsActivity)
    patientDetailsViewModel =
        ViewModelProvider(
                this,
                PatientDetailsViewModelFactory(
                    this@ClientDetailsActivity.application, fhirEngine, "$patientId"),
            )
            .get(ClientDetailsViewModel::class.java)
    val adapter = PatientDetailsRecyclerViewAdapter(this::onItemClicked)
    binding.patientList.adapter = adapter

//    patientDetailsViewModel.livePatientData.observe(this) { adapter.submitList(it) }
    patientDetailsViewModel.getPatientDetailData("Measles Case",null)
  }

  private fun onItemClicked(encounterItem: PatientListViewModel.EncounterItem) {
//    FormatterClass().saveSharedPref("encounterId", encounterItem.id, this@ClientDetailsActivity)
    val intent = Intent(this@ClientDetailsActivity, CaseDetailsActivity::class.java)
    startActivity(intent)
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressedDispatcher.onBackPressed()
    return true
  }
}
