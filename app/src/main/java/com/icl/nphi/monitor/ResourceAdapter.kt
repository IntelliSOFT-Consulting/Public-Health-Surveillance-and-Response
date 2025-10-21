package com.icl.nphi.monitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.fhir.datacapture.extensions.logicalId
import com.icl.nphi.R
import org.hl7.fhir.r4.model.Resource

class ResourceAdapter : ListAdapter<Resource, ResourceAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typeText: TextView = itemView.findViewById(R.id.tvResourceType)
        private val idText: TextView = itemView.findViewById(R.id.tvResourceId)

        fun bind(resource: Resource) {
            typeText.text = resource.resourceType.name
            idText.text = resource.logicalId
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_resource_simple, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Resource>() {
            override fun areItemsTheSame(oldItem: Resource, newItem: Resource): Boolean {
                return oldItem.logicalId == newItem.logicalId
            }

            override fun areContentsTheSame(oldItem: Resource, newItem: Resource): Boolean {
                return oldItem.logicalId == newItem.logicalId
            }
        }
    }
}