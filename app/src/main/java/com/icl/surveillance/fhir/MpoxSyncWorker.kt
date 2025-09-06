package com.icl.surveillance.fhir

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters


class MpoxSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val repo: FhirRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Step 1: Upload Patients
            prepareListInBatches("patients")
            // Wait until done (since suspending)

            // Step 2: Upload Encounters
            prepareEncountersBatches("encounters")

            // Step 3: Upload Observations
            prepareObsBatches("observations")

            // Step 4: Upload Measure Reports
            prepareMeasureReportsBatches("measureReports")

            // Step 5: Upload Questionnaire Responses
            prepareQuestionnaireResponsesBatches("questionnaireResponses")

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry() // or Result.failure()
        }
    }

    private suspend fun prepareListInBatches(nameQuery: String) {
        // move your code here but make it suspend-friendly
        repo.handleDataUpload(nameQuery)
    }

    private suspend fun prepareEncountersBatches(nameQuery: String) {
        // move your code here but make it suspend-friendly
        repo.handleDataUpload(nameQuery)
    }

    private suspend fun prepareObsBatches(nameQuery: String) {
        // move your code here but make it suspend-friendly
        repo.handleDataUpload(nameQuery)
    }

    private suspend fun prepareMeasureReportsBatches(nameQuery: String) {
        // move your code here but make it suspend-friendly
        repo.handleDataUpload(nameQuery)
    }

    private suspend fun prepareQuestionnaireResponsesBatches(nameQuery: String) {
        // move your code here but make it suspend-friendly
        repo.handleDataUpload(nameQuery)
    }
}
