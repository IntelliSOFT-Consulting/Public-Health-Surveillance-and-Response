package com.icl.surveillance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.icl.surveillance.R
import com.icl.surveillance.models.CaseOption

class CaseOptionsAdapter(
    private val items: List<CaseOption>,
    private val onItemClick: (CaseOption) -> Unit
) : RecyclerView.Adapter<CaseOptionsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.title)
        val countContainer: View = view.findViewById(R.id.case_count_container)
        val countText: TextView = view.findViewById(R.id.case_count)

        fun bind(item: CaseOption) {
            title.text = item.title
            if (item.showCount) {
                countContainer.visibility = View.VISIBLE
                countText.text = item.count.toString()
            } else {
                countContainer.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_case_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
