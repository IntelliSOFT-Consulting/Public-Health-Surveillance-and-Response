package com.icl.surveillance.ui.patients

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.asStringValue
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient

class PatientListViewModel(application: Application, private val fhirEngine: FhirEngine) :
    AndroidViewModel(application) {
  val liveSearchedPatients = MutableLiveData<List<PatientItem>>()
  val patientCount = MutableLiveData<Long>()

  init {
    updatePatientListAndPatientCount({ getSearchResults() }, { searchedPatientCount() })
  }

  fun searchPatientsByName(nameQuery: String) {
    updatePatientListAndPatientCount({ getSearchResults(nameQuery) }, { count(nameQuery) })
  }

  /**
   * [updatePatientListAndPatientCount] calls the search and count lambda and updates the live data
   * values accordingly. It is initially called when this [ViewModel] is created. Later its called
   * by the client every time search query changes or data-sync is completed.
   */
  private fun updatePatientListAndPatientCount(
      search: suspend () -> List<PatientItem>,
      count: suspend () -> Long,
  ) {
    viewModelScope.launch {
      liveSearchedPatients.value = search()
      patientCount.value = count()
    }
  }

  /**
   * Returns count of all the [Patient] who match the filter criteria unlike [getSearchResults]
   * which only returns a fixed range.
   */
  private suspend fun count(nameQuery: String = ""): Long {
    return fhirEngine.count<Patient> {
      if (nameQuery.isNotEmpty()) {
        filter(
            Patient.NAME,
            {
              modifier = StringFilterModifier.CONTAINS
              value = nameQuery
            },
        )
      }
    }
  }

  private suspend fun getSearchResults(nameQuery: String = ""): List<PatientItem> {
    val patients: MutableList<PatientItem> = mutableListOf()
    fhirEngine
        .search<Patient> {
          if (nameQuery.isNotEmpty()) {
            filter(
                Patient.NAME,
                {
                  modifier = StringFilterModifier.CONTAINS
                  value = nameQuery
                },
            )
          }
          sort(Patient.GIVEN, Order.ASCENDING)
          count = 100
          from = 0
        }
        .mapIndexed { index, fhirPatient ->
          var item = fhirPatient.resource.toPatientItem(index + 1)
          try {

            val encounter = loadEncounter(item.resourceId)
            val caseInfoEncounter =
                encounter.firstOrNull {
                  it.reasonCodeFirstRep.codingFirstRep.code == "Case Information"
                }

            caseInfoEncounter?.let {
              println("Found Encounter: ${it.id}")
              // pull all Obs for this Encounter
              val obs =
                  fhirEngine.search<Observation> {
                    filter(Observation.ENCOUNTER, { value = "Encounter/${it.logicalId}" })
                  }
              val epid =
                  obs.firstOrNull { it.resource.code.codingFirstRep.code == "EPID" }
                      ?.resource
                      ?.value
                      ?.asStringValue() ?: ""
              val county =
                  obs.firstOrNull { it.resource.code.codingFirstRep.code == "a4-county" }
                      ?.resource
                      ?.value
                      ?.asStringValue() ?: ""
              val subCounty =
                  obs.firstOrNull { it.resource.code.codingFirstRep.code == "a3-sub-county" }
                      ?.resource
                      ?.value
                      ?.asStringValue() ?: ""
              val onset =
                  obs.firstOrNull { it.resource.code.codingFirstRep.code == "c1-date-onset" }
                      ?.resource
                      ?.value
                      ?.asStringValue() ?: ""

              item =
                  item.copy(
                      epid = epid, subCounty = subCounty, county = county, caseOnsetDate = onset)
            } ?: println("No Encounter found with reasonCode 'Case Information'")
          } catch (e: Exception) {
            e.printStackTrace()
          }
          //              item.copy()
          item
        }
        .let { patients.addAll(it) }

    return patients
  }

  /** The Patient's details for display purposes. */
  data class PatientItem(
      val id: String,
      val resourceId: String,
      val name: String,
      val gender: String,
      val dob: LocalDate? = null,
      val phone: String,
      val city: String,
      val country: String,
      val isActive: Boolean,
      val epid: String,
      val county: String,
      val subCounty: String,
      val caseOnsetDate: String,
  ) {
    override fun toString(): String = name
  }

  /** The Observation's details for display purposes. */
  data class ObservationItem(
      val id: String,
      val code: String,
      val effective: String,
      val value: String,
  ) {
    override fun toString(): String = code
  }

  interface PatientDetailData {
    val firstInGroup: Boolean
    val lastInGroup: Boolean
  }

  data class CaseDetailData(
      val logicalId: String,
      val name: String,
      val sex: String,
      val dob: String,
      val epid: String,
      val county: String,
      val subCounty: String,
      val onset: String,
      val residence: String,
      val facility: String,
      val type: String,
      val disease: String,
      val dateFirstSeen: String,
      val dateSubCountyNotified: String,
      val hospitalized: String,
      val admissionDate: String,
      val ipNo: String,
      val diagnosis: String,
      val diagnosisMeans: String,
      val diagnosisMeansOther: String,
      val targetDisease: String,
      val wasPatientVaccinated: String,
      val noOfDoses: String,
      val twoMonthsVaccination: String,
      val patientStatus: String,
      val vaccineDate: String
  )

  data class PatientDetailOverview(
      val patient: PatientListViewModel.PatientItem,
      override val firstInGroup: Boolean = false,
      override val lastInGroup: Boolean = false,
  ) : PatientDetailData

  data class EncounterItem(
      val id: String,
      val reasonCode: String,
      val status: String,
  ) {
    override fun toString(): String = reasonCode
  }

  data class ConditionItem(
      val id: String,
      val code: String,
      val effective: String,
      val value: String,
  ) {
    override fun toString(): String = code
  }

  class PatientListViewModelFactory(
      private val application: Application,
      private val fhirEngine: FhirEngine,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(PatientListViewModel::class.java)) {
        return PatientListViewModel(application, fhirEngine) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class")
    }
  }

  private var patientGivenName: String? = null
  private var patientFamilyName: String? = null

  fun setPatientGivenName(givenName: String) {
    patientGivenName = givenName
    searchPatientsByParameter()
  }

  fun setPatientFamilyName(familyName: String) {
    patientFamilyName = familyName
    searchPatientsByParameter()
  }

  private fun searchPatientsByParameter() {
    viewModelScope.launch {
      liveSearchedPatients.value = searchPatients()
      patientCount.value = searchedPatientCount()
    }
  }

  private suspend fun searchPatients(): List<PatientItem> {
    val patients =
        fhirEngine
            .search<Patient> {
              filter(
                  Patient.GIVEN,
                  {
                    modifier = StringFilterModifier.CONTAINS
                    this.value = patientGivenName ?: ""
                  },
              )
              filter(
                  Patient.FAMILY,
                  {
                    modifier = StringFilterModifier.CONTAINS
                    this.value = patientFamilyName ?: ""
                  },
              )
              sort(Patient.GIVEN, Order.ASCENDING)
              count = 100
              from = 0
            }
            .mapIndexed { index, fhirPatient ->
              val item = fhirPatient.resource.toPatientItem(index + 1)
              try {

                val encounter = loadEncounter(item.resourceId)
                encounter.forEach {
                  println(
                      "Printing Encounter Details: it.resource.reasonCode: ${it.reasonCodeFirstRep.codingFirstRep.code}")
                }
              } catch (e: Exception) {
                e.printStackTrace()
              }
              //              item.copy()
              item
            }
            .toMutableList()

    return patients
  }

  private suspend fun loadEncounter(patientId: String): List<Encounter> {
    return fhirEngine
        .search<Encounter> { filter(Encounter.SUBJECT, { value = "Patient/$patientId" }) }
        .map { it.resource }
  }

  private suspend fun searchedPatientCount(): Long {
    return fhirEngine.count<Patient> {
      filter(
          Patient.GIVEN,
          {
            modifier = StringFilterModifier.CONTAINS
            this.value = patientGivenName ?: ""
          },
      )
      filter(
          Patient.FAMILY,
          {
            modifier = StringFilterModifier.CONTAINS
            this.value = patientFamilyName ?: ""
          },
      )
    }
  }
}

internal fun Patient.toPatientItem(position: Int): PatientListViewModel.PatientItem {
  // Show nothing if no values available for gender and date of birth.
  val patientId = if (hasIdElement()) idElement.idPart else ""
  val name = if (hasName()) name[0].nameAsSingleString else ""
  val gender = if (hasGenderElement()) genderElement.valueAsString else ""
  val dob =
      if (hasBirthDateElement()) {
        birthDateElement.value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
      } else {
        null
      }
  val phone = if (hasTelecom()) telecom[0].value else ""
  val city = if (hasAddress()) address[0].city else ""
  val country = if (hasAddress()) address[0].country else ""
  val isActive = active
  var epid = ""
  var county = ""
  var subCounty = ""
  var caseOnsetDate = ""

  return PatientListViewModel.PatientItem(
      id = position.toString(),
      resourceId = patientId,
      name = " $name",
      gender = gender ?: "",
      dob = dob,
      phone = phone ?: "",
      city = city ?: "",
      country = country ?: "",
      isActive = isActive,
      epid = epid,
      county = county,
      subCounty = subCounty,
      caseOnsetDate = caseOnsetDate,
  )
}
