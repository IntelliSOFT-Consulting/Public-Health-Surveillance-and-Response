package com.icl.surveillance.viewmodels

import android.app.Application
import android.content.Context
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
import com.ibm.icu.text.SimpleDateFormat
import com.icl.surveillance.clients.AddClientFragment.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import com.icl.surveillance.fhir.FhirApplication
import com.icl.surveillance.models.QuestionnaireAnswer
import com.icl.surveillance.utils.FormatterClass
import com.icl.surveillance.utils.QuestionnaireHelper
import com.icl.surveillance.utils.readFileFromAssets
import java.time.LocalDate
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.json.JSONArray
import org.json.JSONObject

class AddClientViewModel(application: Application, private val state: SavedStateHandle) :
    AndroidViewModel(application) {

    private var _questionnaireJson: String? = null
    val questionnaireJson: String
        get() = fetchQuestionnaireJson()

    val isPatientSaved = MutableLiveData<Boolean>()

    private val questionnaire: Questionnaire
        get() =
            FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
                .parseResource(questionnaireJson)
                    as Questionnaire

    private var fhirEngine: FhirEngine = FhirApplication.fhirEngine(application.applicationContext)

    /**
     * Saves patient registration questionnaire response into the application database.
     *
     * @param questionnaireResponse patient registration questionnaire response
     */
    fun savePatientData(
        questionnaireResponse: QuestionnaireResponse,
        questionnaireResponseString: String,
        context: Context
    ) {
        viewModelScope.launch {
            if (QuestionnaireResponseValidator.validateQuestionnaireResponse(
                    questionnaire,
                    questionnaireResponse,
                    getApplication(),
                )
                    .values
                    .flatten()
                    .any { it is Invalid }
            ) {
                isPatientSaved.value = false
                return@launch
            }

            val patientId = generateUuid()
            val subjectReference = Reference("Patient/$patientId")
            val jsonObject = JSONObject(questionnaireResponseString)
            val extractedAnswers = extractStructuredAnswers(jsonObject)
            val reasonCode = FormatterClass().getSharedPref(
                "currentCase",
                context
            )
            var patient = Patient()
            patient.id = patientId

            val qh = QuestionnaireHelper()
            val encounterId = generateUuid()
            val enc = qh.generalEncounter(null)
            enc.id = encounterId
            enc.subject = subjectReference
            enc.reasonCodeFirstRep.codingFirstRep.code = "$reasonCode"
            var case = "case-information"
            if (reasonCode != null) {
                case = reasonCode.toSlug()

            }

            val patientNameEntry = extractedAnswers.find { it.linkId == "794460715014" }

            var firstName: String? = null
            var secondName: String? = null
            var otherNames: List<String> = emptyList()
            var pfirstName: String? = null
            var psecondName: String? = null
            var potherNames: List<String> = emptyList()

            val encounterReference = Reference("Encounter/$encounterId")
            when (case) {
                "measles-case-information" -> {
                    val genderEntry = extractedAnswers.find { it.linkId == "929966324957" }
                    val dobEntry = extractedAnswers.find { it.linkId == "257830485990" }
                    val parentEntry = extractedAnswers.find { it.linkId == "856448027666" }
                    val residenceEntry = extractedAnswers.find { it.linkId == "242811643559" }
                    val pNeighborEntry = extractedAnswers.find { it.linkId == "946232932304" }
                    val pStreetEntry = extractedAnswers.find { it.linkId == "424111786438" }
                    val pTownEntry = extractedAnswers.find { it.linkId == "110761799063" }
                    val pSubCountyEntry = extractedAnswers.find { it.linkId == "885995384353" }
                    val pCountyEntry = extractedAnswers.find { it.linkId == "301322368614" }
                    val pPhoneEntry = extractedAnswers.find { it.linkId == "754217593839" }

                    val subCountyEntry = extractedAnswers.find { it.linkId == "a3-sub-county" }
                    val countyEntry = extractedAnswers.find { it.linkId == "a4-county" }

                    patientNameEntry?.answer?.let { fullName ->
                        val parts = fullName.trim().split("\\s+".toRegex())
                        when (parts.size) {
                            1 -> {
                                firstName = parts[0]
                            }

                            2 -> {
                                firstName = parts[0]
                                secondName = parts[1]
                            }

                            else -> {
                                firstName = parts[0]
                                secondName = parts[1]
                                otherNames = parts.drop(2)
                            }
                        }
                    }
                    parentEntry?.answer?.let { fullName ->
                        val parts = fullName.trim().split("\\s+".toRegex())
                        when (parts.size) {
                            1 -> {
                                pfirstName = parts[0]
                            }

                            2 -> {
                                pfirstName = parts[0]
                                psecondName = parts[1]
                            }

                            else -> {
                                pfirstName = parts[0]
                                psecondName = parts[1]
                                potherNames = parts.drop(2)
                            }
                        }
                    }

                    if (genderEntry != null) {
                        val gender = when (genderEntry.answer) {
                            "Male" -> Enumerations.AdministrativeGender.MALE
                            "Female" -> Enumerations.AdministrativeGender.FEMALE
                            else -> Enumerations.AdministrativeGender.UNKNOWN
                        }
                        patient.gender = gender
                    }

                    val parentPhone = ContactPoint()
                    if (pPhoneEntry != null) {

                        parentPhone.value = pPhoneEntry.answer
                        parentPhone.system = ContactPoint.ContactPointSystem.PHONE
                        parentPhone.use = ContactPoint.ContactPointUse.MOBILE
                    }
                    val parentAddress = Address()

                    if (residenceEntry != null) {
                        parentAddress.addLine(residenceEntry.answer)
                    }
                    if (pNeighborEntry != null) {
                        parentAddress.addLine(pNeighborEntry.answer)
                    }
                    if (pStreetEntry != null) {
                        parentAddress.addLine(pStreetEntry.answer)
                    }
                    if (pTownEntry != null) {
                        parentAddress.addLine(pTownEntry.answer)
                    }
                    if (pSubCountyEntry != null) {
                        parentAddress.addLine(pSubCountyEntry.answer)
                    }
                    if (pCountyEntry != null) {
                        parentAddress.addLine(pCountyEntry.answer)
                    }
                    try {
                        if (dobEntry != null) {
                            patient.birthDate =
                                SimpleDateFormat("yyyy-MM-dd").parse(dobEntry.answer)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (firstName != null) {
                        patient.nameFirstRep.family = firstName

                    }
                    if (secondName != null) {
                        patient.nameFirstRep.addGiven(secondName)
                    }
                    if (otherNames.isNotEmpty()) {
                        otherNames.forEach {
                            patient.nameFirstRep.addGiven(it)
                        }
                    }
                    val parentName = HumanName()
                    if (pfirstName != null) {
                        parentName.family = pfirstName

                    }
                    if (psecondName != null) {
                        parentName.addGiven(psecondName)
                    }
                    if (potherNames.isNotEmpty()) {
                        potherNames.forEach {
                            parentName.addGiven(it)
                        }
                    }

                    patient.contactFirstRep.name = parentName
                    patient.contactFirstRep.address = parentAddress
                    patient.contactFirstRep.addTelecom(parentPhone)


                    var county = ""
                    var subCounty = ""
                    val currentYear = LocalDate.now().year

                    if (subCountyEntry != null) {
                        subCounty = subCountyEntry.answer
                    }
                    if (countyEntry != null) {
                        county = countyEntry.answer
                    }

                    val countyCode = county.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode = subCounty.padEnd(3, 'X').take(3).uppercase()


                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                        .setCode("EPID").display =
                        "EPID No"
                    obs.code.text = epid
                    createResource(obs, subjectReference, encounterReference)


                }

                "afp-case-information" -> {
                    val fNameEntry = extractedAnswers.find { it.linkId == "873240407472" }
                    val mNameEntry = extractedAnswers.find { it.linkId == "246751846436" }
                    val lNameEntry = extractedAnswers.find { it.linkId == "486402457213" }
                    val genderEntry = extractedAnswers.find { it.linkId == "929966324957" }

                    val subCountyEntry = extractedAnswers.find { it.linkId == "a3-sub-county" }
                    val countyEntry = extractedAnswers.find { it.linkId == "a4-county" }

                    if (genderEntry != null) {
                        val gender = when (genderEntry.answer.lowercase()) {
                            "male" -> Enumerations.AdministrativeGender.MALE
                            "female" -> Enumerations.AdministrativeGender.FEMALE
                            else -> Enumerations.AdministrativeGender.UNKNOWN
                        }
                        patient.gender = gender
                    }
                    if (fNameEntry != null) {
                        patient.nameFirstRep.family = fNameEntry.answer
                    }
                    if (mNameEntry != null) {
                        patient.nameFirstRep.addGiven(mNameEntry.answer)
                    }
                    if (lNameEntry != null) {
                        patient.nameFirstRep.addGiven(lNameEntry.answer)
                    }

                    var county = ""
                    var subCounty = ""
                    val currentYear = LocalDate.now().year

                    if (subCountyEntry != null) {
                        subCounty = subCountyEntry.answer
                    }
                    if (countyEntry != null) {
                        county = countyEntry.answer
                    }

                    val countyCode = county.padEnd(3, 'X').take(3).uppercase()
                    val subCountyCode = subCounty.padEnd(3, 'X').take(3).uppercase()


                    val epid = "KEN-$countyCode-$subCountyCode-$currentYear-"

                    val obs = qh.codingQuestionnaire("EPID", "EPID No", epid)
                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                        .setCode("EPID").display =
                        "EPID No"
                    obs.code.text = epid
                    createResource(obs, subjectReference, encounterReference)
                }
            }
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


                    val identifierSystem = Identifier()
                    val typeCodeableConcept = CodeableConcept()
                    val codingList = ArrayList<Coding>()
                    val coding = Coding()
                    coding.system = case
                    coding.code = case
                    coding.display = case
                    codingList.add(coding)
                    typeCodeableConcept.coding = codingList
                    typeCodeableConcept.text = encounterId

                    identifierSystem.value = encounterId
                    identifierSystem.system = case
                    identifierSystem.type = typeCodeableConcept


                    patient.identifier.add(identifierSystem0)
                    patient.identifier.add(identifierSystem)
                    fhirEngine.create(patient)
                    fhirEngine.create(enc)



                    extractedAnswers.forEach {

                        val obs = qh.codingQuestionnaire(
                            it.linkId, it.text,
                            it.answer
                        )
                        obs.code.addCoding().setSystem("http://snomed.info/sct")
                            .setCode(it.linkId).display =
                            it.text
                        obs.code.text = it.answer
                        createResource(obs, subjectReference, encounterReference)
                        println("Data Found LinkId: ${it.linkId}, Text: ${it.text}, Answer: ${it.answer}")
                    }
                } catch (e: Exception) {
                    Log.e("TAG", "Error experienced ${e.message}}")
                }

                withContext(Dispatchers.Main) { isPatientSaved.value = true }
            }
        }
    }

    private fun String.toSlug(): String {
        return this
            .trim() // remove leading/trailing spaces
            .lowercase() // make all lowercase
            .replace("[^a-z0-9\\s-]".toRegex(), "") // remove special characters
            .replace("\\s+".toRegex(), "-") // replace spaces with hyphens
            .replace("-+".toRegex(), "-") // collapse multiple hyphens
    }

    private fun extractStructuredAnswers(response: JSONObject): List<QuestionnaireAnswer> {
        val results = mutableListOf<QuestionnaireAnswer>()

        fun extractFromItems(items: JSONArray?) {
            if (items == null) return

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val linkId = item.optString("linkId", "N/A")
                val text = item.optString("text", "N/A")

                // Extract answers
                if (item.has("answer")) {
                    val answerArray = item.getJSONArray("answer")
                    for (j in 0 until answerArray.length()) {
                        val answer = answerArray.getJSONObject(j)
                        val value = when {
                            answer.has("valueInteger") -> answer.getString("valueInteger")
                            answer.has("valueString") -> answer.getString("valueString")
                            answer.has("valueDate") -> answer.getString("valueDate")
                            answer.has("valueDateTime") -> answer.getString("valueDateTime")
                            answer.has("valueCoding") -> {
                                val coding = answer.getJSONObject("valueCoding")
                                coding.optString("display", coding.optString("code", ""))
                            }

                            else -> "Unsupported answer type"
                        }
                        results.add(QuestionnaireAnswer(linkId, text, value))
                    }
                }

                // Recurse into nested items
                if (item.has("item")) {
                    extractFromItems(item.getJSONArray("item"))
                }
            }
        }

        extractFromItems(response.optJSONArray("item"))
        return results
    }

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
                    .any { it is Invalid }
            ) {
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

                            println("Manipulating Observations Data **** ${item.getString("linkId")}")
                            when (val linkId = item.getString("linkId")) {
                                "c1-date-onset" -> {
                                    val code = extractResponse(item, "valueDate")
                                    val obs = qh.codingQuestionnaire(
                                        linkId,
                                        "Date of onset of illness",
                                        code
                                    )
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Date of onset of illness"
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "c2-date-first-seen" -> {
                                    val code = extractResponse(item, "valueDate")
                                    val obs = qh.codingQuestionnaire(
                                        linkId,
                                        "Date first seen at facility",
                                        code
                                    )
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Date first seen at facility"
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "c3-date-notified" -> {
                                    val code = extractResponse(item, "valueDate")
                                    val obs = qh.codingQuestionnaire(
                                        linkId,
                                        "Date case was notified",
                                        code
                                    )
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Date case was notified"
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "c5-ip-op-no" -> {
                                    val code = extractResponse(item, "valueString")
                                    val obs = qh.codingQuestionnaire(linkId, "IP/OP No.", code)
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "IP/OP No."
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "c6-diagnosis" -> {
                                    val code = extractResponse(item, "valueString")
                                    val obs = qh.codingQuestionnaire(linkId, "Diagnosis", code)
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Diagnosis"
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "c7-means-of-diagnosis" -> {
                                    val code = extractResponseCode(item, "valueCoding")
                                    val obs =
                                        qh.codingQuestionnaire(linkId, "Means of diagnosis", code)
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Means of diagnosis"
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "c7-other-specify" -> {
                                    val code = extractResponse(item, "valueString")
                                    val obs = qh.codingQuestionnaire(
                                        linkId,
                                        "Other diagnosis means",
                                        code
                                    )
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Other diagnosis means"
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "c8a-vaccinated" -> {
                                    val code = extractResponseCode(item, "valueCoding")
                                    val obs = qh.codingQuestionnaire(
                                        linkId,
                                        "Vaccinated against illness",
                                        code
                                    )
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Vaccinated against illness"
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "c8a-no-of-doses" -> {
                                    val code = extractResponse(item, "valueInteger")
                                    val obs = qh.codingQuestionnaire(
                                        linkId,
                                        "Number of doses received",
                                        code
                                    )
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Number of doses received"
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "c8b-recent-vaccine" -> {
                                    val code = extractResponseCode(item, "valueCoding")
                                    val obs = qh.codingQuestionnaire(
                                        linkId,
                                        "Vaccinated in last two months",
                                        code
                                    )
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Vaccinated in last two months"
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "c8b-date-of-vaccine" -> {
                                    val code = extractResponse(item, "valueDate")
                                    val obs =
                                        qh.codingQuestionnaire(linkId, "Date of last vaccine", code)
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Date of last vaccine"
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "c9-patient-status" -> {
                                    val code = extractResponseCode(item, "valueCoding")
                                    val obs = qh.codingQuestionnaire(
                                        linkId,
                                        "Status of the patient",
                                        code
                                    )
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Status of the patient"
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "inpatient-outpatient" -> {
                                    val code = extractResponseCode(item, "valueCoding")
                                    val obs =
                                        qh.codingQuestionnaire(linkId, "Inpatient/Outpatient", code)
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Inpatient/Outpatient"
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "c9-patient-outcome" -> {
                                    val code = extractResponseCode(item, "valueCoding")
                                    val obs =
                                        qh.codingQuestionnaire(linkId, "Patient Outcome", code)
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
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
                                    val obs =
                                        qh.codingQuestionnaire(linkId, "Health Facility", value)
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Health Facility"
                                    obs.code.text = value
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "a2-type" -> {
                                    val value = extractResponse(item, "valueString")
                                    val obs = qh.codingQuestionnaire(linkId, "Facility Type", value)

                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Facility Type"
                                    obs.code.text = value
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "a3-sub-county" -> {
                                    val value = extractResponse(item, "valueString")
                                    val obs = qh.codingQuestionnaire(linkId, "Sub-County", value)
                                    subCounty = value
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Sub-County"
                                    obs.code.text = value
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "a4-county" -> {
                                    val value = extractResponse(item, "valueString")
                                    val obs = qh.codingQuestionnaire(linkId, "County", value)
                                    county = value
                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "County "
                                    obs.code.text = value
                                    createResource(obs, subjectReference, encounterReference)
                                }

                                "a5-disease-reported" -> {
                                    val code = extractResponseCode(item, "valueCoding")
                                    val obs =
                                        qh.codingQuestionnaire(linkId, "Disease reported ", code)

                                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                                        .setCode(linkId).display =
                                        "Disease reported "
                                    obs.code.text = code
                                    createResource(obs, subjectReference, encounterReference)
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
                    obs.code.addCoding().setSystem("http://snomed.info/sct")
                        .setCode("EPID").display =
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
