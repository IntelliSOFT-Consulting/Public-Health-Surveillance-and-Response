package com.icl.surveillance.clients

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import cn.pedant.SweetAlert.SweetAlertDialog
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.icl.surveillance.R
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FRAGMENT_TAG
import com.icl.surveillance.databinding.ActivityAddParentCaseBinding
import com.icl.surveillance.utils.FormatterClass
import com.icl.surveillance.utils.ProgressDialogManager
import com.icl.surveillance.viewmodels.AddClientViewModel
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse

class AddParentCaseActivity : AppCompatActivity() {

    private val viewModel: AddClientViewModel by viewModels()
    private lateinit var binding:
            ActivityAddParentCaseBinding // Binding class name is based on layout file name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddParentCaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
//    setSupportActionBar(binding.toolbar)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    val titleName = FormatterClass().getSharedPref("title", this@AddParentCaseActivity)
    supportActionBar.apply { title = titleName }


        updateArguments()
        if (savedInstanceState == null) {
            addQuestionnaireFragment()
        }
        observePatientSaveAction()

        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.SUBMIT_REQUEST_KEY,
            this@AddParentCaseActivity,
        ) { _, _ ->
            onSubmitAction()
        }
        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.CANCEL_REQUEST_KEY,
            this@AddParentCaseActivity,
        ) { _, _ ->
            onBackPressed()
        }
    }

    private fun onSubmitAction() {
        ProgressDialogManager.show(this, "Please Wait.....")
        lifecycleScope.launch {
            val questionnaireFragment =
                supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG)
                        as QuestionnaireFragment

            val questionnaireResponse = questionnaireFragment.getQuestionnaireResponse()
            // Print the response to the log
            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val questionnaireResponseString =
                jsonParser.encodeResourceToString(questionnaireResponse)
            Log.e("response", questionnaireResponseString)
            println("Response $questionnaireResponseString")
            saveCase(questionnaireFragment.getQuestionnaireResponse())
        }
    }


    private fun showCancelScreenerQuestionnaireAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage(getString(R.string.cancel_questionnaire_message))
            setPositiveButton(getString(android.R.string.yes)) { _, _ ->
                this@AddParentCaseActivity.finish()
            }
            setNegativeButton(getString(android.R.string.no)) { _, _ -> }
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun saveCase(questionnaireResponse: QuestionnaireResponse) {
        val context = FhirContext.forR4()
        val questionnaireResponseString =
            context.newJsonParser().encodeResourceToString(questionnaireResponse)
        println("Questionnaire Response: $questionnaireResponseString")
        viewModel.savePatientData(questionnaireResponse, questionnaireResponseString)
    }

    private fun addQuestionnaireFragment() {
        supportFragmentManager.commit {
            add(
                R.id.add_patient_container,
                QuestionnaireFragment.builder()
                    .setQuestionnaire(viewModel.questionnaireJson)
                    .setShowCancelButton(true)
                    .showReviewPageBeforeSubmit(true)
                    .setSubmitButtonText("Submit")
                    .build(),
                QUESTIONNAIRE_FRAGMENT_TAG,
            )
        }
    }

    private fun observePatientSaveAction() {
        viewModel.isPatientSaved.observe(this) {
            ProgressDialogManager.dismiss()

            if (!it) {
                Toast.makeText(this, "Please Enter all Required Fields.", Toast.LENGTH_SHORT).show()
                return@observe
            }
            val alert = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            alert
                .setTitleText("Success!")
                .setContentText("You have successfully created a case!")
                .setConfirmText("OK")
                .setConfirmClickListener { dialog ->
                    dialog.dismissWithAnimation()
                    this@AddParentCaseActivity.finish()
                }
                .setCancelable(false)
            alert.show()
        }
    }

    private fun updateArguments() {
        val json = FormatterClass().getSharedPref("questionnaire", this@AddParentCaseActivity)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, json)
    }

    override fun onSupportNavigateUp(): Boolean {
        showCancelScreenerQuestionnaireAlertDialog()
        return true
    }
}
