package com.example.oopsreportapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.oopsreportapp.R
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
            binding.tvDesc.text = report.description
            binding.tvLocation.text = report.location
            binding.tvStatus.text = report.status
            binding.tvDate.text = formatDate(report.createdAt)

            val context = itemView.context

            // Badge Prioritas
            binding.tvPriority.text = report.priority
            val priorityColor = when (report.priority) {
                "Darurat" -> R.color.status_red
                "Sedang" -> R.color.status_orange
                else -> R.color.status_green
            }
            binding.tvPriority.setBackgroundColor(ContextCompat.getColor(context, priorityColor))

            // Warna Status (Background matching item_report.xml style)
            val statusColor = when (report.status) {
                "Pending" -> R.color.status_orange
                "Proses" -> R.color.status_blue
                "Selesai" -> R.color.status_green
                else -> R.color.text_secondary
            }
            binding.tvStatus.setBackgroundColor(ContextCompat.getColor(context, statusColor))
            binding.tvStatus.setTextColor(ContextCompat.getColor(context, if (report.status == "Proses" || report.status == "Selesai" && report.priority == "Darurat") R.color.white else R.color.black))
            
            // Fix text color logic for status
            if (report.status == "Proses") {
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.black))
            }

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
