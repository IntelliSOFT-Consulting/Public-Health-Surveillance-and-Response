package com.icl.nphi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.icl.nphi.databinding.DiseaseHolderBinding
import com.icl.nphi.holders.DiseaseItemViewHolder
import com.icl.nphi.ui.patients.PatientListViewModel

class PatientDiseaseRecyclerViewAdapter(
    private val onItemClicked: (PatientListViewModel.CaseDiseaseData) -> Unit,
) :
    ListAdapter<PatientListViewModel.CaseDiseaseData, DiseaseItemViewHolder>(
        PatientItemDiffCallback()) {

  class PatientItemDiffCallback : DiffUtil.ItemCallback<PatientListViewModel.CaseDiseaseData>() {
    override fun areItemsTheSame(
        oldItem: PatientListViewModel.CaseDiseaseData,
        newItem: PatientListViewModel.CaseDiseaseData,
    ): Boolean = oldItem.logicalId == newItem.logicalId

    override fun areContentsTheSame(
        oldItem: PatientListViewModel.CaseDiseaseData,
        newItem: PatientListViewModel.CaseDiseaseData,
    ): Boolean = oldItem.logicalId == newItem.logicalId
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiseaseItemViewHolder {
    return DiseaseItemViewHolder(
        DiseaseHolderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )
  }

  override fun onBindViewHolder(holder: DiseaseItemViewHolder, position: Int) {
    val item = currentList[position]
    holder.bindTo(item, onItemClicked)
  }
}
