package com.example.oopsreportapp.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.oopsreportapp.data.model.Report
import com.example.oopsreportapp.databinding.ItemReportBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ReportAdapter(
    private val onItemClick: (Report) -> Unit
) : ListAdapter<Report, ReportAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val binding = ItemReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReportViewHolder(
        private val binding: ItemReportBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(report: Report) {
            binding.tvTitle.text = report.title
            binding.tvDescription.text = report.description
            binding.tvLocation.text = report.location
            binding.tvCategory.text = report.category
            binding.tvStatus.text = report.status
            binding.tvDate.text = formatDate(report.createdAt)

            // Badge Prioritas
            binding.tvPriorityBadge.text = report.priority
            val priorityColor = when (report.priority) {
                "Darurat" -> "#F44336" // Merah
                "Sedang" -> "#FF9800"  // Orange
                else -> "#4CAF50"      // Hijau
            }
            binding.tvPriorityBadge.setBackgroundColor(Color.parseColor(priorityColor))

            // Warna Status
            val statusColor = when (report.status) {
                "Pending" -> "#FF9800"
                "Proses" -> "#2196F3"
                "Selesai" -> "#4CAF50"
                else -> "#757575"
            }
            binding.tvStatus.setTextColor(Color.parseColor(statusColor))

            itemView.setOnClickListener { onItemClick(report) }
        }

        private fun formatDate(date: java.util.Date?): String {
            if (date == null) return "-"
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            return sdf.format(date)
        }
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<Report>() {
        override fun areItemsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Report, newItem: Report): Boolean {
            return oldItem == newItem
        }
    }
}
