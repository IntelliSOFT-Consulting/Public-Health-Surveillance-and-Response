package com.icl.surveillance.fhir

import android.app.Application
import android.content.Context
import android.util.Log
import com.google.android.fhir.DatabaseErrorStrategy
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.FhirEngineConfiguration
import com.google.android.fhir.FhirEngineProvider
import com.google.android.fhir.NetworkConfiguration
import com.google.android.fhir.ServerConfiguration
import com.google.android.fhir.datacapture.DataCaptureConfig
import com.google.android.fhir.datacapture.XFhirQueryResolver
import com.google.android.fhir.search.search // Import the local fhir
import com.google.android.fhir.sync.Sync
import com.google.android.fhir.sync.remote.HttpLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FhirApplication : Application(), DataCaptureConfig.Provider {
  // Only initiate the FhirEngine when used for the first time, not when the app is created.
  private val fhirEngine: FhirEngine by lazy { constructFhirEngine() }

  private var dataCaptureConfig: DataCaptureConfig? = null

  private val dataStore by lazy { DemoDataStore(this) }

  override fun onCreate() {
    super.onCreate()

    FhirEngineProvider.init(
        FhirEngineConfiguration(
            enableEncryptionIfSupported = false,
            DatabaseErrorStrategy.RECREATE_AT_OPEN,
            ServerConfiguration(
                //                "https://hapi.fhir.org/baseR4/",
                "https://dsrfhir.intellisoftkenya.com/hapi/fhir/",
                httpLogger =
                    HttpLogger(
                        HttpLogger.Configuration(
//                                                        if (BuildConfig.DEBUG)
                            // HttpLogger.Level.BODY
                            //                            else
                            HttpLogger.Level.BASIC,
                        ),
                    ) {
                      Log.e("App-HttpLog", it)
                    },
                networkConfiguration = NetworkConfiguration(uploadWithGzip = false),
            ),
        ),
    )

    dataCaptureConfig =
        DataCaptureConfig().apply {
          urlResolver = ReferenceUrlResolver(this@FhirApplication as Context)
          xFhirQueryResolver = XFhirQueryResolver { it ->
            fhirEngine.search(it).map { it.resource }
          }
        }

    CoroutineScope(Dispatchers.IO).launch { Sync.oneTimeSync<FhirSyncWorker>(this@FhirApplication) }
  }

  private fun constructFhirEngine(): FhirEngine {
    return FhirEngineProvider.getInstance(this)
  }

  companion object {
    fun fhirEngine(context: Context) = (context.applicationContext as FhirApplication).fhirEngine

    fun dataStore(context: Context) = (context.applicationContext as FhirApplication).dataStore
  }

  override fun getDataCaptureConfig(): DataCaptureConfig = dataCaptureConfig ?: DataCaptureConfig()
}
