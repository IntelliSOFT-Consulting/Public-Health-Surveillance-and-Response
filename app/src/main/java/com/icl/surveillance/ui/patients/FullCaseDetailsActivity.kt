package com.icl.surveillance.ui.patients

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.icl.surveillance.R
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.databinding.ActivityFullCaseDetailsBinding
import com.icl.surveillance.fhir.FhirApplication
import com.icl.surveillance.utils.FormatterClass
import com.icl.surveillance.viewmodels.ClientDetailsViewModel
import com.icl.surveillance.viewmodels.factories.PatientDetailsViewModelFactory

class FullCaseDetailsActivity : AppCompatActivity() {
  private lateinit var binding: ActivityFullCaseDetailsBinding
  private lateinit var fhirEngine: FhirEngine
  private lateinit var patientDetailsViewModel: ClientDetailsViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    binding = ActivityFullCaseDetailsBinding.inflate(layoutInflater)
    setContentView(binding.root)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    val patientId = FormatterClass().getSharedPref("resourceId", this@FullCaseDetailsActivity)

    fhirEngine = FhirApplication.fhirEngine(this@FullCaseDetailsActivity)
    patientDetailsViewModel =
        ViewModelProvider(
                this,
                PatientDetailsViewModelFactory(
                    this@FullCaseDetailsActivity.application, fhirEngine, "$patientId"),
            )
            .get(ClientDetailsViewModel::class.java)
    patientDetailsViewModel.getPatientInfo()
    // getPatientDetailData("Measles Case", null)
    patientDetailsViewModel.livecaseData.observe(this) {
      println("Patient Detail Information ${it.name}")
      binding.apply {
        tvName.text = it.name
        tvSex.text = it.sex
        tvDob.text = it.dob
        tvResidence.text = ""
      }
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressedDispatcher.onBackPressed()
    return true
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_refresh -> {
        FormatterClass().saveSharedPref("questionnaire", "measles-case.json", this)
        FormatterClass().saveSharedPref("title", "Measles Case", this)
        val intent = Intent(this, AddCaseActivity::class.java)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "measles-case.json")
        startActivity(intent)
        true
      }
      R.id.action_settings -> {
        FormatterClass().saveSharedPref("questionnaire", "measles-lab-results.json", this)
        FormatterClass().saveSharedPref("title", "Lab Results", this)
        val intent = Intent(this, AddCaseActivity::class.java)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, "measles-lab-results.json")
        startActivity(intent)
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}
