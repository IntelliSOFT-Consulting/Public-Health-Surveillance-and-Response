package com.icl.nphi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.icl.nphi.databinding.LabHolderBinding
import com.icl.nphi.holders.LabItemViewHolder
import com.icl.nphi.ui.patients.PatientListViewModel


class LabRecyclerViewAdapter(
  private val onItemClicked: (PatientListViewModel.CaseLabResultsData) -> Unit,
) :
  ListAdapter<PatientListViewModel.CaseLabResultsData, LabItemViewHolder>(
    PatientItemDiffCallback()) {
  
  class PatientItemDiffCallback : DiffUtil.ItemCallback<PatientListViewModel.CaseLabResultsData>() {
    override fun areItemsTheSame(
      oldItem: PatientListViewModel.CaseLabResultsData,
      newItem: PatientListViewModel.CaseLabResultsData,
    ): Boolean = oldItem.logicalId == newItem.logicalId
    
    override fun areContentsTheSame(
      oldItem: PatientListViewModel.CaseLabResultsData,
      newItem: PatientListViewModel.CaseLabResultsData,
    ): Boolean = oldItem.logicalId == newItem.logicalId
  }
  
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabItemViewHolder {
    return LabItemViewHolder(
      LabHolderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )
  }
  
  override fun onBindViewHolder(holder: LabItemViewHolder, position: Int) {
    val item = currentList[position]
    holder.bindTo(item, onItemClicked)
  }
}
