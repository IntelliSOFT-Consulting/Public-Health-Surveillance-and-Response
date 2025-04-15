package com.icl.surveillance.viewmodels

import android.app.Application
import android.content.res.Resources
import android.icu.text.DateFormat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.revInclude
import com.google.android.fhir.search.search
import com.icl.surveillance.R
import com.icl.surveillance.ui.patients.PatientListViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
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
  val livePatientData = MutableLiveData<List<PatientListViewModel.EncounterItem>>()

  /** Emits list of [PatientDetailData]. */
  fun getPatientDetailData(category: String, parent: String?) {
    viewModelScope.launch { livePatientData.value = getPatientDetailDataModel(category, parent) }
  }

  private suspend fun getPatientDetailDataModel(
      category: String,
      parent: String?
  ): List<PatientListViewModel.EncounterItem> {
    val searchResult =
        fhirEngine.search<Patient> {
          filter(Resource.RES_ID, { value = of(patientId) })
          revInclude<Observation>(Observation.SUBJECT)
          revInclude<Condition>(Condition.SUBJECT)
          revInclude<Encounter>(Encounter.SUBJECT)
        }
    val data = mutableListOf<PatientListViewModel.EncounterItem>()

    searchResult.first().let {
      it.revIncluded?.get(ResourceType.Encounter to Encounter.SUBJECT.paramName)?.let {
        data.addEncounterData(it as List<Encounter>, category, parent)
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

  companion object {

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
    val patient: PatientListViewModel.PatientItem,
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
