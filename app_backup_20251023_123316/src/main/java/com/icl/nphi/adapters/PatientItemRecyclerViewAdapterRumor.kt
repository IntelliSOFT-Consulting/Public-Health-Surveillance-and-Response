package com.icl.nphi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.icl.nphi.databinding.RumorItemViewBinding
import com.icl.nphi.holders.RumorItemViewHolder
import com.icl.nphi.ui.patients.PatientListViewModel.RumorItem


class PatientItemRecyclerViewAdapterRumor(
    private val onItemClicked: (RumorItem) -> Unit,
) :
    ListAdapter<RumorItem, RumorItemViewHolder>(
        PatientItemDiffCallback()
    ) {

    class PatientItemDiffCallback : DiffUtil.ItemCallback<RumorItem>() {
        override fun areItemsTheSame(
            oldItem: RumorItem,
            newItem: RumorItem,
        ): Boolean = oldItem.resourceId == newItem.resourceId

        override fun areContentsTheSame(
            oldItem: RumorItem,
            newItem: RumorItem,
        ): Boolean = oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RumorItemViewHolder {
        return RumorItemViewHolder(
            RumorItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: RumorItemViewHolder, position: Int) {
        val item = currentList[position]
        holder.bindTo(item, onItemClicked)
    }
}
