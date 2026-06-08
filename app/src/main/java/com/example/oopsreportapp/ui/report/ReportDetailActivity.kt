package com.example.oopsreportapp.ui.report

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.oopsreportapp.R
import com.example.oopsreportapp.data.model.Report
import com.example.oopsreportapp.databinding.ActivityReportDetailBinding
import com.example.oopsreportapp.util.SessionManager
import com.example.oopsreportapp.viewmodel.ReportViewModel
import com.example.oopsreportapp.viewmodel.UiState
import com.example.oopsreportapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ReportDetailActivity : AppCompatActivity() {
    private var _binding: ActivityReportDetailBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ReportViewModel by viewModels {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var sessionManager: SessionManager
    private var reportId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        sessionManager = SessionManager(this)
        
        reportId = intent.getStringExtra(EXTRA_REPORT_ID)
        
        if (reportId == null) {
            Toast.makeText(this, "ID laporan tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

        observeViewModel()
        viewModel.loadReportById(reportId!!)
    }

    private fun displayReportDetails(report: Report) {
        binding.apply {
            tvDetailTitle.text = report.title
            tvDetailCategory.text = report.category
            tvDetailStatus.text = report.status
            tvDetailLocation.text = report.location
            tvDetailDescription.text = report.description
            
            // Warna Badge Prioritas
            tvDetailPriority.text = report.priority
            val priorityColor = when (report.priority) {
                "Darurat" -> "#F44336" // Merah
                "Sedang" -> "#FF9800"  // Orange
                else -> "#4CAF50"      // Hijau
            }
            try {
                tvDetailPriority.setBackgroundColor(Color.parseColor(priorityColor))
            } catch (e: Exception) {
                tvDetailPriority.setBackgroundColor(Color.GRAY)
            }

            // Timeline Riwayat Lengkap
            tvLogCreated.text = "• Dibuat: ${formatDate(report.createdAt)}"
            tvLogRead.text = "• Dibaca Admin: ${formatDate(report.readAt)}"
            tvLogProcessed.text = "• Diproses: ${formatDate(report.processedAt)}"
            tvLogCompleted.text = "• Selesai: ${formatDate(report.completedAt)}"

            tvAdminResponse.text = if (report.adminResponse.isEmpty()) "Belum ada respon" else report.adminResponse

            // Gambar Bukti
            if (!report.imageUrl.isNullOrEmpty()) {
                ivDetailImage.visibility = View.VISIBLE
                try {
                    ivDetailImage.setImageURI(Uri.parse(report.imageUrl))
                } catch (e: Exception) {
                    ivDetailImage.setImageResource(R.drawable.ic_launcher_background)
                }
            } else {
                ivDetailImage.visibility = View.GONE
            }
        }
    }

    private fun setupAdminPanel(report: Report) {
        val isAdmin = sessionManager.getUserRole() == "admin"
        if (isAdmin && report.status != "Selesai") {
            binding.layoutAdminAction.visibility = View.VISIBLE
            
            binding.btnProcess.setOnClickListener {
                val response = binding.etResponse.text.toString().trim()
                if (response.isEmpty()) {
                    Toast.makeText(this, "Respon wajib diisi", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateReportStatus(report.id, "Proses", response)
                }
            }

            binding.btnDone.setOnClickListener {
                val response = binding.etResponse.text.toString().trim()
                if (response.isEmpty()) {
                    Toast.makeText(this, "Respon wajib diisi", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateReportStatus(report.id, "Selesai", response)
                }
            }
        } else {
            binding.layoutAdminAction.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.report_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmation()
                true
            }
            R.id.action_download -> {
                Toast.makeText(this, "Fitur PDF akan segera hadir", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Laporan")
            .setMessage("Laporan ini akan dihapus permanen. Lanjutkan?")
            .setPositiveButton("Hapus") { _, _ ->
                reportId?.let { viewModel.deleteReport(it) }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Memuat Data Detail
                launch {
                    viewModel.singleReportState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                            is UiState.Success -> {
                                binding.progressBar.visibility = View.GONE
                                displayReportDetails(state.data)
                                setupAdminPanel(state.data)
                                
                                if (sessionManager.getUserRole() == "admin") {
                                    viewModel.markAsRead(state.data.id)
                                }
                            }
                            is UiState.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(this@ReportDetailActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }
                
                // Pantau Status Hapus
                launch {
                    viewModel.deleteState.collectLatest { state ->
                        if (state is UiState.Success) {
                            Toast.makeText(this@ReportDetailActivity, state.data, Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun formatDate(date: java.util.Date?): String {
        if (date == null) return "-"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(date)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        const val EXTRA_REPORT_ID = "extra_report_id"
    }
}
