package com.icl.surveillance.fhir

import android.content.Context
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.icl.surveillance.models.LocalLocationEntry
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Organization
import org.hl7.fhir.r4.model.Reference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ResourceCreationHelper {
    fun readJsonFromAssets(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    fun parseLocationEntries(json: String): List<LocalLocationEntry> {
        val gson = Gson()
        val listType = object : TypeToken<List<LocalLocationEntry>>() {}.type
        return gson.fromJson(json, listType)
    }

    fun createLocations(context: Context): List<Location> {
        val locationsList = mutableListOf<Location>()
        val json = readJsonFromAssets(context, "locations.json")
        val locations = parseLocationEntries(json)
        locations.forEach { entry ->
            val location = Location().apply {
                id = entry.resource.id
                meta = Meta().apply {
                    versionId = entry.resource.meta.versionId
                    lastUpdated = try {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
                            .apply { timeZone = TimeZone.getTimeZone("UTC") }
                            .parse(entry.resource.meta.lastUpdated)
                    } catch (e: Exception) {
                        Date()
                    }
                    source = entry.resource.meta.source
                }
                name = entry.resource.name
                val coding = entry.resource.type?.firstOrNull()?.coding?.firstOrNull()
                if (coding != null) {
                    type = mutableListOf(CodeableConcept().apply {
                        addCoding(Coding().apply {
                            system = coding.system
                            code = coding.code
                            display = coding.display
                        })
                    })
                }
                partOf = entry.resource.partOf?.let {
                    Reference().apply {
                        reference = it.reference
                        display = it.display
                    }
                }
            }
            locationsList.add(location)
        }
        return locationsList
    }

    fun createRespectiveOrganization(context: Context): List<Organization> {
        val locationsList = mutableListOf<Organization>()
        val json = readJsonFromAssets(context, "locations.json")
        val locations = parseLocationEntries(json)
        locations.forEach { entry ->
            val location = Organization().apply {
                id = entry.resource.id
                meta = Meta().apply {
                    versionId = entry.resource.meta.versionId
                    lastUpdated = try {
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
                            .apply { timeZone = TimeZone.getTimeZone("UTC") }
                            .parse(entry.resource.meta.lastUpdated)
                    } catch (e: Exception) {
                        Date()
                    }
                    source = entry.resource.meta.source
                }
                name = entry.resource.name
                val coding = entry.resource.type?.firstOrNull()?.coding?.firstOrNull()
                if (coding != null) {
                    type = mutableListOf(CodeableConcept().apply {
                        addCoding(Coding().apply {
                            system = coding.system
                            code = coding.code
                            display = coding.display
                        })
                    })
                }
                partOf = entry.resource.partOf?.let {
                    Reference().apply {
                        reference = it.reference.replace("Location", "Organization")
                        display = it.display
                    }
                }
            }
            locationsList.add(location)
        }
        return locationsList
    }

}