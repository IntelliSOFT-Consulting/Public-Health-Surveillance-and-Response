package com.icl.surveillance.clients

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.material.button.MaterialButton
import com.icl.surveillance.R
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FRAGMENT_TAG
import com.icl.surveillance.databinding.ActivityAddParentCaseBinding
import com.icl.surveillance.utils.ContribQuestionnaireItemViewHolderFactoryMatchersProviderFactory
import com.icl.surveillance.utils.FormatterClass
import com.icl.surveillance.utils.LocationUtils
import com.icl.surveillance.utils.ProgressDialogManager
import com.icl.surveillance.viewmodels.AddClientViewModel
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

class AddParentCaseActivity : AppCompatActivity() {
    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private val viewModel: AddClientViewModel by viewModels()
    private lateinit var binding:
            ActivityAddParentCaseBinding // Binding class name is based on layout file name

    private fun getStringFromAssets(fileName: String): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddParentCaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val titleName = FormatterClass().getSharedPref("AddParentTitle", this@AddParentCaseActivity)
        supportActionBar.apply { title = titleName }

//        checkAndRequestLocationPermission()
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

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun checkAndRequestLocationPermission() {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED ||
            coarseLocationPermission != PackageManager.PERMISSION_GRANTED
        ) {
            AlertDialog.Builder(this@AddParentCaseActivity)
                .setTitle("Location Permission Needed")
                .setMessage("We need your location to provide better services. Please allow location access.")
                .setPositiveButton("Allow") { dialog, _ ->
                    dialog.dismiss()
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@AddParentCaseActivity,
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ),
                            LOCATION_PERMISSION_REQUEST_CODE
                        )
                    } else {
                        openAppSettings()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Permission already granted
            startLocationUpdates()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startLocationUpdates()
            }
        }
    }


    private fun startLocationUpdates() {
        LocationUtils.requestCurrentLocation(
            this,
            onLocationReceived = { lat, lon ->

                val latitude = lat.toString()
                val longitude = lon.toString()
                FormatterClass().saveSharedPref("latitude", latitude, this)
                FormatterClass().saveSharedPref("longitude", longitude, this)
            },
            onError = { error ->
                println("Error: $error")

            }
        )
    }

    private fun onSubmitActionSubmit() {
        lifecycleScope.launch {
            val fragment = supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG)
                    as QuestionnaireFragment
            val questionnaireResponse = fragment.getQuestionnaireResponse()

            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val questionnaireResponseString =
                jsonParser.encodeResourceToString(questionnaireResponse)
            Log.d("extraction response", questionnaireResponseString)

            val questionnaire =
                jsonParser.parseResource(viewModel.questionnaireJson) as Questionnaire

            Log.d("Questionnaire Response::::", "$questionnaire")
            Log.d("Questionnaire Response::::: ", "$questionnaireResponse")
            val bundle = ResourceMapper.extract(questionnaire, questionnaireResponse)
            Log.d("Questionnaire Response::::", jsonParser.encodeResourceToString(bundle))
        }
    }

    private fun onSubmitAction() {
        ProgressDialogManager.show(this, "Please Wait.....")
        lifecycleScope.launch {
            val questionnaireFragment =
                supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG)
                        as QuestionnaireFragment


            saveCase(questionnaireFragment.getQuestionnaireResponse(), )
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

    private fun saveCase(
        questionnaireResponse: QuestionnaireResponse
    ) {
        val case = FormatterClass().getSharedPref("currentCase", this@AddParentCaseActivity)
        // print case
        Log.d("Questionnaire Response::::", "$case")
        when (case) {
            "Mpox - Supervisor Checklist" -> {
                viewModel.saveUserResponse(questionnaireResponse, case, this@AddParentCaseActivity)

            }

            else -> {
                viewModel.savePatientData(
                    questionnaireResponse,
                    this@AddParentCaseActivity
                )
            }
        }

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

    private fun addQuestionnaireFragment() {
        lifecycleScope.launch {
            if (supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG) == null) {
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    val questionnaireFragmentBuilder =
                        QuestionnaireFragment.builder().apply {
                            setCustomQuestionnaireItemViewHolderFactoryMatchersProvider(
                                ContribQuestionnaireItemViewHolderFactoryMatchersProviderFactory
                                    .LOCATION_WIDGET_PROVIDER,
                            )
                            setQuestionnaire(viewModel.questionnaireJson)
                        }
//                    LayoutListViewModel.questionnaireLambdaMap[args.questionnaireLambdaKey ?: ""]!!.invoke(
//                        questionnaireFragmentBuilder,
//                    )
                    add(
                        R.id.add_patient_container,
                        questionnaireFragmentBuilder.build(),
                        QUESTIONNAIRE_FRAGMENT_TAG
                    )
                }
            }
        }
    }
//    private fun addQuestionnaireFragment() {
//        supportFragmentManager.commit {
//            add(
//                R.id.add_patient_container,
//                QuestionnaireFragment.builder()
//                    .setQuestionnaire(viewModel.questionnaireJson)
//                    .setShowCancelButton(true)
//                    .setSubmitButtonText("Submit")
//                    .build(),
//                QUESTIONNAIRE_FRAGMENT_TAG,
//            )
//        }
//    }

    private fun observePatientSaveAction() {
        viewModel.isPatientSaved.observe(this) {
            ProgressDialogManager.dismiss()

            if (!it) {
                Toast.makeText(this, "Please Enter all Required Fields.", Toast.LENGTH_SHORT).show()
                return@observe
            }
            showSuccessDialog(this@AddParentCaseActivity)

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
            this@AddParentCaseActivity.finish()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_finish).setOnClickListener {
            // handle finish action
            this@AddParentCaseActivity.finish()
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
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



