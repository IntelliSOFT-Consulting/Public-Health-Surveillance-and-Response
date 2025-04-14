package com.icl.surveillance.ui.patients

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.icl.surveillance.R
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FRAGMENT_TAG
import com.icl.surveillance.databinding.ActivityAddCaseBinding
import com.icl.surveillance.utils.FormatterClass
import com.icl.surveillance.viewmodels.ScreenerViewModel
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse

class AddCaseActivity : AppCompatActivity() {

  private val viewModel: ScreenerViewModel by viewModels()
  private lateinit var binding:
      ActivityAddCaseBinding // Binding class name is based on layout file name

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    binding = ActivityAddCaseBinding.inflate(layoutInflater)
    setContentView(binding.root)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    updateArguments()
    if (savedInstanceState == null) {
      addQuestionnaireFragment()
    }
    observePatientSaveAction()
    supportFragmentManager.setFragmentResultListener(
        QuestionnaireFragment.SUBMIT_REQUEST_KEY,
        this@AddCaseActivity,
    ) { _, _ ->
      onSubmitAction()
    }
    supportFragmentManager.setFragmentResultListener(
        QuestionnaireFragment.CANCEL_REQUEST_KEY,
        this@AddCaseActivity,
    ) { _, _ ->
      onBackPressed()
    }
  }

  private fun onSubmitAction() {
    lifecycleScope.launch {
      val questionnaireFragment =
          supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG)
              as QuestionnaireFragment

      val questionnaireResponse = questionnaireFragment.getQuestionnaireResponse()
      // Print the response to the log
      val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
      val questionnaireResponseString = jsonParser.encodeResourceToString(questionnaireResponse)
      Log.e("response", questionnaireResponseString)
      println("Response $questionnaireResponseString")
      saveCase(questionnaireFragment.getQuestionnaireResponse())
    }
  }

  private fun saveCase(questionnaireResponse: QuestionnaireResponse) {
    val patientId = FormatterClass().getSharedPref("resourceId", this@AddCaseActivity)
    val questionnaire = FormatterClass().getSharedPref("questionnaire", this@AddCaseActivity)
    val encounter = FormatterClass().getSharedPref("encounterId", this@AddCaseActivity)
    when (questionnaire) {
      "measles.json" -> viewModel.completeAssessment(questionnaireResponse, "$patientId")
      "measles-lab.json" ->
          viewModel.completeLabAssessment(questionnaireResponse, "$patientId", "$encounter")
      "covid.json" -> viewModel.completeAssessment(questionnaireResponse, "$patientId")
    }
  }

  private fun addQuestionnaireFragment() {
    supportFragmentManager.commit {
      add(
          R.id.add_patient_container,
          QuestionnaireFragment.builder()
              .setQuestionnaire(viewModel.questionnaire)
              .setShowCancelButton(true)
              .setSubmitButtonText("Submit")
              .build(),
          QUESTIONNAIRE_FRAGMENT_TAG,
      )
    }
  }

  private fun observePatientSaveAction() {
    viewModel.isResourcesSaved.observe(this@AddCaseActivity) {
      if (!it) {
        Toast.makeText(
                this@AddCaseActivity, "Please Enter all Required Fields.", Toast.LENGTH_SHORT)
            .show()
        return@observe
      }
      Toast.makeText(this@AddCaseActivity, "Record saved successfully.", Toast.LENGTH_SHORT).show()
      onBackPressed()
    }
  }

  private fun updateArguments() {
    val json = FormatterClass().getSharedPref("questionnaire", this@AddCaseActivity)
    intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, json)
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressedDispatcher.onBackPressed()
    return true
  }
}
