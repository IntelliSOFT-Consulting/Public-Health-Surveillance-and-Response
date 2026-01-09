package com.icl.surveillance.auth

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.fhir.FhirEngine
import com.icl.surveillance.MainActivity
import com.icl.surveillance.R
import com.icl.surveillance.databinding.ActivityInitialSyncBinding
import com.icl.surveillance.fhir.FhirApplication
import com.icl.surveillance.utils.FhirBundleLoader
import com.icl.surveillance.utils.FormatterClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class InitialSyncActivity : AppCompatActivity() {
    private lateinit var fhirEngine: FhirEngine
    private lateinit var binding: ActivityInitialSyncBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityInitialSyncBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        fhirEngine = FhirApplication.fhirEngine(this@InitialSyncActivity)
        if (FormatterClass().isSyncDone(this)) {
            startMain()

        }else {
            handleInitialSync()
        }
    }


    private fun handleInitialSync() {
        lifecycleScope.launch {
            val loader = FhirBundleLoader(this@InitialSyncActivity)
            val status = binding.syncStatusText   // or findViewById
            fun update(msg: String) {

                status.text = msg
            }

            update("Preparing data…")

            importBundleFile(
                loader, fhirEngine,
                "fhir-bundle-counties-kenya.json",
                "Counties",
                ::update
            )

            importBundleFile(
                loader, fhirEngine,
                "fhir-bundle-sub-counties-kenya.json",
                "Sub Counties",
                ::update
            )

            importBundleFile(
                loader, fhirEngine,
                "fhir-bundle-wards-kenya.json",
                "Wards",
                ::update
            )

            importBundleFile(
                loader, fhirEngine,
                "fhir-bundle-facilities-kenya.json",
                "Facilities",
                ::update
            )
            update("All data imported successfully.")
            FormatterClass().setSyncDone(this@InitialSyncActivity)
            startMain()
        }
    }

    private fun startMain() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private suspend fun importBundleFile(
        loader: FhirBundleLoader,
        engine: FhirEngine,
        fileName: String,
        label: String,
        onStatus: (String) -> Unit
    ) {
        onStatus("Preparing $label…")

        val json = when (fileName) {
            "fhir-bundle.json" -> loader.loadCompressedBundleJson("fhir-bundle-facilities.json.gz")
            else -> loader.loadBundleJson(fileName)
        }

        onStatus("Parsing $label…")
        yield()
        val bundle = loader.parseFhirBundle(json)

        onStatus("Loading ${bundle.entry.size - 1} $label…")
        yield()


        val bundleResult = withContext(Dispatchers.IO) {
            loader.createBundleInEngine(engine, bundle)
        }
        withContext(Dispatchers.Main) {
            val message = buildString {
                append("Processed ${bundleResult.processed} / ${bundle.entry.size}")
                if (bundleResult.skipped > 0) append(", skipped ${bundleResult.skipped}")
                if (bundleResult.failed > 0) append(", failed ${bundleResult.failed}")
            }
            onStatus(message)
            yield()
            onStatus("Finished $label.")
            yield()
        }

    }
}