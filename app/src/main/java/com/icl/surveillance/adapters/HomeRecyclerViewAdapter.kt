package com.icl.surveillance.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.icl.surveillance.databinding.LandingPageItemBinding
import com.icl.surveillance.ui.home.HomeViewModel


class HomeRecyclerViewAdapter(private val onItemClick: (HomeViewModel.Layout) -> Unit) :
    ListAdapter<HomeViewModel.Layout, LayoutViewHolder>(LayoutDiffUtil()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayoutViewHolder {
        return LayoutViewHolder(
            LandingPageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onItemClick,
        )
    }

    override fun onBindViewHolder(holder: LayoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class LayoutViewHolder(
    val binding: LandingPageItemBinding,
    private val onItemClick: (HomeViewModel.Layout) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(layout: HomeViewModel.Layout) {
        binding.componentLayoutIconImageview.setImageResource(layout.iconId)
        binding.componentLayoutTextView.text =
            binding.componentLayoutTextView.context.getString(layout.textId)
        binding.root.setOnClickListener { onItemClick(layout) }
    }
}

class LayoutDiffUtil : DiffUtil.ItemCallback<HomeViewModel.Layout>() {
    override fun areItemsTheSame(
        oldLayout: HomeViewModel.Layout,
        newLayout: HomeViewModel.Layout,
    ) = oldLayout === newLayout

    override fun areContentsTheSame(
        oldLayout: HomeViewModel.Layout,
        newLayout: HomeViewModel.Layout,
    ) = oldLayout == newLayout
}

