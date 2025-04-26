package com.icl.surveillance.ui.patients

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.fhir.FhirEngine
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.icl.surveillance.R
import com.icl.surveillance.adapters.GroupPagerAdapter
import com.icl.surveillance.databinding.ActivityFullCaseDetailsBinding
import com.icl.surveillance.databinding.ActivitySummarizedBinding
import com.icl.surveillance.fhir.FhirApplication
import com.icl.surveillance.models.ChildItem
import com.icl.surveillance.models.OutputGroup
import com.icl.surveillance.models.OutputItem
import com.icl.surveillance.models.QuestionnaireItem
import com.icl.surveillance.ui.patients.FullCaseDetailsActivity
import com.icl.surveillance.utils.FormatterClass
import com.icl.surveillance.utils.toSlug
import com.icl.surveillance.viewmodels.ClientDetailsViewModel
import com.icl.surveillance.viewmodels.factories.PatientDetailsViewModelFactory
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class SummarizedActivity : AppCompatActivity() {
    private lateinit var groups: List<OutputGroup>
    private lateinit var binding: ActivitySummarizedBinding
    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientDetailsViewModel: ClientDetailsViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySummarizedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val patientId = FormatterClass().getSharedPref("resourceId", this@SummarizedActivity)
        val currentCase = FormatterClass().getSharedPref("currentCase", this)

        fhirEngine = FhirApplication.fhirEngine(this@SummarizedActivity)
        patientDetailsViewModel =
            ViewModelProvider(
                this,
                PatientDetailsViewModelFactory(
                    this@SummarizedActivity.application, fhirEngine, "$patientId"
                ),
            )
                .get(ClientDetailsViewModel::class.java)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        groups = parseFromAssets(this) // this = Context
        for (group in groups) {
            Log.d("Group", "Group Item: ${group.text} (${group.linkId})")
            for (item in group.items) {
                Log.d("Item", " - Item: ${item.text} (${item.linkId}) Type: ${item.type}")
            }
        }
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout

        if (currentCase != null) {
            val slug = currentCase.toSlug()
            patientDetailsViewModel.getPatientInfoSummaryData(slug)
        }
        patientDetailsViewModel.liveSummaryData.observe(this) { data ->
            Log.e("Observation", "Observation ***** Name ${data.name}")
            data.observations.forEach {
                Log.e("Observation", "Observation ***** ${it.code}  ${it.value}")
            }
            groups.forEach { group ->
                // For each item inside the group
                group.items.forEach { outputItem ->
                    // Try to find a matching observation
                    val matchingObservation = data.observations.find { obs ->
                        obs.code == outputItem.linkId
                    }

                    if (matchingObservation != null) {
                        outputItem.value = matchingObservation.value
                    }
                }
            }
            viewPager.adapter = GroupPagerAdapter(this, groups)

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = groups[position].text
            }.attach()
        }


    }


    fun parseFromAssets(context: Context): List<OutputGroup> {
        // Read JSON file from assets
        val jsonContent = context.assets.open("afp-case.json")
            .bufferedReader()
            .use { it.readText() }

        val gson = Gson()
        val questionnaire = gson.fromJson(jsonContent, QuestionnaireItem::class.java)

        val outputGroups = questionnaire.item.map { group ->
            OutputGroup(
                linkId = group.linkId,
                text = group.text,
                type = group.type,
                items = group.item?.flatMap { flattenItems(it) } ?: emptyList()
            )
        }

        return outputGroups
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    fun flattenItems(item: ChildItem): List<OutputItem> {
        val children = item.item?.flatMap { flattenItems(it) } ?: emptyList()

        // If current item is NOT of type "display", include it
        return if (item.type != "display") {
            val current = OutputItem(
                linkId = item.linkId,
                text = item.text,
                type = item.type
            )
            listOf(current) + children
        } else {
            children
        }
    }

}