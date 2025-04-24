package com.icl.surveillance.cases

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.FhirEngine
import com.icl.surveillance.R
import com.icl.surveillance.adapters.PatientItemRecyclerViewAdapter
import com.icl.surveillance.databinding.ActivityCaseListingBinding
import com.icl.surveillance.databinding.ActivityFullCaseDetailsBinding
import com.icl.surveillance.fhir.FhirApplication
import com.icl.surveillance.ui.patients.FullCaseDetailsActivity
import com.icl.surveillance.ui.patients.PatientListViewModel
import com.icl.surveillance.utils.FormatterClass
import com.icl.surveillance.viewmodels.ClientDetailsViewModel

class CaseListingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCaseListingBinding
    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientListViewModel: PatientListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCaseListingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val titleName = FormatterClass().getSharedPref("title", this)
        val currentCase = FormatterClass().getSharedPref("currentCase", this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.apply {
            title = " $titleName"
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fhirEngine = FhirApplication.fhirEngine(this)
        patientListViewModel =
            ViewModelProvider(
                this,
                PatientListViewModel.PatientListViewModelFactory(
                    this.application, fhirEngine
                ),
            )
                .get(PatientListViewModel::class.java)


        val recyclerView: RecyclerView = binding.patientListContainer.patientList
        val adapter = PatientItemRecyclerViewAdapter(this::onPatientItemClicked)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                setDrawable(ColorDrawable(Color.LTGRAY))
            },
        )

        println("Started searching for cases *** $currentCase")
        if (currentCase != null) {
            val slug = currentCase.toSlug()
            patientListViewModel.handleCurrentCaseListing(slug)
        }

        patientListViewModel.liveSearchedCases.observe(this) {
            binding.apply { patientListContainer.pbProgress.visibility = View.GONE }
            if (it.isEmpty()) {
                binding.apply { patientListContainer.caseCount.visibility = View.VISIBLE }
            } else {
                binding.apply { patientListContainer.caseCount.visibility = View.GONE }
            }

            adapter.submitList(it)
        }
    }

    private fun onPatientItemClicked(patientItem: PatientListViewModel.PatientItem) {
        println("Going to client details activity with the id as ${patientItem.resourceId} and Encounter ${patientItem.encounterId}")
        FormatterClass().saveSharedPref("resourceId", patientItem.resourceId, this)
        FormatterClass().saveSharedPref("encounterId", patientItem.encounterId, this)
        startActivity(Intent(this@CaseListingActivity, FullCaseDetailsActivity::class.java))
    }

    private fun String.toSlug(): String {
        return this
            .trim() // remove leading/trailing spaces
            .lowercase() // make all lowercase
            .replace("[^a-z0-9\\s-]".toRegex(), "") // remove special characters
            .replace("\\s+".toRegex(), "-") // replace spaces with hyphens
            .replace("-+".toRegex(), "-") // collapse multiple hyphens
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

