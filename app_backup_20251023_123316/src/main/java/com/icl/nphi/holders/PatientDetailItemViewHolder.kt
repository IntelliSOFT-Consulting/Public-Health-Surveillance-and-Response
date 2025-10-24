package com.icl.nphi.holders

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.icl.nphi.databinding.CaseDetailBinding
import com.icl.nphi.ui.patients.PatientListViewModel

class PatientDetailItemViewHolder(binding: CaseDetailBinding) :
    RecyclerView.ViewHolder(binding.root) {
  private val status: TextView = binding.status
  private val reason: TextView = binding.reason

  fun bindTo(
      patientItem: PatientListViewModel.EncounterItem,
      onItemClicked: (PatientListViewModel.EncounterItem) -> Unit,
  ) {
    this.status.text = patientItem.status
    this.reason.text = patientItem.reasonCode
    this.itemView.setOnClickListener { onItemClicked(patientItem) }
  }
}
