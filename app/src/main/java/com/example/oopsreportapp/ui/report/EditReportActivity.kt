package com.example.oopsreportapp.ui.report

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.oopsreportapp.data.model.Report
import com.example.oopsreportapp.databinding.ActivityEditReportBinding
import com.example.oopsreportapp.util.SessionManager
import com.example.oopsreportapp.viewmodel.ReportViewModel
import com.example.oopsreportapp.viewmodel.UiState
import com.example.oopsreportapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class EditReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditReportBinding
    private val viewModel: ReportViewModel by viewModels {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var sessionManager: SessionManager
    private var savedImagePath: String? = null
    private var currentReport: Report? = null

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            saveImageToInternalStorage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        currentReport = intent.getSerializableExtra("EXTRA_REPORT") as? Report

        if (currentReport == null) {
            Toast.makeText(this, "Data laporan tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupDropdowns()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUI() {
        currentReport?.let { report ->
            binding.etTitle.setText(report.title)
            binding.etCategory.setText(report.category, false)
            binding.etLocation.setText(report.location, false)
            binding.etPriority.setText(report.priority, false)
            binding.etDescription.setText(report.description)

            if (!report.imageUrl.isNullOrEmpty()) {
                binding.ivPreview.visibility = View.VISIBLE
                try {
                    val imageBytes = Base64.decode(report.imageUrl, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.ivPreview.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    binding.ivPreview.visibility = View.GONE
                }
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "IMG_EDIT_${UUID.randomUUID()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            savedImagePath = file.absolutePath
            binding.ivPreview.visibility = View.VISIBLE
            binding.ivPreview.setImageURI(Uri.fromFile(file))
            
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getBase64Image(path: String?): String? {
        if (path == null) return currentReport?.imageUrl
        return try {
            val file = File(path)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            
            val scaledBitmap = if (bitmap.width > 800) {
                Bitmap.createScaledBitmap(bitmap, 800, (bitmap.height * (800.0 / bitmap.width)).toInt(), true)
            } else {
                bitmap
            }
            
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()
            
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            currentReport?.imageUrl
        }
    }

    private fun setupDropdowns() {
        val categories = arrayOf("Fasilitas", "Keamanan", "Kebersihan")
        val locations = arrayOf("Gedung A", "Gedung B", "Gedung C", "D", "E", "Lainnya")
        val priorities = arrayOf("Rendah", "Sedang", "Darurat")

        binding.etCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, categories))
        binding.etLocation.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, locations))
        binding.etPriority.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, priorities))
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener { finish() }
        
        binding.btnPickImage.setOnClickListener {
            launcherGallery.launch("image/*")
        }

        binding.btnUpdate.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val category = binding.etCategory.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val priority = binding.etPriority.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            if (title.isEmpty() || category.isEmpty() || location.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pValue = when(priority) {
                "Darurat" -> 1
                "Sedang" -> 2
                else -> 3
            }

            val base64Image = getBase64Image(savedImagePath)

            val updatedReport = currentReport?.copy(
                title = title,
                category = category,
                location = location,
                priority = if (priority.isEmpty()) "Sedang" else priority,
                priorityValue = pValue,
                description = description,
                imageUrl = base64Image
            )

            if (updatedReport != null) {
                viewModel.updateReport(updatedReport, base64Image)
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.submitState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.btnUpdate.isEnabled = false
                            binding.btnUpdate.text = "Memperbarui..."
                        }
                        is UiState.Success -> {
                            Toast.makeText(this@EditReportActivity, "Laporan diperbarui!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        is UiState.Error -> {
                            binding.btnUpdate.isEnabled = true
                            binding.btnUpdate.text = "SIMPAN PERUBAHAN"
                            Toast.makeText(this@EditReportActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
