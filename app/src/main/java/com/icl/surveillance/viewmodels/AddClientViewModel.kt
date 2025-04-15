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
import com.icl.surveillance.utils.QuestionnaireHelper
import com.icl.surveillance.utils.readFileFromAssets
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.json.JSONObject

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
  fun savePatient(
      questionnaireResponse: QuestionnaireResponse,
      questionnaireResponseString: String
  ) {
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

      val entry =
          ResourceMapper.extract(
                  questionnaire,
                  questionnaireResponse,
              )
              .entryFirstRep
      if (entry.resource !is Patient) {
        return@launch
      }
      val patientID = generateUuid()
      withContext(Dispatchers.IO) {
        try {

          val patient = entry.resource as Patient
          patient.id = patientID
          fhirEngine.create(patient)
          
          proceedToExtractInBackground(patientID, questionnaireResponseString)
          withContext(Dispatchers.Main) { isPatientSaved.value = true }
        } catch (e: Exception) {
          Log.e("SavePatient", "Error saving patient", e)
          withContext(Dispatchers.Main) { isPatientSaved.value = false }
        }

      }
    }
  }

  private fun proceedToExtractInBackground(patientId: String, questionnaireResponse: String) {
    println("Starting work on Observations $questionnaireResponse")
    viewModelScope.launch {
      try {
        val qh = QuestionnaireHelper()
        val encounterId = generateUuid()
        val enc = qh.generalEncounter(null)
        enc.id = encounterId
        fhirEngine.create(enc)

        val json = JSONObject(questionnaireResponse)
        val items = json.getJSONArray("item")

        val subjectReference = Reference("Patient/$patientId")
        val encounterReference = Reference("Encounter/$encounterId")
        //
        for (i in 0 until items.length()) {
          val section = items.getJSONObject(i)
          val sectionLinkId = section.getString("linkId")

          // Looking inside "section-a" only
          if (sectionLinkId == "section-a" && section.has("item")) {
            val facilityItems = section.getJSONArray("item")

            for (j in 0 until facilityItems.length()) {
              val item = facilityItems.getJSONObject(j)
              when (val linkId = item.getString("linkId")) {
                "a1-health-facility" -> {
                  val value = extractResponse(item, "valueString")
                  val obs = qh.codingQuestionnaire(linkId, "Health Facility", value)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Health Facility"
                  obs.code.text = value
                  createResource(obs, subjectReference, encounterReference)
                }

              //                "a2-type" -> {
              //                  val value = extractResponse(item, "valueString")
              //                  val obs = qh.codingQuestionnaire(linkId, "Facility Type", value)
              //
              // obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
              //                      "Facility Type"
              //                  obs.code.text = value
              //                  createResource(obs, subjectReference, encounterReference)
              //                }
              //
              //                "a3-sub-county" -> {
              //                  val value = extractResponse(item, "valueString")
              //                  val obs = qh.codingQuestionnaire(linkId, "Sub-County", value)
              //
              // obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
              //                      "Sub-County"
              //                  obs.code.text = value
              //                  createResource(obs, subjectReference, encounterReference)
              //                }
              //
              //                "a4-county" -> {
              //                  val value = extractResponse(item, "valueString")
              //                  val obs = qh.codingQuestionnaire(linkId, "County", value)
              //
              // obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
              //                      "County "
              //                  obs.code.text = value
              //                  createResource(obs, subjectReference, encounterReference)
              //                }
              //
              //                "a5-disease-reported" -> {
              //                  val code = extractResponseCode(item, "valueCoding")
              //                  val obs = qh.codingQuestionnaire(linkId, "Disease reported ",
              // code)
              //
              // obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
              //                      "Disease reported "
              //                  obs.code.text = code
              //                  createResource(obs, subjectReference, encounterReference)
              //                }
              }
            }
          }
        }
      } catch (e: Exception) {
        Log.e("SavePatient", "Error extracting observations", e)
      }
    }
  }

  private suspend fun createResource(
      obs: Observation,
      subjectReference: Reference,
      encounterReference: Reference
  ) {
    try {
      obs.id = generateUuid()
      obs.subject = subjectReference
      obs.encounter = encounterReference
      obs.issued = Date()
      fhirEngine.create(obs)

      println("Observation created: ${obs.id}")
    } catch (e: Exception) {
      Log.e("SavePatient", "Error saving patient", e)
    }
  }

  private fun extractResponseCode(obj: JSONObject, key: String): String {
    return try {
      val answer = obj.getJSONArray("answer").getJSONObject(0)
      val coding = answer.getJSONObject(key)
      coding.optString("display", coding.optString("code", ""))
    } catch (e: Exception) {
      ""
    }
  }

  private fun extractResponse(obj: JSONObject, key: String): String {
    return try {
      val answer = obj.getJSONArray("answer").getJSONObject(0)
      answer.optString(key, "")
    } catch (e: Exception) {
      ""
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
