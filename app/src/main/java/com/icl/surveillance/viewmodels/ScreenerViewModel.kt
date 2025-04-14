package com.icl.surveillance.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.fhir.FhirApplication
import com.icl.surveillance.utils.QuestionnaireHelper
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.json.JSONObject

class ScreenerViewModel(application: Application, private val state: SavedStateHandle) :
    AndroidViewModel(application) {
  val questionnaire: String
    get() = getQuestionnaireJson()

  val isResourcesSaved = MutableLiveData<Boolean>()

  private val questionnaireResource: Questionnaire
    get() =
        FhirContext.forCached(FhirVersionEnum.R4).newJsonParser().parseResource(questionnaire)
            as Questionnaire

  private var questionnaireJson: String? = null
  private var fhirEngine: FhirEngine = FhirApplication.fhirEngine(application.applicationContext)

  /**
   * Saves screener encounter questionnaire response into the application database.
   *
   * @param questionnaireResponse screener encounter questionnaire response
   */
  fun completeAssessment(questionnaireResponse: QuestionnaireResponse, patientId: String) {
    viewModelScope.launch {
      val bundle = ResourceMapper.extract(questionnaireResource, questionnaireResponse)
      val context = FhirContext.forR4()
      val questionnaire = context.newJsonParser().encodeResourceToString(questionnaireResponse)

      CoroutineScope(Dispatchers.IO).launch {
        try {
          //          if (isRequiredFieldMissing(bundle)) {
          //            customMessage.postValue(
          //              MessageItem(
          //                success = false,
          //                message = "Check required fields"
          //              )
          //            )
          //            return@launch
          //          }

          /** Extract Observations, Patient Data */
          val qh = QuestionnaireHelper()
          bundle.addEntry().setResource(qh.generalEncounter(null)).request.url = "Encounter"

          val json = JSONObject(questionnaire)
          val items = json.getJSONArray("item")

          for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val linkId = item.getString("linkId")

            when (linkId) {
              "F1" -> {
                val code = extractResponseCode(item, "valueCoding")
                if (code.isNotEmpty()) {
                  bundle
                      .addEntry()
                      .setResource(qh.codingQuestionnaire("F1", "Presence of fever", code))
                      .request
                      .url = "Observation"
                }
              }

              "F2" -> {
                val code = extractResponse(item, "valueDate")
                bundle
                    .addEntry()
                    .setResource(
                        qh.codingTimeAutoQuestionnaire("F2", "Date of onset of rash", code))
                    .request
                    .url = "Observation"
              }

              "F3" -> {
                val code = extractResponseCode(item, "valueCoding")
                if (code.isNotEmpty()) {
                  bundle
                      .addEntry()
                      .setResource(qh.codingQuestionnaire("F3", "Type of rash", code))
                      .request
                      .url = "Observation"
                }
              }

              "F4.1" -> {
                val code = extractResponseCode(item, "valueCoding")
                if (code.isNotEmpty()) {
                  bundle
                      .addEntry()
                      .setResource(
                          qh.codingQuestionnaire(
                              "F4.1",
                              "Was home of patient visited for contact investigation?",
                              code))
                      .request
                      .url = "Observation"
                }
              }

              "F4.2" -> {
                val code = extractResponse(item, "valueDate")
                bundle
                    .addEntry()
                    .setResource(
                        qh.codingTimeAutoQuestionnaire("F4.2", "If Yes, Date of visit", code))
                    .request
                    .url = "Observation"
              }

              "F5" -> {
                val code = extractResponseCode(item, "valueCoding")
                if (code.isNotEmpty()) {
                  bundle
                      .addEntry()
                      .setResource(
                          qh.codingQuestionnaire(
                              "F5",
                              "Is the case epidemiologically linked to a lab-confirmed case?",
                              code))
                      .request
                      .url = "Observation"
                }
              }
            }
          }

          val encounterId = generateUuid()
          val subjectReference = Reference("Patient/$patientId")
          val title = "Measles Case"
          saveResources(bundle, subjectReference, encounterId, title)
          CoroutineScope(Dispatchers.Main).launch { isResourcesSaved.value = true }
        } catch (e: Exception) {
          println("Error Experienced ${e.message}")
          CoroutineScope(Dispatchers.Main).launch { isResourcesSaved.value = false }
          return@launch
        }
      }
    }
  }

  fun completeLabAssessment(
      questionnaireResponse: QuestionnaireResponse,
      patientId: String,
      encounter: String
  ) {
    viewModelScope.launch {
      val bundle = ResourceMapper.extract(questionnaireResource, questionnaireResponse)
      val context = FhirContext.forR4()
      val questionnaire = context.newJsonParser().encodeResourceToString(questionnaireResponse)

      CoroutineScope(Dispatchers.IO).launch {
        try {
          //          if (isRequiredFieldMissing(bundle)) {
          //            customMessage.postValue(
          //              MessageItem(
          //                success = false,
          //                message = "Check required fields"
          //              )
          //            )
          //            return@launch
          //          }

          /** Extract Observations, Patient Data */
          val qh = QuestionnaireHelper()
          bundle.addEntry().setResource(qh.generalEncounter(encounter)).request.url = "Encounter"

          val json = JSONObject(questionnaire)
          val items = json.getJSONArray("item")

          for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val linkId = item.getString("linkId")

            when (linkId) {
              "date-sent-to-district" -> {
                val code = extractResponse(item, "valueDate")
                bundle
                    .addEntry()
                    .setResource(
                        qh.codingTimeAutoQuestionnaire(linkId, "Date sent form to District", code))
                    .request
                    .url = "Observation"
              }

              "date-received-at-district" -> {
                val code = extractResponse(item, "valueDate")
                bundle
                    .addEntry()
                    .setResource(
                        qh.codingTimeAutoQuestionnaire(
                            linkId, "Date received form at District Level", code))
                    .request
                    .url = "Observation"
              }

              "lab-id" -> {
                val code = extractResponse(item, "valueString")
                bundle
                    .addEntry()
                    .setResource(qh.codingQuestionnaire(linkId, "Lab ID", code))
                    .request
                    .url = "Observation"
              }

              "date-specimen-collected" -> {
                val code = extractResponse(item, "valueDate")
                bundle
                    .addEntry()
                    .setResource(
                        qh.codingTimeAutoQuestionnaire(linkId, "Date Specimen collected", code))
                    .request
                    .url = "Observation"
              }

              "date-sent-to-lab" -> {
                val code = extractResponse(item, "valueDate")
                bundle
                    .addEntry()
                    .setResource(qh.codingTimeAutoQuestionnaire(linkId, "Date sent to lab", code))
                    .request
                    .url = "Observation"
              }

              "date-lab-received-specimen" -> {
                val code = extractResponse(item, "valueDate")
                bundle
                    .addEntry()
                    .setResource(
                        qh.codingTimeAutoQuestionnaire(linkId, "Date Lab received specimen", code))
                    .request
                    .url = "Observation"
              }

              "specimen-condition" -> {
                val code = extractResponseCode(item, "valueCoding")
                if (code.isNotEmpty()) {
                  bundle
                      .addEntry()
                      .setResource(qh.codingQuestionnaire(linkId, "Specimen Condition", code))
                      .request
                      .url = "Observation"
                }
              }

              "measles-igm-result" -> {
                val code = extractResponseCode(item, "valueCoding")
                if (code.isNotEmpty()) {
                  bundle
                      .addEntry()
                      .setResource(qh.codingQuestionnaire(linkId, "Measles IgM Result", code))
                      .request
                      .url = "Observation"
                }
              }

              "rubella-igm-result" -> {
                val code = extractResponseCode(item, "valueCoding")
                if (code.isNotEmpty()) {
                  bundle
                      .addEntry()
                      .setResource(qh.codingQuestionnaire(linkId, "Rubella IgM Result", code))
                      .request
                      .url = "Observation"
                }
              }
            }
          }

          val encounterId = generateUuid()
          val subjectReference = Reference("Patient/$patientId")
          val title = "Measles Lab Information"
          saveResources(bundle, subjectReference, encounterId, title)
          CoroutineScope(Dispatchers.Main).launch { isResourcesSaved.value = true }
        } catch (e: Exception) {
          println("Error Experienced ${e.message}")
          CoroutineScope(Dispatchers.Main).launch { isResourcesSaved.value = false }
          return@launch
        }
      }
    }
  }

  private fun extractResponseQuantity(child: JSONObject, value: String): String {

    val childAnswer = child.getJSONArray("item")
    val ans = childAnswer.getJSONObject(0).getJSONArray("answer")
    return ans.getJSONObject(0).getJSONObject(value).getString("value")
  }

  fun extractResponseCode(obj: JSONObject, key: String): String {
    return try {
      val answer = obj.getJSONArray("answer").getJSONObject(0)
      val coding = answer.getJSONObject(key)
      coding.optString("display", coding.optString("code", ""))
    } catch (e: Exception) {
      ""
    }
  }

  fun extractResponse(obj: JSONObject, key: String): String {
    return try {
      val answer = obj.getJSONArray("answer").getJSONObject(0)
      answer.optString(key, "")
    } catch (e: Exception) {
      ""
    }
  }

  private suspend fun saveResources(
      bundle: Bundle,
      subjectReference: Reference,
      encounterId: String,
      reason: String,
  ) {

    val encounterReference = Reference("Encounter/$encounterId")
    bundle.entry.forEach {
      when (val resource = it.resource) {
        is Observation -> {
          if (resource.hasCode()) {
            resource.id = generateUuid()
            resource.subject = subjectReference
            resource.encounter = encounterReference
            resource.issued = Date()
            saveResourceToDatabase(resource)
          }
        }
        is Condition -> {
          if (resource.hasCode()) {
            resource.id = generateUuid()
            resource.subject = subjectReference
            resource.encounter = encounterReference
            saveResourceToDatabase(resource)
          }
        }
        is Encounter -> {
          resource.subject = subjectReference
          resource.id = encounterId
          resource.reasonCodeFirstRep.text = reason
          resource.reasonCodeFirstRep.codingFirstRep.code = reason
          resource.status = Encounter.EncounterStatus.INPROGRESS
          saveResourceToDatabase(resource)
        }
      }
    }
  }

  private fun isRequiredFieldMissing(bundle: Bundle): Boolean {
    bundle.entry.forEach {
      val resource = it.resource
      when (resource) {
        is Observation -> {
          if (resource.hasValueQuantity() && !resource.valueQuantity.hasValueElement()) {
            return true
          }
        }
      // TODO check other resources inputs
      }
    }
    return false
  }

  private suspend fun saveResourceToDatabase(resource: Resource) {
    fhirEngine.create(resource)
  }

  private fun getQuestionnaireJson(): String {
    questionnaireJson?.let {
      return it!!
    }
    questionnaireJson = readFileFromAssets(state[QUESTIONNAIRE_FILE_PATH_KEY]!!)
    return questionnaireJson!!
  }

  private fun readFileFromAssets(filename: String): String {
    return getApplication<Application>().assets.open(filename).bufferedReader().use {
      it.readText()
    }
  }

  private fun generateUuid(): String {
    return UUID.randomUUID().toString()
  }

  private companion object {
    const val ASTHMA = "161527007"
    const val LUNG_DISEASE = "13645005"
    const val DEPRESSION = "35489007"
    const val DIABETES = "161445009"
    const val HYPER_TENSION = "161501007"
    const val HEART_DISEASE = "56265001"
    const val HIGH_BLOOD_LIPIDS = "161450003"

    const val FEVER = "386661006"
    const val SHORTNESS_BREATH = "13645005"
    const val COUGH = "49727002"
    const val LOSS_OF_SMELL = "44169009"

    const val SPO2 = "59408-5"

    private val comorbidities: Set<String> =
        setOf(
            ASTHMA,
            LUNG_DISEASE,
            DEPRESSION,
            DIABETES,
            HYPER_TENSION,
            HEART_DISEASE,
            HIGH_BLOOD_LIPIDS,
        )
    private val symptoms: Set<String> = setOf(FEVER, SHORTNESS_BREATH, COUGH, LOSS_OF_SMELL)
  }
}
