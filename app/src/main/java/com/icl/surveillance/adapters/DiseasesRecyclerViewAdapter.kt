package com.icl.surveillance.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.icl.surveillance.databinding.LandingPageItemBinding
import com.icl.surveillance.ui.home.HomeViewModel


class DiseasesRecyclerViewAdapter(private val onItemClick: (HomeViewModel.Diseases) -> Unit) :
    ListAdapter<HomeViewModel.Diseases, DiseaseViewHolder>(DiseaseDiffUtil()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiseaseViewHolder {
        return DiseaseViewHolder(
            LandingPageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onItemClick,
        )
    }

    override fun onBindViewHolder(holder: DiseaseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class DiseaseViewHolder(
    val binding: LandingPageItemBinding,
    private val onItemClick: (HomeViewModel.Diseases) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(layout: HomeViewModel.Diseases) {
        binding.componentLayoutIconImageview.setImageResource(layout.iconId)
        binding.componentLayoutTextView.text =
            binding.componentLayoutTextView.context.getString(layout.textId)
        binding.root.setOnClickListener { onItemClick(layout) }
    }
}

class DiseaseDiffUtil : DiffUtil.ItemCallback<HomeViewModel.Diseases>() {
    override fun areItemsTheSame(
        oldLayout: HomeViewModel.Diseases,
        newLayout: HomeViewModel.Diseases,
    ) = oldLayout === newLayout

    override fun areContentsTheSame(
        oldLayout: HomeViewModel.Diseases,
        newLayout: HomeViewModel.Diseases,
    ) = oldLayout == newLayout
}

