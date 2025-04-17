package com.icl.surveillance.holders

import android.widget.TextView
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

    if (patientItem.status != "Pending Results") {
      this.status.text = patientItem.status
      this.status.setTextColor(this.status.context.getColor(R.color.red))
    } else {
      this.status.text = patientItem.status
    }

    this.itemView.setOnClickListener { onItemClicked(patientItem) }
  }

  /** The new ui just shows shortened id with just last 3 characters. */
  private fun getTruncatedId(patientItem: PatientListViewModel.PatientItem): String {
    return patientItem.resourceId.takeLast(3)
  }
}
