package com.icl.surveillance.holders

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.icl.surveillance.databinding.PatientListItemViewBinding
import com.icl.surveillance.ui.patients.PatientListViewModel

class PatientItemViewHolder(binding: PatientListItemViewBinding) :
    RecyclerView.ViewHolder(binding.root) {
  private val nameView: TextView = binding.name
  private val epid: TextView = binding.epid
  private val county: TextView = binding.county
  private val subCounty: TextView = binding.subCounty
  private val dateReported: TextView = binding.dateReported

  fun bindTo(
      patientItem: PatientListViewModel.PatientItem,
      onItemClicked: (PatientListViewModel.PatientItem) -> Unit,
  ) {
    this.nameView.text = patientItem.name
    this.itemView.setOnClickListener { onItemClicked(patientItem) }
  }

  /** The new ui just shows shortened id with just last 3 characters. */
  private fun getTruncatedId(patientItem: PatientListViewModel.PatientItem): String {
    return patientItem.resourceId.takeLast(3)
  }
}
