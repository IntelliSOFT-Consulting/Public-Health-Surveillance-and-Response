package com.icl.surveillance.fhir

import android.content.Context
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.search
import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.SyncDataParams
import com.google.android.fhir.sync.download.DownloadRequest
import com.icl.surveillance.models.LocationLevel
import com.icl.surveillance.models.UserRole
import com.icl.surveillance.utils.FormatterClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import kotlin.collections.emptyList

class TimestampBasedDownloadWorkManagerImpl(
    private val dataStore: DemoDataStore, val context: Context, val fhirEngine: FhirEngine
) : DownloadWorkManager {
    private val resourceTypeList = ResourceType.values().map { it.name }
    private var urls: LinkedList<String> = LinkedList()
    private val locationAndOrganizationUrls = LinkedList(listOf("Location", "Organization"))

    init {
        getRespectiveFilteredResources(context) { filteredUrls ->
            urls = LinkedList<String>().apply {
//                addAll(locationAndOrganizationUrls)
                addAll(filteredUrls)
            }
            // ✅ urls is now ready for use
            println("Filtered resources: $urls")
        }
    }

    override suspend fun getNextRequest(): DownloadRequest? {
        var url = urls.poll() ?: return null

        val resourceTypeToDownload =
            ResourceType.fromCode(url.findAnyOf(resourceTypeList, ignoreCase = true)!!.second)
        dataStore.getLastUpdateTimestamp(resourceTypeToDownload)?.let {
            url = affixLastUpdatedTimestamp(url, it)
        }
        return DownloadRequest.of(url)
    }

    override suspend fun getSummaryRequestUrls(): Map<ResourceType, String> {
        return urls.associate { url ->
            val resourceType = ResourceType.fromCode(url.substringBefore("?"))
            if (resourceType == ResourceType.Patient) {
                resourceType to url.plus("&${SyncDataParams.SUMMARY_KEY}=${SyncDataParams.SUMMARY_COUNT_VALUE}")
            } else {
                resourceType to url
            }
        }
    }

    override suspend fun processResponse(response: Resource): Collection<Resource> {
        // As per FHIR documentation :
        // If the search fails (cannot be executed, not that there are no matches), the
        // return value SHALL be a status code 4xx or 5xx with an OperationOutcome.
        // See https://www.hl7.org/fhir/http.html#search for more details.
        if (response is OperationOutcome) {
            throw FHIRException(response.issueFirstRep.diagnostics)
        }

        // If the resource returned is a List containing Patients, extract Patient references and fetch
        // all resources related to the patient using the $everything operation.
        if (response is ListResource) {

            for (entry in response.entry) {

                val reference = Reference(entry.item.reference)
                if (reference.referenceElement.resourceType.equals("Patient")) {
                    val patientUrl = "${entry.item.reference}/\$everything"
                    urls.add(patientUrl)
                }

            }
        }

        // If the resource returned is a Bundle, check to see if there is a "next" relation referenced
        // in the Bundle.link component, if so, append the URL referenced to list of URLs to download.
        if (response is Bundle) {
            for (entry in response.entry) {
                val type = entry.resource.resourceType.toString()
                if (type == "Patient") {
                    val patientUrl = "${entry.fullUrl}/\$everything"
                    urls.add(patientUrl)
                }
            }

            val nextUrl =
                response.link.firstOrNull { component -> component.relation == "next" }?.url
            if (nextUrl != null) {
                urls.add(nextUrl)
            }
        }

        // Finally, extract the downloaded resources from the bundle.
        var bundleCollection: Collection<Resource> = mutableListOf()
        if (response is Bundle && response.type == Bundle.BundleType.SEARCHSET) {
            bundleCollection = response.entry.map { it.resource }
                .also { extractAndSaveLastUpdateTimestampToFetchFutureUpdates(it) }
        }
        return bundleCollection
    }

    private suspend fun extractAndSaveLastUpdateTimestampToFetchFutureUpdates(
        resources: List<Resource>,
    ) {
        resources.groupBy { it.resourceType }.entries.map { map ->
            dataStore.saveLastUpdatedTimestamp(
                map.key,
                map.value.maxOfOrNull { it.meta.lastUpdated }?.toTimeZoneString() ?: "",
            )
        }
    }

    fun getFacilitiesByLevel(
        startId: String,
        level: LocationLevel,
        onResult: (List<String>) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val facilityIds = mutableListOf<String>()
                val locationIdsToProcess = mutableListOf(startId)

                when (level) {
                    LocationLevel.FACILITY -> {
                        // Already at facility level
                        facilityIds.add(startId)
                    }

                    LocationLevel.WARD -> {
                        // One-level deep
                        for (wardId in locationIdsToProcess) {
                            val facilities = fhirEngine.search<Location> {
                                filter(Location.PARTOF, { value = "Location/$wardId" })
                            }
                            facilityIds.addAll(facilities.map { it.resource.logicalId })
                        }
                    }

                    LocationLevel.SUB_COUNTY -> {
                        // Get wards under sub-county
                        val wards = fhirEngine.search<Location> {
                            filter(Location.PARTOF, { value = "Location/$startId" })
                        }
                        val wardIds = wards.map { it.resource.logicalId }

                        // Get facilities under each ward
                        for (wardId in wardIds) {
                            val facilities = fhirEngine.search<Location> {
                                filter(Location.PARTOF, { value = "Location/$wardId" })
                            }
                            facilityIds.addAll(facilities.map { it.resource.logicalId })
                        }
                    }

                    LocationLevel.COUNTY -> {
                        // Get sub-counties under county
                        val subCounties = fhirEngine.search<Location> {
                            filter(Location.PARTOF, { value = "Location/$startId" })
                        }

                        val subCountyIds = subCounties.map { it.resource.logicalId }

                        for (subCountyId in subCountyIds) {
                            // Get wards under sub-county
                            val wards = fhirEngine.search<Location> {
                                filter(Location.PARTOF, { value = "Location/$subCountyId" })
                            }

                            val wardIds = wards.map { it.resource.logicalId }

                            // Get facilities under each ward
                            for (wardId in wardIds) {
                                val facilities = fhirEngine.search<Location> {
                                    filter(Location.PARTOF, { value = "Location/$wardId" })
                                }
                                facilityIds.addAll(facilities.map { it.resource.logicalId })
                            }
                        }
                    }

                    LocationLevel.NATIONAL -> {
                        // Get all counties first
                        val counties = fhirEngine.search<Location> {
                            // Optional: Add filter by type = "county" if available
                        }

                        val countyIds = counties.map { it.resource.logicalId }

                        for (countyId in countyIds) {
                            // Recursive call for each county
                            getFacilitiesByLevel(countyId, LocationLevel.COUNTY) { ids ->
                                facilityIds.addAll(ids)
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult(facilityIds)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(emptyList())
                }
            }
        }
    }


    fun getRespectiveFilteredResources(
        context: Context, onResult: (LinkedList<String>) -> Unit
    ) {
        val formatter = FormatterClass()
        val storedRole = formatter.getSharedPref("practitionerRole", context)
        val userRole = UserRole.fromAny(storedRole ?: "")

        println("Current User Role:::: $userRole")

        when (userRole) {
            UserRole.FACILITY_SURVEILLANCE_FOCAL_PERSON, UserRole.SUPERVISOR, UserRole.VACCINATOR -> {
                val facility = formatter.getSharedPref("facility", context)
                val urls = if (facility != null) {
                    listOf(
                        "Patient?organization=Organization/$facility&_sort=_lastUpdated",
                        "AllergyIntolerance",
                        "Observation?_count=1000",
                        "Encounter?_count=1000"
                    )
                } else emptyList()

                onResult(LinkedList(urls))
            }

            UserRole.SUBCOUNTY_DISEASE_SURVEILLANCE_OFFICER -> {
                val subCounty = formatter.getSharedPref("subCounty", context)
                if (subCounty != null) {
                    getFacilitiesByLevel(subCounty, LocationLevel.SUB_COUNTY) { facilities ->
                        val patientQueries = facilities.map { facilityId ->
                            "Patient?organization=Organization/$facilityId&_sort=_lastUpdated"
                        }

                        val extraResources = listOf(
                            "Patient?_sort=_lastUpdated",
                            "Encounter?_count=1000",
                            "MeasureReport?_count=1000",
                            "QuestionnaireResponse?_count=1000"
                        )

                        val combinedResources = LinkedList(patientQueries + extraResources)
                        onResult(combinedResources)
                    }
                } else {
                    onResult(LinkedList()) // subCounty was null
                }
            }

            UserRole.COUNTY_DISEASE_SURVEILLANCE_OFFICER -> {
                val urls = listOf(
                    "MeasureReport?_count=1000",
                    "QuestionnaireResponse?_count=1000",
                    "Specimen?_count=1000"
                )
                onResult(LinkedList(urls))
            }

            null -> {
                onResult(LinkedList()) // unknown role
            }

            UserRole.ADMINISTRATOR -> {

            }

            UserRole.SUPERUSER -> {


            }
        }
    }


    /**
     * Affixes the last updated timestamp to the request URL.
     *
     * If the request URL includes the `$everything` parameter, the last updated timestamp will be
     * attached using the `_since` parameter. Otherwise, the last updated timestamp will be attached
     * using the `_lastUpdated` parameter.
     */
    private fun affixLastUpdatedTimestamp(url: String, lastUpdated: String): String {
        var downloadUrl = url

        // Affix lastUpdate to a $everything query using _since as per:
        // https://hl7.org/fhir/operation-patient-everything.html
        if (downloadUrl.contains("\$everything")) {
            downloadUrl = "$downloadUrl?_since=$lastUpdated"
        }
        if (!downloadUrl.contains("\$everything")) {
            downloadUrl = if (downloadUrl.contains("?_count=")) {
                url
            } else if (downloadUrl.contains("&_lastUpdated")) {
                url
            } else if (downloadUrl.contains("sort")) {
                "$downloadUrl&_lastUpdated=gt$lastUpdated"
            } else {
                "$downloadUrl?_lastUpdated=gt$lastUpdated"
            }
        }

        // Do not modify any URL set by a server that specifies the token of the page to return.
        if (downloadUrl.contains("&page_token")) {
            downloadUrl = url
        }
        return downloadUrl
    }


    private fun Date.toTimeZoneString(): String {
        val simpleDateFormat =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
        return simpleDateFormat.format(this.toInstant())
    }
}
