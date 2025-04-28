package com.icl.surveillance.ui.patients

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.material.button.MaterialButton
import com.icl.surveillance.R
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FRAGMENT_TAG
import com.icl.surveillance.databinding.ActivityAddCaseBinding
import com.icl.surveillance.utils.FormatterClass
import com.icl.surveillance.utils.ProgressDialogManager
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
        val titleName = FormatterClass().getSharedPref("title", this@AddCaseActivity)
        supportActionBar.apply { title = titleName }
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
        ProgressDialogManager.show(this, "Please wait.....")
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

    private fun saveCase(questionnaireResponse: QuestionnaireResponse) {

        val patientId = FormatterClass().getSharedPref("resourceId", this@AddCaseActivity)
        val questionnaire = FormatterClass().getSharedPref("questionnaire", this@AddCaseActivity)
        val encounter = FormatterClass().getSharedPref("encounterId", this@AddCaseActivity)
        val context = FhirContext.forR4()
        val questionnaireResponseString =
            context.newJsonParser().encodeResourceToString(questionnaireResponse)

        println("Parent Encounter $encounter")
        when (questionnaire) {
            "measles-case.json" ->
                viewModel.completeAssessment(questionnaireResponse, "$patientId", "$encounter")

            "measles-lab-results.json" ->
                viewModel.completeLabAssessment(
                    questionnaireResponse,
                    "$patientId",
                    "$encounter",
                    "Measles Lab Information",
                    questionnaireResponseString
                )

            "measles-lab-reg-results.json" ->
                viewModel.completeLabAssessment(
                    questionnaireResponse,
                    "$patientId",
                    "$encounter",
                    "Measles Regional Lab Information",
                            questionnaireResponseString
                )

            "afp-case-stool-lab-results.json" ->
                viewModel.completeLabAssessment(
                    questionnaireResponse,
                    "$patientId",
                    "$encounter",
                    "AFP Stool Lab Information",
                    questionnaireResponseString
                )
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
            ProgressDialogManager.dismiss()
            if (!it) {
                Toast.makeText(
                    this@AddCaseActivity, "Please Enter all Required Fields.", Toast.LENGTH_SHORT
                )
                    .show()
                return@observe
            }

            showSuccessDialog(this@AddCaseActivity)
//            val alert = SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
//            alert
//                .setTitleText("Success!")
//                .setContentText("Record successfully saved!")
//                .setConfirmText("OK")
//                .setConfirmClickListener { dialog ->
//                    dialog.dismissWithAnimation()
//                    this@AddCaseActivity.finish()
//                }
//                .setCancelable(false)
//            alert.show()
        }
    }

    fun showSuccessDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.success_dialog, null)
        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            alertDialog.dismiss()
            this@AddCaseActivity.finish()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_finish).setOnClickListener {
            // handle finish action
            this@AddCaseActivity.finish()
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    private fun updateArguments() {
        val json = FormatterClass().getSharedPref("questionnaire", this@AddCaseActivity)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, json)
    }

    override fun onBackPressed() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                super.onBackPressed() // Exit the activity
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog
            }
            .create()

        dialog.show()
    }

}
