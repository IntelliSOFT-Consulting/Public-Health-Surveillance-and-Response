package com.icl.surveillance.ui.patients

import android.app.Application
import android.content.Context
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
import com.icl.surveillance.utils.FormatterClass
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient

class PatientListViewModel(
    application: Application,
    private val fhirEngine: FhirEngine
) :
    AndroidViewModel(application) {
    val liveSearchedPatients = MutableLiveData<List<PatientItem>>()
    val liveSearchedCases = MutableLiveData<List<PatientItem>>()
    val patientCount = MutableLiveData<Long>()


    init {
        updatePatientListAndPatientCount(
            { getSearchResults() },
            { searchedPatientCount() })
    }

    fun searchPatientsByName(nameQuery: String) {
        updatePatientListAndPatientCount(
            { getSearchResults(nameQuery) },
            { count(nameQuery) })
    }

    fun handleCurrentCaseListing(category: String) {
        viewModelScope.launch {
            liveSearchedCases.value = retrieveCasesByDisease(category)
//            patientCount.value = count()
        }
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

    private suspend fun getSearchResults(
        nameQuery: String = "",
    ): List<PatientItem> {

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
                        println("Found : None Results here ${it.logicalId}")
                        // Load Child Encounter Here

                        val childEncounter = loadChildEncounter(item.resourceId, it.logicalId)
                        val childCaseInfoEncounter =
                            childEncounter.firstOrNull {
                                it.reasonCodeFirstRep.codingFirstRep.code == "Measles Lab Information"
                            }

                        childCaseInfoEncounter?.let { kk ->
                            val obs1 =
                                fhirEngine.search<Observation> {
                                    filter(
                                        Observation.ENCOUNTER,
                                        { value = "Encounter/${kk.logicalId}" })
                                }

                            val measlesIgm =
                                obs1.firstOrNull { it.resource.code.codingFirstRep.code == "measles-igm" }
                                    ?.resource
                                    ?.value
                                    ?.asStringValue() ?: ""

//                            val finalClassification =
//                                obs1.firstOrNull {
//                                    it.resource.code.codingFirstRep.code == "final-classification"
//                                }
//                                    ?.resource
//                                    ?.value
//                                    ?.asStringValue() ?: ""
//
//                            val finalClassification =
//                                obs1.firstOrNull {
//                                    it.resource.code.codingFirstRep.code == "final-confirm-classification"
//                                }
//                                    ?.resource
//                                    ?.value
//                                    ?.asStringValue() ?: ""
//
//                            val finalClassification =
//                                obs1.firstOrNull {
//                                    it.resource.code.codingFirstRep.code == "final-negative-classification"
//                                }
//                                    ?.resource
//                                    ?.value
//                                    ?.asStringValue() ?: ""

                            val finalClassification = when (measlesIgm.lowercase()) {
                                "positive" -> obs1.firstOrNull {
                                    it.resource.code.codingFirstRep.code == "final-confirm-classification"
                                }?.resource?.value?.asStringValue() ?: ""

                                "negative" -> obs1.firstOrNull {
                                    it.resource.code.codingFirstRep.code == "final-negative-classification"
                                }?.resource?.value?.asStringValue() ?: ""

                                else -> obs1.firstOrNull {
                                    it.resource.code.codingFirstRep.code == "final-classification"
                                }?.resource?.value?.asStringValue() ?: ""
                            }

                            println("Found Child Encounter: ${it.id}")

                            item = item.copy(labResults = measlesIgm, status = finalClassification)
                        }

                        // pull all Obs for this Encounter
                        val obs =
                            fhirEngine.search<Observation> {
                                filter(
                                    Observation.ENCOUNTER,
                                    { value = "Encounter/${it.logicalId}" })
                            }

                        val epid =
                            obs.firstOrNull { it.resource.code.codingFirstRep.code == "EPID" }
                                ?.resource
                                ?.value
                                ?.asStringValue() ?: "still loading"
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
                            obs.firstOrNull { it.resource.code.codingFirstRep.code == "728034137219" }
                                ?.resource
                                ?.value
                                ?.asStringValue() ?: ""

                        item =
                            item.copy(
                                encounterId = it.logicalId,
                                epid = epid,
                                subCounty = subCounty,
                                county = county,
                                caseOnsetDate = onset
                            )
                    }

                    println("Found : None for Now")

                } catch (e: Exception) {
                    e.printStackTrace()

                    println("Error Loading Page : ${e.message}")
                }
                item
            }
            .let {
                val sortedCases = it.sortedByDescending { q -> q.lastUpdated }

                patients.addAll(sortedCases)
            }

        return patients
    }

    private suspend fun retrieveCasesByDisease(
        nameQuery: String,
    ): List<PatientItem> {

        println("Started searching for cases *** $nameQuery")

        return fhirEngine
            .search<Patient> {
                sort(Patient.GIVEN, Order.ASCENDING)
                count = 100
                from = 0
            }
            .mapIndexedNotNull { index, fhirPatient ->
                // Only return the patient if one of the identifiers matches the system
                val matchingIdentifier = fhirPatient.resource.identifier.find {
                    it.system == nameQuery
                }

                if (matchingIdentifier != null) {
                    // Convert the FHIR Patient resource to your PatientItem model
                    var data = fhirPatient.resource.toPatientItem(index + 1)
                    val logicalId = matchingIdentifier.value
                    val obs =
                        fhirEngine.search<Observation> {
                            filter(
                                Observation.ENCOUNTER,
                                { value = "Encounter/${logicalId}" })
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
                        obs.firstOrNull { it.resource.code.codingFirstRep.code == "728034137219" }
                            ?.resource
                            ?.value
                            ?.asStringValue() ?: ""

                    // Loading Lab Results
                    val childEncounter = loadChildEncounter(data.resourceId, logicalId)

                    val childCaseInfoEncounter =
                        childEncounter.firstOrNull {
                            it.reasonCodeFirstRep.codingFirstRep.code == "Measles Lab Information"
                        }
                    var measlesIgm = "Pending"
                    var finalClassification = "Pending Results"
                    childCaseInfoEncounter?.let { kk ->
                        val obs1 =
                            fhirEngine.search<Observation> {
                                filter(
                                    Observation.ENCOUNTER,
                                    { value = "Encounter/${kk.logicalId}" })
                            }

                        measlesIgm =
                            obs1.firstOrNull { it.resource.code.codingFirstRep.code == "measles-igm" }
                                ?.resource
                                ?.value
                                ?.asStringValue() ?: ""


                        finalClassification = when (measlesIgm.lowercase()) {
                            "positive" -> obs1.firstOrNull {
                                it.resource.code.codingFirstRep.code == "final-confirm-classification"
                            }?.resource?.value?.asStringValue() ?: ""

                            "negative" -> obs1.firstOrNull {
                                it.resource.code.codingFirstRep.code == "final-negative-classification"
                            }?.resource?.value?.asStringValue() ?: ""

                            else -> obs1.firstOrNull {
                                it.resource.code.codingFirstRep.code == "final-classification"
                            }?.resource?.value?.asStringValue() ?: ""
                        }
                    }
                    data =
                        data.copy(
                            encounterId = logicalId,
                            epid = epid, labResults = measlesIgm, status = finalClassification,
                            county = county, subCounty = subCounty, caseOnsetDate = onset
                        )
                    data
                } else {
                    null // Not a match — exclude
                }
            }
            .sortedByDescending { it.lastUpdated }
    }

//
//    private suspend fun retrieveCasesByDisease(
//        nameQuery: String,
//    ): List<PatientItem> {
//
//        println("Started searching for cases *** $nameQuery")
//        val patients: MutableList<PatientItem> = mutableListOf()
//        fhirEngine
//            .search<Patient> {
////                filter(
////                    Patient.IDENTIFIER,
////                    {
////                        value = of(Coding().apply {
////                            system = nameQuery
////                            code = nameQuery
////                        })
////                    })
////                filter(
////                    Patient.IDENTIFIER,
////                    { value = of("$nameQuery|$nameQuery") }
////                )
//                sort(Patient.GIVEN, Order.ASCENDING)
//                count = 100
//                from = 0
//            }
//            .mapIndexedNotNull { index, fhirPatient ->
//
//                val patient = fhirPatient.resource.identifier.find { it.system == nameQuery }
//                if (patient != null) {
//                    var item = fhirPatient.resource.toPatientItem(index + 1)
//
//
////                try {
////
////                    val encounter = loadEncounter(item.resourceId)
////                    val caseInfoEncounter =
////                        encounter.firstOrNull {
////                            it.reasonCodeFirstRep.codingFirstRep.code == "Measles Case Information"
////                        }
////
////                    caseInfoEncounter?.let {
////
////                        val childEncounter = loadChildEncounter(item.resourceId, it.logicalId)
////                        val childCaseInfoEncounter =
////                            childEncounter.firstOrNull {
////                                it.reasonCodeFirstRep.codingFirstRep.code == "Measles Lab Information"
////                            }
////
////                        childCaseInfoEncounter?.let { kk ->
////                            val obs1 =
////                                fhirEngine.search<Observation> {
////                                    filter(
////                                        Observation.ENCOUNTER,
////                                        { value = "Encounter/${kk.logicalId}" })
////                                }
////
////                            val measlesIgm =
////                                obs1.firstOrNull { it.resource.code.codingFirstRep.code == "measles-igm" }
////                                    ?.resource
////                                    ?.value
////                                    ?.asStringValue() ?: ""
////
////
////                            val finalClassification = when (measlesIgm.lowercase()) {
////                                "positive" -> obs1.firstOrNull {
////                                    it.resource.code.codingFirstRep.code == "final-confirm-classification"
////                                }?.resource?.value?.asStringValue() ?: ""
////
////                                "negative" -> obs1.firstOrNull {
////                                    it.resource.code.codingFirstRep.code == "final-negative-classification"
////                                }?.resource?.value?.asStringValue() ?: ""
////
////                                else -> obs1.firstOrNull {
////                                    it.resource.code.codingFirstRep.code == "final-classification"
////                                }?.resource?.value?.asStringValue() ?: ""
////                            }
////
////                            println("Found Child Encounter: ${it.id}")
////
////                            item = item.copy(labResults = measlesIgm, status = finalClassification)
////                        }
////
////                        // pull all Obs for this Encounter
////                        val obs =
////                            fhirEngine.search<Observation> {
////                                filter(
////                                    Observation.ENCOUNTER,
////                                    { value = "Encounter/${it.logicalId}" })
////                            }
////
////                        val epid =
////                            obs.firstOrNull { it.resource.code.codingFirstRep.code == "EPID" }
////                                ?.resource
////                                ?.value
////                                ?.asStringValue() ?: "still loading"
////                        val county =
////                            obs.firstOrNull { it.resource.code.codingFirstRep.code == "a4-county" }
////                                ?.resource
////                                ?.value
////                                ?.asStringValue() ?: ""
////                        val subCounty =
////                            obs.firstOrNull { it.resource.code.codingFirstRep.code == "a3-sub-county" }
////                                ?.resource
////                                ?.value
////                                ?.asStringValue() ?: ""
////                        val onset =
////                            obs.firstOrNull { it.resource.code.codingFirstRep.code == "728034137219" }
////                                ?.resource
////                                ?.value
////                                ?.asStringValue() ?: ""
////
////                        item =
////                            item.copy(
////                                encounterId = it.logicalId,
////                                epid = epid,
////                                subCounty = subCounty,
////                                county = county,
////                                caseOnsetDate = onset
////                            )
////                    }
////
////
////                } catch (e: Exception) {
////                    e.printStackTrace()
////
////                    println("Error Loading Page : ${e.message}")
////                }
//                    item
//                }
//            }
//            .let {
//                val sortedCases = it.sortedByDescending { q -> q.lastUpdated }
//
//                patients.addAll(sortedCases)
//            }
//
//        return patients
//    }

    /** The Patient's details for display purposes. */
    data class PatientItem(
        val id: String,
        val resourceId: String,
        val encounterId: String,
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
        val status: String = "Pending Results",
        val labResults: String = "Pending",
        val lastUpdated: String
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

    data class CaseDiseaseData(
        val logicalId: String,
        val name: String,
        val fever: String = "",
        val rash: String = ""
    )

    data class CaseLabResultsData(
        val logicalId: String,
        val reasonCode: String,
        val dateSpecimenReceived: String = "",
        val specimenCondition: String = "",
        val measlesIgM: String = "",
        val rubellaIgM: String = "",
        val dateLabSentResults: String = "",
        val finalClassification: String = "",
        val subcountyName: String = "",
        val subcountyDesignation: String = "",
        val subcountyPhone: String = "",
        val subcountyEmail: String = "",
        val formCompletedBy: String = "",
        val nameOfPersonCompletingForm: String = "",
        val designation: String = "",
        val sign: String = ""
    )

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
        val parent: String,
        val houseNo: String,
        val neighbour: String,
        val street: String,
        val town: String,
        val subCountyName: String,
        val countyName: String,
        val parentPhone: String,
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
        val vaccineDate: String,
        // Case Details
        val clinicalSymptoms: String,
        val rashDate: String,
        val rashType: String,
        val patientVaccinated: String,
        val patientDoses: String,
        val vaccineDateThirtyDays: String,
        val lastDoseDate: String,
        val homeVisited: String,
        val homeVisitedDate: String,
        val epiLinked: String,

        // Clinical

        val patientOutcome: String,
        val sampleCollected: String,
        val inPatientOutPatient: String,

        //    Lab Information
        val specimen: String,
        val noWhy: String,
        val collectionDate: String,
        val specimenType: String,
        val specimenTypeOther: String,
        val dateSent: String,
        val labName: String,
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
                                "Printing Encounter Details: it.resource.reasonCode: ${it.reasonCodeFirstRep.codingFirstRep.code}"
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        println("Error Loading Patient data ${e.message}")
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

    private suspend fun loadChildEncounter(
        patientId: String,
        encounterId: String
    ): List<Encounter> {
        return fhirEngine
            .search<Encounter> {
                filter(Encounter.SUBJECT, { value = "Patient/$patientId" })
                filter(Encounter.PART_OF, { value = "Encounter/$encounterId" })
            }
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

internal fun Patient.toPatientItem(
    position: Int,
): PatientListViewModel.PatientItem {
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

    var lastUpdated = ""
    if (hasIdentifier()) {
        val id = identifier.find { it.system == "system-creation" }
        if (id != null) {
            lastUpdated = id.value
        }
    } else {
        lastUpdated = ""
    }

    val encounterId =

        return PatientListViewModel.PatientItem(
            id = position.toString(),
            encounterId = "encounterId",
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
            lastUpdated = lastUpdated
        )
}