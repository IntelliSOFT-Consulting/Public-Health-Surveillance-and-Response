package com.icl.surveillance.ui.patients

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.google.android.fhir.FhirEngine
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.icl.surveillance.R
import com.icl.surveillance.adapters.GroupPagerAdapter
import com.icl.surveillance.databinding.ActivitySummarizedBinding
import com.icl.surveillance.fhir.FhirApplication
import com.icl.surveillance.models.ChildItem
import com.icl.surveillance.models.OutputGroup
import com.icl.surveillance.models.OutputItem
import com.icl.surveillance.models.QuestionnaireItem
import com.icl.surveillance.ui.patients.custom.ContactInformationFragment
import com.icl.surveillance.ui.patients.custom.ITDLabFragment
import com.icl.surveillance.ui.patients.custom.LocalLabFragment
import com.icl.surveillance.ui.patients.custom.RegionalLabFragment
import com.icl.surveillance.ui.patients.custom.VlFollowupFragment
import com.icl.surveillance.ui.patients.custom.VlLabFragment
import com.icl.surveillance.ui.patients.custom.VlTreatmentFragment
import com.icl.surveillance.utils.FormatterClass
import com.icl.surveillance.viewmodels.ClientDetailsViewModel
import com.icl.surveillance.viewmodels.factories.PatientDetailsViewModelFactory

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
        val latestEncounter = FormatterClass().getSharedPref("latestEncounter", this)

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
        if (latestEncounter != null) {

            groups = parseFromAssets(this, latestEncounter) // this = Context
            for (group in groups) {
                Log.d("Group", "Group Item: ${group.text} (${group.linkId})")
                for (item in group.items) {
                    Log.d(
                        "Item",
                        " - Item: ${item.text} (${item.linkId}) Type: ${item.type} ${item.value}"
                    )
                }
            }
            val viewPager = binding.viewPager
            val tabLayout = binding.tabLayout

            if (currentCase != null) {
                val slug = currentCase.toSlug()
                patientDetailsViewModel.getPatientInfoSummaryData(slug)
            }

            val customFragments = when (latestEncounter) {
                "afp-case-information" -> {
                    listOf(
                        "Stool Specimen Results" to LocalLabFragment(),
                        "ITD Lab Results" to ITDLabFragment(),
                        "Final Laboratory Results" to RegionalLabFragment(),
                        "Contact Information" to ContactInformationFragment()
                    )
                }

                "vl-case-information" -> {
                    listOf(
                        "Laboratory Examination" to VlLabFragment(),
                        "Treatment/Hospitalization" to VlTreatmentFragment(),
                        "Six months followup examinations" to VlFollowupFragment()
                    )
                }

                else -> emptyList()
            }
            patientDetailsViewModel.liveSummaryData.observe(this) { data ->

                groups.forEach { group ->
                    // For each item inside the group
                    group.items.forEach { outputItem ->
                        // Try to find a matching observation
                        val matchingObservation = data.observations.find { obs ->
                            obs.code == outputItem.linkId
                        }

                        // Get EPID No
                        if (outputItem.linkId == "992818778559") {
                            outputItem.value = data.epidNo
                        } else {

                            if (matchingObservation != null) {
                                outputItem.value = matchingObservation.value
                            }
                        }
                    }
                }
                val adapter = GroupPagerAdapter(this, groups, customFragments)
                viewPager.adapter = adapter

                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text = adapter.getTabTitle(position)
                }.attach()
            }

        } else {
            Toast.makeText(this, "Please try again later!!", Toast.LENGTH_SHORT).show()
        }
    }

    fun String.toSlug(): String {
        return this
            .trim()
            .lowercase()
            .replace("[^a-z0-9\\s-]".toRegex(), "")
            .replace("\\s+".toRegex(), "-")
            .replace("-+".toRegex(), "-")
    }



    fun parseFromAssets(context: Context, latestEncounter: String): List<OutputGroup> {
        var outputGroups: List<OutputGroup> = emptyList()

        val assets = when (latestEncounter) {
            "afp-case-information" -> "afp-case.json"
            "vl-case-information" -> "vl-case.json"
            "social-listening-and-rumor-tracking-tool" -> "rumor-tracking-case.json"
            else -> ""
        }
        try {
            if (assets.isNotEmpty()) {
                val jsonContent = context.assets.open(assets)
                    .bufferedReader()
                    .use { it.readText() }

                val gson = Gson()
                val questionnaire = gson.fromJson(jsonContent, QuestionnaireItem::class.java)

                outputGroups = questionnaire.item.map { group ->
                    OutputGroup(
                        linkId = group.linkId,
                        text = group.text,
                        type = group.type,
                        items = group.item?.flatMap { flattenItems(it) } ?: emptyList()
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("TAG", "File Error ${e.message}")
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