package com.example.oopsreportapp.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.oopsreportapp.data.model.Report
import com.example.oopsreportapp.databinding.ActivityStatsBinding
import com.example.oopsreportapp.viewmodel.ReportViewModel
import com.example.oopsreportapp.viewmodel.UiState
import com.example.oopsreportapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class StatsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatsBinding
    private val viewModel: ReportViewModel by viewModels {
        ViewModelFactory.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            title = "Statistik Laporan"
            setDisplayHomeAsUpEnabled(true)
        }

        observeViewModel()
        viewModel.loadReports("", isAdmin = true)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reportsState.collect { state ->
                    if (state is UiState.Success) {
                        calculateStats(state.data)
                    }
                }
            }
        }
    }

    private fun calculateStats(reports: List<Report>) {
        if (reports.isEmpty()) return

        val totalPending = reports.count { it.status == "Pending" }
        val totalProses = reports.count { it.status == "Proses" }
        val totalSelesai = reports.count { it.status == "Selesai" }

        binding.tvTotalPending.text = "Total Pending: $totalPending"
        binding.tvTotalProses.text = "Total Proses: $totalProses"
        binding.tvTotalSelesai.text = "Total Selesai: $totalSelesai"

        val locationStats = reports.groupBy { it.location }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        binding.layoutStatsContainer.removeAllViews()
        
        val maxWidth = resources.displayMetrics.widthPixels - 200
        val maxCount = locationStats.maxOfOrNull { it.second } ?: 1

        locationStats.forEach { (location, count) ->
            val textView = TextView(this).apply {
                text = "$location: $count Laporan"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setPadding(0, 8, 0, 4)
            }
            
            val bar = View(this).apply {
                val barWidth = (count.toFloat() / maxCount * maxWidth).toInt().coerceAtLeast(30)
                val params = LinearLayout.LayoutParams(barWidth, 30)
                params.setMargins(0, 0, 0, 24)
                layoutParams = params
                setBackgroundColor(Color.parseColor("#6200EE"))
            }

            binding.layoutStatsContainer.addView(textView)
            binding.layoutStatsContainer.addView(bar)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
