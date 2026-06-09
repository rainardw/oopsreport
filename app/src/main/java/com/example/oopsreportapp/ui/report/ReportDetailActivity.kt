package com.example.oopsreportapp.ui.report

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.oopsreportapp.R
import com.example.oopsreportapp.data.model.Report
import com.example.oopsreportapp.databinding.ActivityReportDetailBinding
import com.example.oopsreportapp.util.SessionManager
import com.example.oopsreportapp.viewmodel.ReportViewModel
import com.example.oopsreportapp.viewmodel.UiState
import com.example.oopsreportapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
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
    private var currentReport: Report? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        
        reportId = intent.getStringExtra(EXTRA_REPORT_ID)
        
        if (reportId == null) {
            Toast.makeText(this, "Data laporan tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnMenu.setOnClickListener {
            showMenuDialog()
        }

        observeViewModel()
        viewModel.loadReportById(reportId!!)
    }

    private fun showMenuDialog() {
        val options = arrayOf("Unduh PDF", "Hapus Laporan")
        AlertDialog.Builder(this)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> currentReport?.let { generatePDF(it) }
                    1 -> {
                        AlertDialog.Builder(this)
                            .setTitle("Hapus Laporan?")
                            .setMessage("Data ini akan dihapus permanen.")
                            .setPositiveButton("Hapus") { _, _ ->
                                reportId?.let { viewModel.deleteReport(it) }
                            }
                            .setNegativeButton("Batal", null)
                            .show()
                    }
                }
            }
            .show()
    }

    private fun displayReportDetails(report: Report) {
        currentReport = report
        binding.apply {
            tvDetailTitle.text = report.title
            tvDetailStatus.text = report.status
            tvDetailDescription.text = report.description
            tvDetailPriority.text = report.priority
            
            tvAdminResponse.text = if (report.adminResponse.isEmpty()) "Belum ada tanggapan." else report.adminResponse
            
            tvLogDate1.text = formatDate(report.createdAt)
            tvLogDate2.text = if (report.completedAt != null) formatDate(report.completedAt) else if (report.processedAt != null) formatDate(report.processedAt) else "-"
            tvLogText2.text = if (report.status == "Selesai") "Laporan Selesai" else if (report.status == "Proses") "Laporan Diproses" else "Menunggu Validasi"

            if (!report.imageUrl.isNullOrEmpty()) {
                tvNoImage.visibility = View.GONE
                ivDetailImage.visibility = View.VISIBLE
                try {
                    val imageBytes = Base64.decode(report.imageUrl, Base64.DEFAULT)
                    Glide.with(this@ReportDetailActivity)
                        .asBitmap()
                        .load(imageBytes)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(ivDetailImage)
                } catch (e: Exception) {
                    ivDetailImage.visibility = View.GONE
                    tvNoImage.visibility = View.VISIBLE
                }
            } else {
                ivDetailImage.visibility = View.GONE
                tvNoImage.visibility = View.VISIBLE
            }
        }
    }

    private fun generatePDF(report: Report) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        titlePaint.textSize = 24f
        titlePaint.isFakeBoldText = true
        titlePaint.textAlign = Paint.Align.CENTER
        canvas.drawText("LAPORAN KERUSAKAN FASILITAS", 297f, 60f, titlePaint)
        
        paint.textSize = 12f
        canvas.drawLine(50f, 80f, 545f, 80f, paint)

        var y = 120f
        val x = 50f
        val lineSpacing = 30f

        fun drawField(label: String, value: String) {
            paint.isFakeBoldText = true
            canvas.drawText("$label: ", x, y, paint)
            paint.isFakeBoldText = false
            canvas.drawText(value, x + 120f, y, paint)
            y += lineSpacing
        }

        drawField("ID Laporan", report.id.take(8).uppercase())
        drawField("Nama Pelapor", report.userName)
        drawField("Judul Laporan", report.title)
        drawField("Kategori", report.category)
        drawField("Lokasi", report.location)
        drawField("Prioritas", report.priority)
        drawField("Status Akhir", report.status)
        drawField("Tanggal Lapor", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(report.createdAt ?: java.util.Date()))
        
        y += 10f
        paint.isFakeBoldText = true
        canvas.drawText("Deskripsi Keluhan:", x, y, paint); y += 25f
        paint.isFakeBoldText = false
        
        val description = report.description
        val words = description.split(" ")
        var line = ""
        for (word in words) {
            if (paint.measureText("$line $word") < 480f) {
                line += "$word "
            } else {
                canvas.drawText(line, x + 20f, y, paint)
                y += 20f
                line = "$word "
            }
        }
        canvas.drawText(line, x + 20f, y, paint)
        
        y += 40f
        paint.isFakeBoldText = true
        canvas.drawText("Tanggapan Administrator:", x, y, paint); y += 25f
        paint.isFakeBoldText = false
        canvas.drawText(if (report.adminResponse.isEmpty()) "Dalam tahap peninjauan." else report.adminResponse, x + 20f, y, paint)

        // Draw Image if exists
        if (!report.imageUrl.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(report.imageUrl, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap != null) {
                    y += 50f
                    if (y + 200 > 800) { // Check if space is enough, otherwise new page or just stop
                         // Simplification: just draw it if it fits
                    }
                    val scaledWidth = 300f
                    val scaledHeight = (bitmap.height.toFloat() / bitmap.width.toFloat()) * scaledWidth
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth.toInt(), scaledHeight.toInt(), true)
                    canvas.drawBitmap(scaledBitmap, x, y, paint)
                }
            } catch (e: Exception) {
                // Skip image if fails
            }
        }

        pdfDocument.finishPage(page)

        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Laporan_Oops_${report.id.take(5)}.pdf")
        try {
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            
            val contentUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(intent, "Buka dengan aplikasi PDF:"))
            
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal ekspor PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupAdminPanel(report: Report) {
        val isAdmin = sessionManager.getUserRole().equals("admin", ignoreCase = true)
        if (isAdmin && report.status != "Selesai") {
            binding.layoutAdminAction.visibility = View.VISIBLE
            binding.btnProcess.setOnClickListener {
                val response = binding.etResponse.text.toString().trim()
                if (response.isNotEmpty()) {
                    viewModel.updateReportStatus(report.id, "Proses", response)
                    binding.etResponse.text.clear()
                }
                else Toast.makeText(this, "Tanggapan wajib diisi!", Toast.LENGTH_SHORT).show()
            }
            binding.btnDone.setOnClickListener {
                val response = binding.etResponse.text.toString().trim()
                if (response.isNotEmpty()) {
                    viewModel.updateReportStatus(report.id, "Selesai", response)
                    binding.etResponse.text.clear()
                }
                else Toast.makeText(this, "Tanggapan wajib diisi!", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.layoutAdminAction.visibility = View.GONE
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.singleReportState.collect { state ->
                        if (state is UiState.Success) {
                            displayReportDetails(state.data)
                            setupAdminPanel(state.data)
                            if (sessionManager.getUserRole().equals("admin", ignoreCase = true)) {
                                viewModel.markAsRead(state.data.id)
                            }
                        }
                    }
                }
                launch {
                    viewModel.deleteState.collectLatest { state ->
                        if (state is UiState.Success) finish()
                    }
                }
                launch {
                    viewModel.updateState.collectLatest { state ->
                        if (state is UiState.Success) {
                            Toast.makeText(this@ReportDetailActivity, state.data, Toast.LENGTH_SHORT).show()
                        } else if (state is UiState.Error) {
                            Toast.makeText(this@ReportDetailActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun formatDate(date: java.util.Date?): String {
        if (date == null) return "-"
        return SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(date)
    }

    override fun onDestroy() { super.onDestroy(); _binding = null }
    
    companion object { const val EXTRA_REPORT_ID = "extra_report_id" }
}
