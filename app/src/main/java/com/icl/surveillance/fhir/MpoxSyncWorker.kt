package com.icl.surveillance.fhir

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MpoxSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val repo = FhirRepository(appContext)

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                // Step 1: Upload Patients
                prepareListInBatches("patients", applicationContext)

                // Step 2: Upload Encounters
                prepareEncountersBatches("encounters", applicationContext)

                // Step 3: Upload Observations
                prepareObsBatches("observations", applicationContext)

                // Step 4: Upload Measure Reports
                prepareMeasureReportsBatches("measureReports", applicationContext)

                // Step 5: Upload Questionnaire Responses
                prepareQuestionnaireResponsesBatches("questionnaireResponses", applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry() // or Result.failure()
        }
    }

    private fun prepareListInBatches(nameQuery: String, context: Context) {
        // move your code here but make it suspend-friendly
        repo.handleDataUpload(nameQuery, context)
    }

    private fun prepareEncountersBatches(nameQuery: String, context: Context) {
        // move your code here but make it suspend-friendly
        repo.handleDataUpload(nameQuery, context)
    }

    private fun prepareObsBatches(nameQuery: String, context: Context) {
        // move your code here but make it suspend-friendly
        repo.handleDataUpload(nameQuery, context)
    }

    private fun prepareMeasureReportsBatches(nameQuery: String, context: Context) {
        // move your code here but make it suspend-friendly
        repo.handleDataUpload(nameQuery, context)
    }

    private fun prepareQuestionnaireResponsesBatches(nameQuery: String, context: Context) {
        // move your code here but make it suspend-friendly
        repo.handleDataUpload(nameQuery, context)
    }
}
