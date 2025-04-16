package com.icl.surveillance.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.icl.surveillance.databinding.PatientListItemViewBinding
import com.icl.surveillance.holders.PatientItemViewHolder
import com.icl.surveillance.ui.patients.PatientListViewModel

class PatientItemRecyclerViewAdapter(
    private val onItemClicked: (PatientListViewModel.PatientItem) -> Unit,
) :
    ListAdapter<PatientListViewModel.PatientItem, PatientItemViewHolder>(
        PatientItemDiffCallback()) {

  class PatientItemDiffCallback : DiffUtil.ItemCallback<PatientListViewModel.PatientItem>() {
    override fun areItemsTheSame(
        oldItem: PatientListViewModel.PatientItem,
        newItem: PatientListViewModel.PatientItem,
    ): Boolean = oldItem.resourceId == newItem.resourceId

    override fun areContentsTheSame(
        oldItem: PatientListViewModel.PatientItem,
        newItem: PatientListViewModel.PatientItem,
    ): Boolean = oldItem.id == newItem.id
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientItemViewHolder {
    return PatientItemViewHolder(
        PatientListItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false),
    )
  }

  override fun onBindViewHolder(holder: PatientItemViewHolder, position: Int) {
    val item = currentList[position]
    holder.bindTo(item, onItemClicked)
  }
}
