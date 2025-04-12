package com.icl.surveillance.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.fhir.FhirApplication
import com.icl.surveillance.utils.readFileFromAssets
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse

class AddClientViewModel(application: Application, private val state: SavedStateHandle) :
    AndroidViewModel(application) {

  private var _questionnaireJson: String? = null
  val questionnaireJson: String
    get() = fetchQuestionnaireJson()

  val isPatientSaved = MutableLiveData<Boolean>()

  private val questionnaire: Questionnaire
    get() =
        FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().parseResource(questionnaireJson)
            as Questionnaire

  private var fhirEngine: FhirEngine = FhirApplication.fhirEngine(application.applicationContext)

  /**
   * Saves patient registration questionnaire response into the application database.
   *
   * @param questionnaireResponse patient registration questionnaire response
   */
  fun savePatient(questionnaireResponse: QuestionnaireResponse) {
    viewModelScope.launch {
      if (QuestionnaireResponseValidator.validateQuestionnaireResponse(
              questionnaire,
              questionnaireResponse,
              getApplication(),
          )
          .values
          .flatten()
          .any { it is Invalid }) {
        isPatientSaved.value = false
        return@launch
      }
      val resources =
          ResourceMapper.extract(
              questionnaire,
              questionnaireResponse,
          )
      val entry =
          ResourceMapper.extract(
                  questionnaire,
                  questionnaireResponse,
              )
              .entryFirstRep
      if (entry.resource !is Patient) {
        return@launch
      }
      withContext(Dispatchers.IO) {
        try {
          val patientID = generateUuid()
          val patient = entry.resource as Patient
          patient.id = patientID
          fhirEngine.create(patient)
          withContext(Dispatchers.Main) { isPatientSaved.value = true }
          proceedToExtractInBackground(patientID, resources)
        } catch (e: Exception) {
          Log.e("SavePatient", "Error saving patient", e)
          withContext(Dispatchers.Main) { isPatientSaved.value = false }
        }
      }
    }
  }

  private suspend fun proceedToExtractInBackground(patientID: String, bundle: Bundle) {
    withContext(Dispatchers.IO) {
      // Example: Save Observations linked to the Patient
      bundle.entry.forEach { entry ->
        val resource = entry.resource
        //        if (resource !is Patient) {
        //          resource.subject = Reference("Patient/$patientID")
        //          fhirEngine.create(resource)
        //        }

        Log.d("BackgroundOps", "All resources saved successfully. $entry")
      }
    }
  }

  private fun fetchQuestionnaireJson(): String {
    _questionnaireJson?.let {
      return it
    }
    _questionnaireJson =
        getApplication<Application>().readFileFromAssets(state[QUESTIONNAIRE_FILE_PATH_KEY]!!)
    return _questionnaireJson!!
  }

  private fun generateUuid(): String {
    return UUID.randomUUID().toString()
  }
}
