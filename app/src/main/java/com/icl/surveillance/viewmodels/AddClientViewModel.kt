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
import com.icl.surveillance.utils.FormatterClass
import com.icl.surveillance.utils.QuestionnaireHelper
import com.icl.surveillance.utils.readFileFromAssets
import java.time.LocalDate
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Identifier
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
          val identifierSystem0 = Identifier()

          val typeCodeableConcept0 = CodeableConcept()

          val codingList0 = ArrayList<Coding>()
          val coding0 = Coding()
          coding0.system = "system-creation"
          coding0.code = "system_creation"
          coding0.display = "System Creation"
          codingList0.add(coding0)
          typeCodeableConcept0.coding = codingList0
          typeCodeableConcept0.text = FormatterClass().formatCurrentDateTime(Date())

          identifierSystem0.value = FormatterClass().formatCurrentDateTime(Date())
          identifierSystem0.system = "system-creation"
          identifierSystem0.type = typeCodeableConcept0

          val patient = entry.resource as Patient
          patient.id = patientID
          patient.identifier.add(identifierSystem0)
          fhirEngine.create(patient)

          proceedToExtractInBackground(patientID, questionnaireResponseString)
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

        val subjectReference = Reference("Patient/$patientId")
        val qh = QuestionnaireHelper()
        val encounterId = generateUuid()
        val enc = qh.generalEncounter(null)
        enc.id = encounterId
        enc.subject = subjectReference
        enc.reasonCodeFirstRep.codingFirstRep.code = "Case Information"
        fhirEngine.create(enc)

        val json = JSONObject(questionnaireResponse)
        val items = json.getJSONArray("item")

        val encounterReference = Reference("Encounter/$encounterId")
        //
        var county = ""
        var subCounty = ""
        val currentYear = LocalDate.now().year

        for (i in 0 until items.length()) {
          val section = items.getJSONObject(i)
          val sectionLinkId = section.getString("linkId")

          // Looking inside "section-c" only
          if (sectionLinkId == "section-c" && section.has("item")) {
            val facilityItems = section.getJSONArray("item")

            for (j in 0 until facilityItems.length()) {
              val item = facilityItems.getJSONObject(j)

              when (val linkId = item.getString("linkId")) {
                "c2-date-first-seen" -> {
                  val code = extractResponse(item, "valueDate")
                  val obs = qh.codingQuestionnaire(linkId, "Date first seen at facility", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Date first seen at facility"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c3-date-notified" -> {
                  val code = extractResponse(item, "valueDate")
                  val obs = qh.codingQuestionnaire(linkId, "Date case was notified", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Date case was notified"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c5-ip-op-no" -> {
                  val code = extractResponse(item, "valueString")
                  val obs = qh.codingQuestionnaire(linkId, "IP/OP No.", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "IP/OP No."
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c6-diagnosis" -> {
                  val code = extractResponse(item, "valueString")
                  val obs = qh.codingQuestionnaire(linkId, "Diagnosis", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Diagnosis"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c7-means-of-diagnosis" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  val obs = qh.codingQuestionnaire(linkId, "Means of diagnosis", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Means of diagnosis"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c7-other-specify" -> {
                  val code = extractResponse(item, "valueString")
                  val obs = qh.codingQuestionnaire(linkId, "Other diagnosis means", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Other diagnosis means"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c8a-vaccinated" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  val obs = qh.codingQuestionnaire(linkId, "Vaccinated against illness", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Vaccinated against illness"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c8a-no-of-doses" -> {
                  val code = extractResponse(item, "valueInteger")
                  val obs = qh.codingQuestionnaire(linkId, "Number of doses received", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Number of doses received"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c8b-recent-vaccine" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  val obs = qh.codingQuestionnaire(linkId, "Vaccinated in last two months", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Vaccinated in last two months"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c8b-date-of-vaccine" -> {
                  val code = extractResponse(item, "valueDate")
                  val obs = qh.codingQuestionnaire(linkId, "Date of last vaccine", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Date of last vaccine"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c4-date-admission" -> {
                  val code = extractResponse(item, "valueDate")
                  val obs = qh.codingQuestionnaire(linkId, "Date of Admission", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Date of Admission"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c9-patient-status" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  val obs = qh.codingQuestionnaire(linkId, "Status of the patient", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Status of the patient"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }
                "lab-information" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  val obs = qh.codingQuestionnaire(linkId, "Sample Collected", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Sample Collected"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "inpatient-outpatient" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  val obs = qh.codingQuestionnaire(linkId, "Inpatient/Outpatient", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Inpatient/Outpatient"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c9-patient-outcome" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  val obs = qh.codingQuestionnaire(linkId, "Patient Outcome", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Patient Outcome"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }
              }
            }
          }
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

                "a2-type" -> {
                  val value = extractResponse(item, "valueString")
                  val obs = qh.codingQuestionnaire(linkId, "Facility Type", value)

                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Facility Type"
                  obs.code.text = value
                  createResource(obs, subjectReference, encounterReference)
                }

                "a3-sub-county" -> {
                  val value = extractResponse(item, "valueString")
                  val obs = qh.codingQuestionnaire(linkId, "Sub-County", value)
                  subCounty = value
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Sub-County"
                  obs.code.text = value
                  createResource(obs, subjectReference, encounterReference)
                }

                "a4-county" -> {
                  val value = extractResponse(item, "valueString")
                  val obs = qh.codingQuestionnaire(linkId, "County", value)
                  county = value
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "County "
                  obs.code.text = value
                  createResource(obs, subjectReference, encounterReference)
                }

                "a5-disease-reported" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  val obs = qh.codingQuestionnaire(linkId, "Disease reported ", code)

                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Disease reported "
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }
              }
            }
          }
          if (sectionLinkId == "section-b" && section.has("item")) {
            val facilityItems = section.getJSONArray("item")

            for (j in 0 until facilityItems.length()) {
              val item = facilityItems.getJSONObject(j)

              when (val linkId = item.getString("linkId")) {
                "residence-setup" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  val obs = qh.codingQuestionnaire(linkId, "Residence", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Residence"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }
              }
            }
          }
          if (sectionLinkId == "section-lab" && section.has("item")) {
            val facilityItems = section.getJSONArray("item")

            for (j in 0 until facilityItems.length()) {
              val item = facilityItems.getJSONObject(j)

              when (val linkId = item.getString("linkId")) {
                "g1a" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  if (code.isNotEmpty()) {

                    val label = "Specimen Collection (To be completed by the health facility)"
                    createObs(
                        qh = qh,
                        linkId = linkId,
                        code = code,
                        label = label,
                        subjectReference = subjectReference,
                        encounterReference = encounterReference)
                  }
                }
                "g1a1" -> {
                  val code = extractResponse(item, "valueString")
                  val label = "If no, why?"

                  createObs(
                      qh = qh,
                      linkId = linkId,
                      code = code,
                      label = label,
                      subjectReference = subjectReference,
                      encounterReference = encounterReference)
                }
                "g1b1" -> {

                  val code = extractResponse(item, "valueDate")
                  val label = "Date(s) of specimen collection"

                  createObs(
                      qh = qh,
                      linkId = linkId,
                      code = code,
                      label = label,
                      subjectReference = subjectReference,
                      encounterReference = encounterReference)
                }
                "g1c" -> {

                  val code = extractResponseCode(item, "valueCoding")
                  val label = "Specimen type"

                  createObs(
                      qh = qh,
                      linkId = linkId,
                      code = code,
                      label = label,
                      subjectReference = subjectReference,
                      encounterReference = encounterReference)
                }
                "g1c1" -> {

                  val code = extractResponse(item, "valueString")
                  val label = "If Other, specify specimen type"

                  createObs(
                      qh = qh,
                      linkId = linkId,
                      code = code,
                      label = label,
                      subjectReference = subjectReference,
                      encounterReference = encounterReference)
                }
                "g1d" -> {

                  val code = extractResponse(item, "valueDate")
                  val label = "Date specimen sent to the lab"
                  createObs(
                      qh = qh,
                      linkId = linkId,
                      code = code,
                      label = label,
                      subjectReference = subjectReference,
                      encounterReference = encounterReference)
                }
                "g1e" -> {
                  val code = extractResponse(item, "valueString")
                  val label = "Name of the lab"

                  createObs(
                      qh = qh,
                      linkId = linkId,
                      code = code,
                      label = label,
                      subjectReference = subjectReference,
                      encounterReference = encounterReference)
                }
              }
            }
          }
          if (sectionLinkId == "section-d" && section.has("item")) {
            val facilityItems = section.getJSONArray("item")

            for (j in 0 until facilityItems.length()) {
              val item = facilityItems.getJSONObject(j)

              when (val linkId = item.getString("linkId")) {
                "f1" -> {
                  val code = extractResponseCode(item, "valueCoding")

                  val obs = qh.codingQuestionnaire(linkId, "Clinical symptoms", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Clinical symptoms"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }
                "f2" -> {
                  val code = extractResponse(item, "valueDate")
                  val obs = qh.codingQuestionnaire(linkId, "Date of onset of rash", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Date of onset of rash"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }
                "f3" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  val obs = qh.codingQuestionnaire(linkId, "Type of rash", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Type of rash"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }
                "c1-date-onset" -> {
                  val code = extractResponse(item, "valueDate")
                  val obs = qh.codingQuestionnaire(linkId, "Date of onset of illness", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Date of onset of illness"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }
                "c8b-date-of-vaccine" -> {
                  val code = extractResponse(item, "valueDate")
                  val obs = qh.codingQuestionnaire(linkId, "Date of last vaccination", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Date of last vaccination"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "f4a" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  if (code.isNotEmpty()) {

                    val obs =
                        qh.codingQuestionnaire(
                            linkId, "Was home of patient visited for contact investigation?", code)
                    obs.code
                        .addCoding()
                        .setSystem("http://snomed.info/sct")
                        .setCode(linkId)
                        .display = "Was home of patient visited for contact investigation?"
                    obs.code.text = code
                    createResource(obs, subjectReference, encounterReference)
                  }
                }
                "c8a-vaccinated" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  val obs =
                      qh.codingQuestionnaire(
                          linkId,
                          "Was the patient vaccinated against illness (including campaign)?",
                          code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Was the patient vaccinated against illness (including campaign)?"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }
                "c8a-no-of-doses" -> {
                  val code = extractResponse(item, "valueInteger")
                  val obs = qh.codingQuestionnaire(linkId, "If yes, number of doses", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "If yes, number of doses"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "c8b-recent-vaccine" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  val obs =
                      qh.codingQuestionnaire(
                          linkId, "Any vaccination given in the last 30 days?", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "Any vaccination given in the last 30 days?"

                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }
                "f4b" -> {
                  val code = extractResponse(item, "valueDate")

                  val obs = qh.codingQuestionnaire(linkId, "If Yes, Date of visit", code)
                  obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display =
                      "If Yes, Date of visit"
                  obs.code.text = code
                  createResource(obs, subjectReference, encounterReference)
                }

                "f5" -> {
                  val code = extractResponseCode(item, "valueCoding")
                  if (code.isNotEmpty()) {

                    val obs =
                        qh.codingQuestionnaire(
                            linkId,
                            "Is the case epidemiologically linked to a lab-confirmed case?",
                            code)
                    obs.code
                        .addCoding()
                        .setSystem("http://snomed.info/sct")
                        .setCode(linkId)
                        .display = "Is the case epidemiologically linked to a lab-confirmed case?"
                    obs.code.text = code
                    createResource(obs, subjectReference, encounterReference)
                  }
                }
              }
            }
          }
        }

        if (subCounty.isNotEmpty() && county.isNotEmpty()) {
          val countyCode = county.padEnd(3, 'X').take(3).uppercase()
          val subCountyCode = subCounty.padEnd(3, 'X').take(3).uppercase()
          val epid = "KEN-$countyCode-$subCountyCode-$currentYear-"
          val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
          obs.code.addCoding().setSystem("http://snomed.info/sct").setCode("EPID").display =
              "EPID No"
          obs.code.text = epid
          createResource(obs, subjectReference, encounterReference)
        }

        withContext(Dispatchers.Main) { isPatientSaved.value = true }
      } catch (e: Exception) {
        Log.e("SavePatient", "Error extracting observations", e)
      }
    }
  }

  private suspend fun createObs(
      linkId: String,
      code: String,
      label: String,
      qh: QuestionnaireHelper,
      subjectReference: Reference,
      encounterReference: Reference
  ) {
    val obs = qh.codingQuestionnaire(linkId, label, code)
    obs.code.addCoding().setSystem("http://snomed.info/sct").setCode(linkId).display = label
    obs.code.text = code
    createResource(obs, subjectReference, encounterReference)
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
