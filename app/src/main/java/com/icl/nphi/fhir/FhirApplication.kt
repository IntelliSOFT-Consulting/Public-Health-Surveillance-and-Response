package com.icl.nphi.fhir

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.NetworkType
import com.google.android.fhir.DatabaseErrorStrategy
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineConfiguration
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.NetworkConfiguration
import com.google.android.fhir.ServerConfiguration
import com.google.android.fhir.datacapture.DataCaptureConfig
import com.google.android.fhir.datacapture.XFhirQueryResolver
import com.google.android.fhir.search.search // Import the local fhir
import com.google.android.fhir.sync.CurrentSyncJobStatus
import com.google.android.fhir.sync.HttpAuthenticationMethod
import com.google.android.fhir.sync.PeriodicSyncConfiguration
import com.google.android.fhir.sync.RepeatInterval
import com.google.android.fhir.sync.ResourceSyncException
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.SyncJobStatus
import com.google.android.fhir.sync.remote.HttpLogger
import com.icl.nphi.network.Constants.BASE_URL
import com.icl.nphi.network.Constants.TEST_TOKEN
import com.icl.nphi.utils.ContribQuestionnaireItemViewHolderFactoryMatchersProviderFactory
import com.icl.nphi.utils.FormatterClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class FhirApplication : Application(), DataCaptureConfig.Provider, Configuration.Provider {
    private val repo by lazy { FhirRepository(this) }

    // Only initiate the FhirEngine when used for the first time, not when the app is created.
    private val fhirEngine: FhirEngine by lazy { constructFhirEngine() }

    private var dataCaptureConfig: DataCaptureConfig? = null

    private val dataStore by lazy { DemoDataStore(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    override fun onCreate() {

        super.onCreate()

        FhirEngineProvider.init(
            FhirEngineConfiguration(
                enableEncryptionIfSupported = false,
                DatabaseErrorStrategy.RECREATE_AT_OPEN,
                ServerConfiguration(
                    BASE_URL,
                    httpLogger =
                        HttpLogger(
                            HttpLogger.Configuration(
                                HttpLogger.Level.BASIC,
                            ),
                        ) {
                            Log.e("App-HttpLog", it)
                        },
                    networkConfiguration = NetworkConfiguration(uploadWithGzip = true),
                    authenticator = { HttpAuthenticationMethod.Bearer(retrieveStoredToken()) }
                ),
            ),
        )
        try {


            dataCaptureConfig =
                DataCaptureConfig().apply {
                    urlResolver = ReferenceUrlResolver(this@FhirApplication as Context)
                    questionnaireItemViewHolderFactoryMatchersProviderFactory =
                        ContribQuestionnaireItemViewHolderFactoryMatchersProviderFactory
                    xFhirQueryResolver = XFhirQueryResolver { it ->
                        fhirEngine.search(it).map { it.resource }
                    }
                }
            setupPeriodicSync()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupPeriodicSync() {
        appScope.launch {
            try {
                Sync.periodicSync<AppFhirSyncWorker>(
                    this@FhirApplication,
                    periodicSyncConfiguration = PeriodicSyncConfiguration(
                        syncConstraints = Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                        repeat = RepeatInterval(interval = 15, timeUnit = TimeUnit.MINUTES)
                    )

                ).catch { throwable ->
                    Log.e(
                        "FHIR_SYNC",
                        "Error setting up periodic sync: ${throwable.message}",
                        throwable
                    )
                }
                    .collect { syncJobStatus ->
//                        when (syncJobStatus) {
//                            is SyncJobStatus.Started -> {
//                                Log.d("FHIR_SYNC", "Sync job enqueued")
//                            }
//
//                            is SyncJobStatus.InProgress -> {
//                                Log.d(
//                                    "FHIR_SYNC",
//                                    "Sync in progress: ${syncJobStatus.currentSyncJobStatus}"
//                                )
//                            }
//
//                            is SyncJobStatus.Succeeded -> {
//                                Log.d("FHIR_SYNC", "Sync completed successfully")
//                            }
//
//                            is SyncJobStatus.Failed -> {
//                                Log.e("FHIR_SYNC", "Periodic sync run FAILED at: ${syncJobStatus.timestamp}")
//                                val failureStatus = syncJobStatus.currentSyncJobStatus
//                                if (failureStatus is CurrentSyncJobStatus.Failed) {
//                                    // Log the specific exceptions for easier debugging.
//                                    failureStatus.timestamp
//                                        .forEach { info ->
//                                        Log.e("FHIR_SYNC_FAILURE", "Failure on resource '${info.resourceType}':", info.exception)
//                                    }
//                                } else {
//                                    Log.e("FHIR_SYNC_FAILURE", "Sync failed with an unexpected status: ${failureStatus::class.simpleName}")
//                                }
//                            }
//
//                            else -> {
//                                Log.d(
//                                    "FHIR_SYNC",
//                                    "Other sync status: ${syncJobStatus::class.simpleName}"
//                                )
//                            }
//                        }
                    }
            } catch (e: Exception) {
                Log.e("FHIR_SYNC", "Error setting up periodic sync: ${e.message}", e)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        appScope.cancel()
    }

    fun retrieveStoredToken(): String {
        return FormatterClass()
            .getSharedPref("access_token", this@FhirApplication)
            ?: TEST_TOKEN
    }

    private fun constructFhirEngine(): FhirEngine {
        return FhirEngineProvider.getInstance(this)
    }

    companion object {
        fun fhirEngine(context: Context) =
            (context.applicationContext as FhirApplication).fhirEngine

        fun dataStore(context: Context) = (context.applicationContext as FhirApplication).dataStore
    }

    override fun getDataCaptureConfig(): DataCaptureConfig =
        dataCaptureConfig ?: DataCaptureConfig()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(MpoxWorkerFactory(repo))
            .build()

}
