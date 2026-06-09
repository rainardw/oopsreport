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

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Statistik Fakultas Teknik"
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
                        updateUI(state.data)
                    }
                }
            }
        }
    }

    private fun updateUI(reports: List<Report>) {
        val totalAll = reports.size
        val totalPending = reports.count { it.status == "Pending" }
        val totalProses = reports.count { it.status == "Proses" }
        val totalSelesai = reports.count { it.status == "Selesai" }

        binding.tvTotal.text = totalAll.toString()
        binding.tvTotalPending.text = totalPending.toString()
        binding.tvTotalProses.text = totalProses.toString()
        binding.tvTotalSelesai.text = totalSelesai.toString()

        val locationStats = reports.groupBy { it.location }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        binding.layoutStatsContainer.removeAllViews()

        if (locationStats.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Belum ada laporan masuk."
                gravity = android.view.Gravity.CENTER
                setPadding(0, 50, 0, 50)
            }
            binding.layoutStatsContainer.addView(emptyTv)
            return
        }

        val maxCount = locationStats.maxOf { it.second }.toFloat()
        val colors = arrayOf("#6200EE", "#03DAC5", "#FF9800", "#E91E63", "#4CAF50")
        
        locationStats.forEachIndexed { index, (location, count) ->
            val itemContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, 32)
            }

            val labelLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val tvLoc = TextView(this).apply {
                text = location
                textSize = 14f
                setTextColor(Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            }

            val tvCount = TextView(this).apply {
                text = "$count Laporan"
                textSize = 12f
                setTextColor(Color.DKGRAY)
            }

            labelLayout.addView(tvLoc)
            labelLayout.addView(tvCount)

            val barContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(-1, dpToPx(14)).apply { topMargin = 12 }
                setBackgroundColor(Color.parseColor("#EEEEEE"))
                
                val progress = View(context).apply {
                    val weight = (count.toFloat() / maxCount).coerceAtLeast(0.05f)
                    layoutParams = LinearLayout.LayoutParams(0, -1, weight)
                    setBackgroundColor(Color.parseColor(colors[index % colors.size]))
                }
                val empty = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, -1, 1 - (count.toFloat() / maxCount))
                }
                addView(progress)
                addView(empty)
            }

            itemContainer.addView(labelLayout)
            itemContainer.addView(barContainer)
            binding.layoutStatsContainer.addView(itemContainer)
        }
    }

    private fun dpToPx(dp: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
    ).toInt()

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
