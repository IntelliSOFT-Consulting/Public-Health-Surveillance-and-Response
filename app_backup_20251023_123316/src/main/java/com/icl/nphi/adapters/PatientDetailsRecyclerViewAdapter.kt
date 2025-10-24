package com.icl.nphi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.icl.nphi.databinding.CaseDetailBinding
import com.icl.nphi.holders.PatientDetailItemViewHolder
import com.icl.nphi.ui.patients.PatientListViewModel

class PatientDetailsRecyclerViewAdapter(
    private val onItemClicked: (PatientListViewModel.EncounterItem) -> Unit,
) :
    ListAdapter<PatientListViewModel.EncounterItem, PatientDetailItemViewHolder>(
        PatientItemDiffCallback()) {

  class PatientItemDiffCallback : DiffUtil.ItemCallback<PatientListViewModel.EncounterItem>() {
    override fun areItemsTheSame(
        oldItem: PatientListViewModel.EncounterItem,
        newItem: PatientListViewModel.EncounterItem,
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: PatientListViewModel.EncounterItem,
        newItem: PatientListViewModel.EncounterItem,
    ): Boolean = oldItem.id == newItem.id
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientDetailItemViewHolder {
    return PatientDetailItemViewHolder(
        CaseDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )
  }

  override fun onBindViewHolder(holder: PatientDetailItemViewHolder, position: Int) {
    val item = currentList[position]
    holder.bindTo(item, onItemClicked)
  }
}
