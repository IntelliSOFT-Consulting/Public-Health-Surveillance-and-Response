package com.icl.surveillance.fhir


import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import com.icl.surveillance.models.FhirEntry
import com.icl.surveillance.network.Interface
import com.icl.surveillance.network.RetrofitBuilder
import com.icl.surveillance.utils.Constants
import com.icl.surveillance.utils.Constants.BASE_URL
import com.icl.surveillance.utils.Constants.LOCATION_STARTER
import com.icl.surveillance.utils.FormatterClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Meta
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class LocationDownloadedWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {


    override fun doWork(): Result = try {
        val context = applicationContext
        val fhirEngine = FhirApplication.fhirEngine(context)
        val currentUrl = Constants.getNextUrl(context) ?: LOCATION_STARTER
       // if (!Constants.isDownloadComplete(context)) {
         //   fetchAllPages(currentUrl, context, fhirEngine)
       // }

        val output = workDataOf("fetch_complete" to true)
        Result.success(output)
    } catch (e: Exception) {
        Log.e("LocationWorker", "Error in doWork", e)
        Result.retry()
    }

    fun createLocationResource(entry: FhirEntry): Location {
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
            val coding = entry.resource.type.firstOrNull()?.coding?.firstOrNull()
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
        return location
    }

    private fun fetchAllPages(
        startUrl: String,
        context: Context,
        fhirEngine: FhirEngine
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val apiService = RetrofitBuilder.getRetrofit(BASE_URL).create(Interface::class.java)
            var nextUrl: String? = startUrl
            var page = 1

            while (!nextUrl.isNullOrEmpty()) {
                if (!Constants.isDownloadComplete(context)) {
                    try {
                        Log.d("LocationWorker", "Fetching page $page: $nextUrl")
                        val token = FormatterClass().getSharedPref("access_token", context)
                        if (token != null) {
                            val bundle = apiService.fetchBundle(nextUrl, "Bearer $token")
                            val entries = bundle.entry ?: emptyList()
                            coroutineScope {
                                entries.map { entry ->
                                    async(Dispatchers.IO) {
                                        try {
                                            val location = createLocationResource(entry)
                                            val exists = fhirEngine.search<Location> {
                                                filter(Resource.RES_ID, { value = of(location.id) })
                                            }
                                            if (exists.isEmpty()) {
                                              //  fhirEngine.create(location)
                                            }
                                        } catch (e: Exception) {
                                            Log.e(
                                                "LocationWorker",
                                                "Failed to save: ${entry.resource.id}",
                                                e
                                            )
                                        }
                                    }
                                }.awaitAll()
                            }
                            nextUrl = bundle.link?.firstOrNull { it.relation == "next" }?.url
                            if (nextUrl != null) {
                                Constants.saveNextUrl(context, nextUrl)
                                Constants.markActionComplete(context, false)
                            } else {
                                Constants.clear(context)
                                Constants.markActionComplete(context, true)
                            }
                            Log.d("LocationWorker", "Next URL: $nextUrl")
                            delay(400) // prevent rapid fire requests
                            page++
                        }
                    } catch (e: Exception) {
                        Log.e("LocationWorker", "Error fetching page: $nextUrl", e)
                        break // Prevent infinite loop
                    }
                }
            }
        }
    }


}