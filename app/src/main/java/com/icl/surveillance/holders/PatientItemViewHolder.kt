package com.icl.surveillance.holders

import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.recyclerview.widget.RecyclerView
import com.icl.surveillance.R
import com.icl.surveillance.databinding.PatientListItemViewBinding
import com.icl.surveillance.ui.patients.PatientListViewModel

class PatientItemViewHolder(binding: PatientListItemViewBinding) :
    RecyclerView.ViewHolder(binding.root) {
    private val nameView: TextView = binding.name
    private val epid: TextView = binding.epid
    private val county: TextView = binding.county
    private val subCounty: TextView = binding.subCounty
    private val dateReported: TextView = binding.dateReported
    private val status: TextView = binding.tvFinal
    private val labResults: TextView = binding.labResults
    private val tvLabLabel: TextView = binding.tvLabLabel

    fun bindTo(
        patientItem: PatientListViewModel.PatientItem,
        onItemClicked: (PatientListViewModel.PatientItem) -> Unit,
    ) {
        this.nameView.text = patientItem.name
        this.epid.text = patientItem.epid
        this.county.text = patientItem.county
        this.subCounty.text = patientItem.subCounty
        this.dateReported.text = patientItem.caseOnsetDate
        this.labResults.text = patientItem.labResults

        println("Case Information type ${patientItem.caseList}")
        var final = patientItem.status

        if (patientItem.caseList.trim() != "Case") {
            this.tvLabLabel.visibility = View.INVISIBLE
            this.labResults.visibility = View.INVISIBLE
            final = "Confirmed by EPI Linkage"
        }
        this.status.text = final
        this.status.setTextColor(Color.BLACK)
        this.labResults.setTextColor(Color.BLACK)
        when (final.trim()) {
            "Confirmed by lab" -> {
                this.status.setTextColor(this.status.context.getColor(R.color.red))
            }

            "Discarded" -> {
                this.status.setTextColor(this.status.context.getColor(R.color.discarded))
            }

            "Compatible/Clinical/Probable" -> {
                this.status.setTextColor(this.status.context.getColor(R.color.compatible))
            }

            "Confirmed by EPI Linkage" -> {
                this.status.setTextColor(this.status.context.getColor(R.color.blue))
            }

            else -> {
                this.status.setTextColor(this.status.context.getColor(R.color.pending))
            }
        }

        if (patientItem.labResults.trim() == "Positive") {
            this.labResults.setTextColor(this.labResults.context.getColor(R.color.red))
        }

        this.itemView.setOnClickListener { onItemClicked(patientItem) }
    }

    /** The new ui just shows shortened id with just last 3 characters. */
    private fun getTruncatedId(patientItem: PatientListViewModel.PatientItem): String {
        return patientItem.resourceId.takeLast(3)
    }
}