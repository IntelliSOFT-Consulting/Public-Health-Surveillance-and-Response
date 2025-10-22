package com.icl.nphi.monitor

import android.util.Log
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.extensions.logicalId
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.MeasureReport
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Resource
import java.util.Date

class FhirSyncService(
    private val fhirEngine: FhirEngine,
    private val fhirDataSource: FhirDataSource // Your server API interface
) {

    suspend fun uploadResource(resource: Resource): SyncResult {
        return try {
            Log.d("FhirSync", "Uploading resource: ${resource.resourceType}/${resource.logicalId}")

            when (resource.resourceType.name) {
                "Patient" -> uploadPatient(resource as Patient)
                "Observation" -> uploadObservation(resource as Observation)
                "Encounter" -> uploadEncounter(resource as Encounter)
                "QuestionnaireResponse" -> uploadQuestionnaireResponse(resource as QuestionnaireResponse)
                "MeasureReport" -> uploadMeasureReport(resource as MeasureReport)
                else -> SyncResult.Failure("Unsupported resource type: ${resource.resourceType}")
            }
        } catch (e: Exception) {
            Log.e(
                "FhirSync",
                "Upload failed for ${resource.resourceType}/${resource.logicalId}: ${e.message}"
            )
            SyncResult.Failure(e.message ?: "Unknown error")
        }
    }

    private suspend fun uploadPatient(patient: Patient): SyncResult {
        return try {
//            let's prepare resource as json string
            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val json = jsonParser.encodeResourceToString(patient)
            val requestBody = json.toRequestBody("application/json".toMediaType())


            val result = fhirDataSource.createPatient(patient.idElement.idPart, requestBody)

            if (result.isSuccessful) {
                // Update local resource with server metadata
                updateLocalResourceAfterSync(patient)
                SyncResult.Success(patient.logicalId)
            } else {
            SyncResult.Failure("Server returned error:  ")
             }
        } catch (e: Exception) {
            SyncResult.Failure("Patient upload failed: ${e.message}")
        }
    }

    private suspend fun uploadObservation(observation: Observation): SyncResult {
        return try {
            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val json = jsonParser.encodeResourceToString(observation)
            val requestBody = json.toRequestBody("application/json".toMediaType())

            val result = fhirDataSource.createObservation(observation.logicalId, requestBody)

            if (result.isSuccessful) {
                updateLocalResourceAfterSync(observation)
                SyncResult.Success(observation.logicalId)
            } else {
                SyncResult.Failure("Observation upload failed: ${result.code()}")
            }
        } catch (e: Exception) {
            SyncResult.Failure("Observation upload failed: ${e.message}")
        }
    }

    private suspend fun uploadEncounter(encounter: Encounter): SyncResult {
        return try {
            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val json = jsonParser.encodeResourceToString(encounter)
            val requestBody = json.toRequestBody("application/json".toMediaType())

            val result = fhirDataSource.createEncounter(encounter.logicalId, requestBody)

            if (result.isSuccessful) {
                updateLocalResourceAfterSync(encounter)
                SyncResult.Success(encounter.logicalId)
            } else {
                SyncResult.Failure("Encounter upload failed: ${result.code()}")
            }
        } catch (e: Exception) {
            SyncResult.Failure("Encounter upload failed: ${e.message}")
        }
    }

    private suspend fun uploadQuestionnaireResponse(response: QuestionnaireResponse): SyncResult {
        return try {
            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val json = jsonParser.encodeResourceToString(response)
            val requestBody = json.toRequestBody("application/json".toMediaType())

            val result = fhirDataSource.createQuestionnaireResponse(response.logicalId, requestBody)

            if (result.isSuccessful) {
                updateLocalResourceAfterSync(response)
                SyncResult.Success(response.logicalId)
            } else {
                SyncResult.Failure("QuestionnaireResponse upload failed: ${result.code()}")
            }
        } catch (e: Exception) {
            SyncResult.Failure("QuestionnaireResponse upload failed: ${e.message}")
        }
    }

    private suspend fun uploadMeasureReport(report: MeasureReport): SyncResult {
        return try {
            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val json = jsonParser.encodeResourceToString(report)
            val requestBody = json.toRequestBody("application/json".toMediaType())

            val result = fhirDataSource.createMeasureReport(report.logicalId, requestBody)

            if (result.isSuccessful) {
                updateLocalResourceAfterSync(report)
                SyncResult.Success(report.logicalId)
            } else {
                SyncResult.Failure("MeasureReport upload failed: ${result.code()}")
            }
        } catch (e: Exception) {
            SyncResult.Failure("MeasureReport upload failed: ${e.message}")
        }
    }

    private suspend fun updateLocalResourceAfterSync(resource: Resource) {
        try {
            // Add sync metadata to the resource
            resource.meta = resource.meta ?: Meta()
            resource.meta!!.lastUpdated = Date() // Set sync timestamp
            resource.meta!!.versionId = "1" // Set version to 1

//            resource.meta!!.addTag().apply {
//                system = "http://nphi.icl.com/tags"
//                code = "synced"
//                display = "Synced with server"
//            }

            // Update the resource in local database
            when (resource) {
                is Patient -> fhirEngine.update(resource)
                is Observation -> fhirEngine.update(resource)
                is Encounter -> fhirEngine.update(resource)
                is QuestionnaireResponse -> fhirEngine.update(resource)
                is MeasureReport -> fhirEngine.update(resource)
            }

            Log.d(
                "FhirSync",
                "Updated local resource: ${resource.resourceType}/${resource.logicalId}"
            )
        } catch (e: Exception) {
            Log.e("FhirSync", "Failed to update local resource: ${e.message}")
        }
    }

    suspend fun uploadMultipleResources(resources: List<Resource>): BulkSyncResult {
        val results = mutableListOf<SyncResult>()

        resources.forEach { resource ->
            val result = uploadResource(resource)
            results.add(result)
            delay(100) // Small delay between requests to avoid overwhelming server
        }

        val successful = results.count { it is SyncResult.Success }
        val failed = results.count { it is SyncResult.Failure }

        return BulkSyncResult(
            total = resources.size,
            successful = successful,
            failed = failed,
            individualResults = results
        )
    }
}



