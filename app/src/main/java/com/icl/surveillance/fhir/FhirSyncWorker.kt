package com.icl.surveillance.fhir

import android.content.Context
import androidx.work.WorkerParameters
import com.google.android.fhir.sync.AcceptLocalConflictResolver
import com.google.android.fhir.sync.DownloadWorkManager
import com.google.android.fhir.sync.FhirSyncWorker
import com.google.android.fhir.sync.upload.HttpCreateMethod
import com.google.android.fhir.sync.upload.HttpUpdateMethod
import com.google.android.fhir.sync.upload.UploadStrategy

class FhirSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    FhirSyncWorker(appContext, workerParams) {

  override fun getDownloadWorkManager(): DownloadWorkManager {
    return TimestampBasedDownloadWorkManagerImpl(FhirApplication.dataStore(applicationContext))
  }

  override fun getConflictResolver() = AcceptLocalConflictResolver

  override fun getFhirEngine() = FhirApplication.fhirEngine(applicationContext)

  override fun getUploadStrategy(): UploadStrategy =
      UploadStrategy.forBundleRequest(
          methodForCreate = HttpCreateMethod.PUT,
          methodForUpdate = HttpUpdateMethod.PATCH,
          squash = true,
          bundleSize = 500,
      )
}
