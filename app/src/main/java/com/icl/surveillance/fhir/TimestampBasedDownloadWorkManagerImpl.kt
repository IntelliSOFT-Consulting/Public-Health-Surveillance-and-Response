package com.icl.surveillance.fhir

import android.content.Context
import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.SyncDataParams
import com.google.android.fhir.sync.download.DownloadRequest
import com.icl.surveillance.models.UserRole
import com.icl.surveillance.utils.FormatterClass
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.LinkedList
import java.util.Locale
import org.hl7.fhir.exceptions.FHIRException
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.ListResource
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType

class TimestampBasedDownloadWorkManagerImpl(
    private val dataStore: DemoDataStore,
    val context: Context
) :
    DownloadWorkManager {
    private val resourceTypeList = ResourceType.values().map { it.name }
    private val urls = getRespectiveFilteredResources(context)


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
                resourceType to
                        url.plus("&${SyncDataParams.SUMMARY_KEY}=${SyncDataParams.SUMMARY_COUNT_VALUE}")
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
//                if (type == "Encounter") {
//                    val patientUrl = "${entry.fullUrl}/\$everything"
//                    urls.add(patientUrl)
//
//                    val no = entry.resource as Encounter
//                    if (no.hasPartOf()) {
//                        val patientUrl = "${entry.fullUrl}/\$everything"
//                        urls.add(patientUrl)
//                    }
//                }
//
//                if (type == "Observation") {
//                    val patientUrl = "${entry.fullUrl}"
//                    urls.add(patientUrl)
//                }

//                if (type == "Location") {
//                    val patientUrl = "${entry.fullUrl}"
//                    urls.add(patientUrl)
//                }
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
            bundleCollection =
                response.entry
                    .map { it.resource }
                    .also { extractAndSaveLastUpdateTimestampToFetchFutureUpdates(it) }
        }
        return bundleCollection
    }

    private suspend fun extractAndSaveLastUpdateTimestampToFetchFutureUpdates(
        resources: List<Resource>,
    ) {
        resources
            .groupBy { it.resourceType }
            .entries
            .map { map ->
                dataStore.saveLastUpdatedTimestamp(
                    map.key,
                    map.value.maxOfOrNull { it.meta.lastUpdated }?.toTimeZoneString() ?: "",
                )
            }
    }

    fun getFacilitiesInSubcounty(subcountyId: String): List<String> {
        val map = mapOf(
            "sc001" to listOf("101", "102", "103"),
            "sc002" to listOf("201", "202")
        )
        return map[subcountyId] ?: emptyList()
    }


    fun getRespectiveFilteredResources(context: Context): LinkedList<String> {
        val formatter = FormatterClass()
        val storedRole = formatter.getSharedPref("practitionerRole", context)
        val userRole = UserRole.fromKey(storedRole ?: "")
        val urls = when (userRole) {

            UserRole.FACILITY_SURVEILLANCE_FOCAL_PERSON, UserRole.SUPERVISOR, UserRole.VACCINATOR -> {
                val facility = formatter.getSharedPref("facility", context)
                if (facility != null) {
                    listOf(
                        "Patient?organization?=Organization/{$facility}_sort=_lastUpdated",
                        "AllergyIntolerance",
                        "Observation?_count=1000",
                        "Encounter?_count=1000"
                    )
                } else emptyList()
            }

            UserRole.SUBCOUNTY_DISEASE_SURVEILLANCE_OFFICER -> {
                val subCounty = formatter.getSharedPref("subCounty", context)
                if (subCounty != null) {
                    val facilities = getFacilitiesInSubcounty(subCounty) // e.g., ["1234", "5678"]
                    if (facilities.isNotEmpty()) {
                        val patientQueries = facilities.map { facilityId ->
                            "Patient?organization=Organization/$facilityId&_sort=_lastUpdated"
                        }
                        val extraResources = listOf(
                            "Patient?_sort=_lastUpdated",
                            "Encounter?_count=1000",
                            "MeasureReport?_count=1000",
                            "QuestionnaireResponse?_count=1000"
                        )
                        patientQueries + extraResources
                    } else
                        emptyList()

                } else emptyList()
            }

            UserRole.COUNTY_DISEASE_SURVEILLANCE_OFFICER -> listOf(
                "MeasureReport?_count=1000",
                "QuestionnaireResponse?_count=1000",
                "Specimen?_count=1000"
            )


            null -> emptyList()
        }

        return LinkedList(urls)
    }

    fun getRespectiveFilteredResourcesAlt(): LinkedList<String> {
        val userRole = "sub_county_users"

        val urls = when (userRole.lowercase()) {
            "facility_nurse" -> listOf(
                "Patient?_sort=_lastUpdated",
                "AllergyIntolerance",
                "Observation?_count=1000",
                "Encounter?_count=1000"
            )

            "sub_county_user" -> listOf(
                "Patient?_sort=_lastUpdated",
                "Encounter?_count=1000",
                "MeasureReport?_count=1000",
                "QuestionnaireResponse?_count=1000"
            )

            "county_user" -> listOf(
                "MeasureReport?_count=1000",
                "QuestionnaireResponse?_count=1000",
                "Specimen?_count=1000"
            )

            "national_user" -> listOf(
                "Patient?_sort=_lastUpdated",
                "AllergyIntolerance",
                "Observation?_count=1000",
                "Encounter?_count=1000",
                "MeasureReport?_count=1000",
                "QuestionnaireResponse?_count=1000",
                "Specimen?_count=1000"
            )

            else -> emptyList() // No access or undefined role
        }

        return LinkedList(urls)
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
