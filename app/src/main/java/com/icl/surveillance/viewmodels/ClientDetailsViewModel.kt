package com.icl.surveillance.viewmodels

import android.app.Application
import android.content.res.Resources
import android.icu.text.DateFormat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import com.google.android.fhir.datacapture.extensions.asStringValue
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.revInclude
import com.google.android.fhir.search.search
import com.icl.surveillance.R
import com.icl.surveillance.ui.patients.PatientListViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType

class ClientDetailsViewModel(
    application: Application,
    private val fhirEngine: FhirEngine,
    private val patientId: String,
) : AndroidViewModel(application) {
  val livePatientData = MutableLiveData<List<PatientListViewModel.PatientDetailData>>()
  val livecaseData = MutableLiveData<PatientListViewModel.CaseDetailData>()
  val liveDiseaseData = MutableLiveData<List<PatientListViewModel.CaseDiseaseData>>()
  val liveLabData = MutableLiveData<List<PatientListViewModel.CaseLabResultsData>>()

  /** Emits list of [PatientDetailData]. */
  fun getPatientDetailData(category: String, parent: String?) {
    viewModelScope.launch { livePatientData.value = getPatientDetailDataModel(category, parent) }
  }

  private suspend fun getPatientDetailDataModel(
      category: String,
      parent: String?
  ): List<PatientListViewModel.PatientDetailData> {
    val searchResult =
        fhirEngine.search<Patient> {
          filter(Resource.RES_ID, { value = of(patientId) })
          revInclude<Observation>(Observation.SUBJECT)
          revInclude<Condition>(Condition.SUBJECT)
          revInclude<Encounter>(Encounter.SUBJECT)
        }
    val data = mutableListOf<PatientListViewModel.PatientDetailData>()

    searchResult.first().let {
      it.revIncluded?.get(ResourceType.Encounter to Encounter.SUBJECT.paramName)?.let {
        //        data.addEncounterData(it as List<Encounter>, category, parent)
      }
    }

    return data
  }

  private fun MutableList<PatientListViewModel.EncounterItem>.addEncounterData(
      datas: List<Encounter>,
      category: String,
      parent: String?
  ) {
    if (datas.isNotEmpty()) {

      datas
          .filter { encounter ->
            if (parent != null) {
              encounter.partOf.display == parent
            }
            encounter.reasonCode.any { reason -> reason.coding.any { it.code == category } }
          }
          .take(100)
          .map { createEncounterItem(it, getApplication<Application>().resources) }
          .mapIndexed { index, data ->
            PatientListViewModel.EncounterItem(
                id = data.id, reasonCode = data.reasonCode, status = data.status)
          }
          .let { addAll(it) }
    }
  }

  private val LocalDate.localizedString: String
    get() {
      val date = Date.from(atStartOfDay(ZoneId.systemDefault())?.toInstant())
      return if (isAndroidIcuSupported()) {
        DateFormat.getDateInstance(DateFormat.DEFAULT).format(date)
      } else {
        SimpleDateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault()).format(date)
      }
    }

  // Android ICU is supported API level 24 onwards.
  private fun isAndroidIcuSupported() = true

  private fun getString(resId: Int) = getApplication<Application>().resources.getString(resId)

  private suspend fun loadEncounter(patientId: String): List<Encounter> {
    return fhirEngine
        .search<Encounter> { filter(Encounter.SUBJECT, { value = "Patient/$patientId" }) }
        .map { it.resource }
  }

  private suspend fun getPatientInfoCard(): PatientListViewModel.CaseDetailData {
    val searchResult =
        fhirEngine.search<Patient> { filter(Resource.RES_ID, { value = of(patientId) }) }
    var logicalId = ""
    var name = ""
    var sex = ""
    var dob = ""
    var epid = ""
    var county = ""
    var subCounty = ""
    var onset = ""
    var facility = ""
    var type = ""
    var disease = ""
    var dateFirstSeen = ""
    var dateSubCountyNotified = ""
    var hospitalized = ""
    var ipNo = ""
    var diagnosis = ""
    var diagnosisMeans = ""
    var diagnosisMeansOther = ""
    var wasPatientVaccinated = ""
    var noOfDoses = ""
    var twoMonthsVaccination = ""
    var patientStatus = ""
    var admissionDate = ""
    var vaccineDate = ""
    var residence = ""
    var parent = ""
    var houseno = ""
    var parentPhone = ""
    var countyName = ""
    var subCountyName = ""
    var neighbour = ""
    var street = ""
    var town = ""

    var clinicalSymptoms = ""
    var rashDate = ""
    var rashType = ""
    var patientVaccinated = ""
    var patientDoses = ""
    var vaccineDateThirtyDays = ""
    var lastDoseDate = ""
    var homeVisited = ""
    var homeVisitedDate = ""
    var epiLinked = ""

    searchResult.first().let {
      logicalId = it.resource.logicalId
      name =
          if (it.resource.hasName()) {
            // display name in order as fname, then others
            "${it.resource.name[0].givenAsSingleString} ${it.resource.name[0].family} "
          } else ""
      sex = if (it.resource.hasGenderElement()) it.resource.gender.display else ""
      dob =
          if (it.resource.hasBirthDateElement())
              if (it.resource.birthDateElement.hasValue())
                  it.resource.birthDateElement.valueAsString
              else ""
          else ""

      val encounter = loadEncounter(logicalId)
      val caseInfoEncounter =
          encounter.firstOrNull { it.reasonCodeFirstRep.codingFirstRep.code == "Case Information" }

      caseInfoEncounter?.let {
        val obs =
            fhirEngine.search<Observation> {
              filter(Observation.ENCOUNTER, { value = "Encounter/${it.logicalId}" })
            }
        obs.forEach { println("Observation Details :::: ${it.resource.value}") }

        // Case Details

        onset = generateResponse(obs, "c1-date-onset")
        clinicalSymptoms = generateResponse(obs, "f1")
        rashDate = generateResponse(obs, "f2")
        rashType = generateResponse(obs, "f3")
        patientVaccinated = generateResponse(obs, "c8a-vaccinated")
        patientDoses = generateResponse(obs, "c8a-no-of-doses")
        vaccineDateThirtyDays = generateResponse(obs, "c8b-recent-vaccine")
        lastDoseDate = generateResponse(obs, "c8b-date-of-vaccine")
        homeVisited = generateResponse(obs, "f4a")
        homeVisitedDate = generateResponse(obs, "f4b")
        epiLinked = generateResponse(obs, "f5")

        //        End of Case details
        residence = generateResponse(obs, "residence-setup")
        epid = generateResponse(obs, "EPID")
        county = generateResponse(obs, "a4-county")
        subCounty = generateResponse(obs, "a3-sub-county")
        facility = generateResponse(obs, "a1-health-facility")
        type = generateResponse(obs, "a2-type")
        disease = generateResponse(obs, "a5-disease-reported")

        /** Section C* */
        dateFirstSeen = generateResponse(obs, "c2-date-first-seen")
        dateSubCountyNotified = generateResponse(obs, "c3-date-notified")
        hospitalized = generateResponse(obs, "c4-hospitalized")
        admissionDate = generateResponse(obs, "c4-date-admission")
        ipNo = generateResponse(obs, "c5-ip-op-no")
        diagnosis = generateResponse(obs, "c6-diagnosis")
        diagnosisMeans = generateResponse(obs, "c7-means-of-diagnosis")
        diagnosisMeansOther = generateResponse(obs, "c7-other-specify")
        wasPatientVaccinated = generateResponse(obs, "c8a-vaccinated")
        noOfDoses = generateResponse(obs, "c8a-no-of-doses")
        twoMonthsVaccination = generateResponse(obs, "c8b-recent-vaccine")
        vaccineDate = generateResponse(obs, "c8b-date-of-vaccine")
        patientStatus = generateResponse(obs, "c9-patient-status")
      }

      parent =
          if (it.resource.hasContact())
              if (it.resource.contactFirstRep.hasName())
                  it.resource.contactFirstRep.name.givenAsSingleString
              else ""
          else ""

      houseno =
          if (it.resource.hasContact())
              if (it.resource.contactFirstRep.hasAddress())
                  it.resource.contactFirstRep.address.line[0].value.toString()
              else ""
          else ""
      neighbour =
          if (it.resource.hasContact())
              if (it.resource.contactFirstRep.hasAddress())
                  if (it.resource.contactFirstRep.address.line.size > 4) {
                    it.resource.contactFirstRep.address.line[1].value.toString()
                  } else {
                    ""
                  }
              else ""
          else ""
      street =
          if (it.resource.hasContact())
              if (it.resource.contactFirstRep.hasAddress())
                  if (it.resource.contactFirstRep.address.line.size > 2) {
                    it.resource.contactFirstRep.address.line[2].value.toString()
                  } else {
                    ""
                  }
              else ""
          else ""

      town =
          if (it.resource.hasContact())
              if (it.resource.contactFirstRep.hasAddress())
                  if (it.resource.contactFirstRep.address.line.size > 3) {
                    it.resource.contactFirstRep.address.line[3].value.toString()
                  } else {
                    ""
                  }
              else ""
          else ""
      subCountyName =
          if (it.resource.hasContact())
              if (it.resource.contactFirstRep.hasAddress())
                  if (it.resource.contactFirstRep.address.line.size > 4) {
                    it.resource.contactFirstRep.address.line[4].value.toString()
                  } else {
                    ""
                  }
              else ""
          else ""
      countyName =
          if (it.resource.hasContact())
              if (it.resource.contactFirstRep.hasAddress())
                  if (it.resource.contactFirstRep.address.line.size > 5) {
                    it.resource.contactFirstRep.address.line[5].value.toString()
                  } else {
                    ""
                  }
              else ""
          else ""

      parentPhone =
          if (it.resource.hasContact())
              if (it.resource.contactFirstRep.hasTelecom())
                  it.resource.contactFirstRep.telecomFirstRep.value.toString()
              else ""
          else ""
    }

    return PatientListViewModel.CaseDetailData(
        epid = epid,
        county = county,
        subCounty = subCounty,
        onset = onset,
        logicalId = logicalId,

        //      Case Information

        clinicalSymptoms = clinicalSymptoms,
        rashDate = rashDate,
        rashType = rashType,
        patientVaccinated = patientVaccinated,
        patientDoses = patientDoses,
        vaccineDateThirtyDays = vaccineDateThirtyDays,
        lastDoseDate = lastDoseDate,
        homeVisited = homeVisited,
        homeVisitedDate = homeVisitedDate,
        epiLinked = epiLinked,

        //      End of Case

        //      SECTION A
        facility = facility,
        disease = disease,
        type = type,
        // SECTION B

        name = name,
        sex = sex,
        dob = dob,
        residence = residence,
        parent = parent,
        houseNo = houseno,
        neighbour = neighbour,
        street = street,
        town = town,
        subCountyName = subCountyName,
        countyName = countyName,
        parentPhone = parentPhone,

        //      SECTION C

        dateFirstSeen = dateFirstSeen,
        dateSubCountyNotified = dateSubCountyNotified,
        hospitalized = hospitalized,
        admissionDate = admissionDate,
        ipNo = ipNo,
        diagnosis = diagnosis,
        diagnosisMeans = diagnosisMeans,
        diagnosisMeansOther = diagnosisMeansOther,
        targetDisease = disease,
        wasPatientVaccinated = wasPatientVaccinated,
        noOfDoses = noOfDoses,
        twoMonthsVaccination = twoMonthsVaccination,
        vaccineDate = vaccineDate,
        patientStatus = patientStatus)
  }

  private fun generateResponse(obs: List<SearchResult<Observation>>, s: String): String {

    return obs.firstOrNull { it.resource.code.codingFirstRep.code == s }
        ?.resource
        ?.value
        ?.asStringValue() ?: ""
  }

  fun getPatientInfo() {
    CoroutineScope(Dispatchers.IO).launch {
      val patientData = getPatientInfoCard()
      withContext(Dispatchers.Main) { livecaseData.value = patientData }
    }
  }

  fun getPatientDiseaseData(reason: String, parent: String, isCase: Boolean) {
    CoroutineScope(Dispatchers.IO).launch {
      if (isCase) {
        val patientData = getPatientDiseaseDataInformation(reason, parent)
        withContext(Dispatchers.Main) { liveDiseaseData.value = patientData }
      } else {
        val patientData = getPatientLabDataInformation(reason, parent)
        withContext(Dispatchers.Main) { liveLabData.value = patientData }
      }
    }
  }

  private suspend fun getPatientLabDataInformation(
      reason: String,
      parent: String
  ): List<PatientListViewModel.CaseLabResultsData> {
    val patients: MutableList<PatientListViewModel.CaseLabResultsData> = mutableListOf()
    fhirEngine
        .search<Encounter> {
          filter(Encounter.SUBJECT, { value = "Patient/$patientId" })
          filter(Encounter.PART_OF, { value = "Encounter/$parent" })
        }
        .mapIndexedNotNull() { index, data ->
          val code = data.resource.reasonCodeFirstRep.codingFirstRep.code
          if (code == reason) {
            var loop = createEncounterItemLabData(data.resource)

            val obs =
                fhirEngine.search<Observation> {
                  filter(Observation.ENCOUNTER, { value = "Encounter/${loop.logicalId}" })
                }

            val dateSpecimenReceived = generateResponse(obs, "date-specimen-received")
            val specimenCondition = generateResponse(obs, "specimen-condition")
            val measlesIgM = generateResponse(obs, "measles-igm")
            val rubellaIgM = generateResponse(obs, "rubella-igm")
            val dateLabSentResults = generateResponse(obs, "date-lab-sent-results")
            val finalClassification = generateResponse(obs, "final-classification")
            val subcountyName = generateResponse(obs, "contact-name")
            val subcountyDesignation = generateResponse(obs, "contact-designation")
            val subcountyPhone = generateResponse(obs, "contact-phone")
            val subcountyEmail = generateResponse(obs, "contact-email")
            val formCompletedBy = generateResponse(obs, "completer-name")
            val nameOfPersonCompletingForm = generateResponse(obs, "completer-name")
            val designation = generateResponse(obs, "completer-designation")
            val sign = generateResponse(obs, "completer-sign")

            loop =
                loop.copy(
                    dateSpecimenReceived = dateSpecimenReceived,
                    specimenCondition = specimenCondition,
                    measlesIgM = measlesIgM,
                    rubellaIgM = rubellaIgM,
                    dateLabSentResults = dateLabSentResults,
                    finalClassification = finalClassification,
                    subcountyName = subcountyName,
                    subcountyDesignation = subcountyDesignation,
                    subcountyPhone = subcountyPhone,
                    subcountyEmail = subcountyEmail,
                    formCompletedBy = formCompletedBy,
                    nameOfPersonCompletingForm = nameOfPersonCompletingForm,
                    designation = designation,
                    sign = sign)

            loop
          } else {
            null
          }
        }
        .let { patients.addAll(it) }

    return patients
  }

  private suspend fun getPatientDiseaseDataInformation(
      reason: String,
      parent: String
  ): List<PatientListViewModel.CaseDiseaseData> {
    val patients: MutableList<PatientListViewModel.CaseDiseaseData> = mutableListOf()
    fhirEngine
        .search<Encounter> {
          filter(Encounter.SUBJECT, { value = "Patient/$patientId" })
          filter(Encounter.PART_OF, { value = "Encounter/$parent" })
        }
        .mapIndexedNotNull() { index, data ->
          val code = data.resource.reasonCodeFirstRep.codingFirstRep.code
          if (code == reason) {
            var loop = createEncounterItemLab(data.resource)

            val obs =
                fhirEngine.search<Observation> {
                  filter(Observation.ENCOUNTER, { value = "Encounter/${loop.logicalId}" })
                }

            val fever = generateResponse(obs, "f1")
            val rash = generateResponse(obs, "f2")

            loop = loop.copy(fever = fever, rash = rash)

            loop
          } else {
            null
          }
        }
        .let { patients.addAll(it) }

    return patients
  }

  companion object {

    private fun createEncounterItemLabData(
        encounter: Encounter,
    ): PatientListViewModel.CaseLabResultsData {
      val reasonCode = encounter.reasonCodeFirstRep.codingFirstRep.code

      return PatientListViewModel.CaseLabResultsData(
          logicalId = encounter.logicalId,
          reasonCode = reasonCode,
      )
    }

    private fun createEncounterItemLab(
        encounter: Encounter,
    ): PatientListViewModel.CaseDiseaseData {
      val reasonCode = encounter.reasonCodeFirstRep.codingFirstRep.code

      return PatientListViewModel.CaseDiseaseData(
          logicalId = encounter.logicalId,
          name = reasonCode,
      )
    }

    private fun createEncounterItem(
        encounter: Encounter,
        resources: Resources,
    ): PatientListViewModel.EncounterItem {
      val reasonCode = encounter.reasonCodeFirstRep.codingFirstRep.code
      val status = encounter.status.display
      return PatientListViewModel.EncounterItem(
          id = encounter.logicalId,
          reasonCode = reasonCode,
          status = status,
      )
    }

    /**
     * Creates ObservationItem objects with displayable values from the Fhir Observation objects.
     */
    private fun createObservationItem(
        observation: Observation,
        resources: Resources,
    ): PatientListViewModel.ObservationItem {
      val observationCode = observation.code.text ?: observation.code.codingFirstRep.display

      // Show nothing if no values available for datetime and value quantity.
      val dateTimeString =
          if (observation.hasEffectiveDateTimeType()) {
            observation.effectiveDateTimeType.asStringValue()
          } else {
            resources.getText(R.string.message_no_datetime).toString()
          }
      val value =
          if (observation.hasValueQuantity()) {
            observation.valueQuantity.value.toString()
          } else if (observation.hasValueCodeableConcept()) {
            observation.valueCodeableConcept.coding.firstOrNull()?.display ?: ""
          } else {
            ""
          }
      val valueUnit =
          if (observation.hasValueQuantity()) {
            observation.valueQuantity.unit ?: observation.valueQuantity.code
          } else {
            ""
          }
      val valueString = "$value $valueUnit"

      return PatientListViewModel.ObservationItem(
          observation.logicalId,
          observationCode,
          dateTimeString,
          valueString,
      )
    }

    /** Creates ConditionItem objects with displayable values from the Fhir Condition objects. */
    private fun createConditionItem(
        condition: Condition,
        resources: Resources,
    ): PatientListViewModel.ConditionItem {
      val observationCode = condition.code.text ?: condition.code.codingFirstRep.display ?: ""

      // Show nothing if no values available for datetime and value quantity.
      val dateTimeString =
          if (condition.hasOnsetDateTimeType()) {
            condition.onsetDateTimeType.asStringValue()
          } else {
            resources.getText(R.string.message_no_datetime).toString()
          }
      val value =
          if (condition.hasVerificationStatus()) {
            condition.verificationStatus.codingFirstRep.code
          } else {
            ""
          }
      return PatientListViewModel.ConditionItem(
          condition.logicalId,
          observationCode,
          dateTimeString,
          value,
      )
    }
  }
}

interface PatientDetailData {
  val firstInGroup: Boolean
  val lastInGroup: Boolean
}

data class PatientDetailHeader(
    val header: String,
    override val firstInGroup: Boolean = false,
    override val lastInGroup: Boolean = false,
) : PatientDetailData

data class PatientDetailProperty(
    val patientProperty: PatientProperty,
    override val firstInGroup: Boolean = false,
    override val lastInGroup: Boolean = false,
) : PatientDetailData

data class PatientDetailOverview(
    val patient: PatientProperty,
    override val firstInGroup: Boolean = false,
    override val lastInGroup: Boolean = false,
) : PatientDetailData

data class PatientDetailObservation(
    val observation: PatientListViewModel.ObservationItem,
    override val firstInGroup: Boolean = false,
    override val lastInGroup: Boolean = false,
) : PatientDetailData

data class PatientDetailEncounter(
    val data: PatientListViewModel.EncounterItem,
    override val firstInGroup: Boolean = false,
    override val lastInGroup: Boolean = false,
) : PatientDetailData

data class PatientDetailCondition(
    val condition: PatientListViewModel.ConditionItem,
    override val firstInGroup: Boolean = false,
    override val lastInGroup: Boolean = false,
) : PatientDetailData

data class PatientProperty(val header: String, val value: String)

data class RiskAssessmentItem(
    var riskStatusColor: Int,
    var riskStatus: String,
    var lastContacted: String,
    var patientCardColor: Int,
)

/**
 * The logical (unqualified) part of the ID. For example, if the ID is
 * "http://example.com/fhir/Patient/123/_history/456", then this value would be "123".
 */
private val Resource.logicalId: String
  get() {
    return this.idElement?.idPart.orEmpty()
  }
