package com.icl.nphi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.icl.nphi.databinding.ContactItemBinding
import com.icl.nphi.holders.ContactHolder
import com.icl.nphi.ui.patients.PatientListViewModel.ContactResults


class ContactsAdapter(
    private val onItemClicked: (ContactResults) -> Unit,
) :
    ListAdapter<ContactResults, ContactHolder>(
        PatientItemDiffCallback()) {

    class PatientItemDiffCallback : DiffUtil.ItemCallback<ContactResults>() {
        override fun areItemsTheSame(
            oldItem:ContactResults,
            newItem: ContactResults,
        ): Boolean = oldItem.childId == newItem.childId

        override fun areContentsTheSame(
            oldItem: ContactResults,
            newItem: ContactResults,
        ): Boolean = oldItem.childId == newItem.childId
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactHolder {
        return ContactHolder(
            ContactItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: ContactHolder, position: Int) {
        val item = currentList[position]
        holder.bindTo(item, onItemClicked)
    }
}
