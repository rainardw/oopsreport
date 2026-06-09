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
import com.example.oopsreportapp.databinding.ActivityCreateReportBinding
import com.example.oopsreportapp.util.SessionManager
import com.example.oopsreportapp.viewmodel.ReportViewModel
import com.example.oopsreportapp.viewmodel.UiState
import com.example.oopsreportapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.UUID

class CreateReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateReportBinding
    private val viewModel: ReportViewModel by viewModels {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var sessionManager: SessionManager
    private var savedImagePath: String? = null

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            saveImageToInternalStorage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupDropdowns()
        setupClickListeners()
        observeViewModel()
    }

    private fun saveImageToInternalStorage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "IMG_${UUID.randomUUID()}.jpg"
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

    // Fungsi baru untuk mengubah gambar ke Base64 (Plan B)
    private fun getBase64Image(path: String?): String? {
        if (path == null) return null
        return try {
            val file = File(path)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            
            // Resize gambar jika terlalu besar (opsional tapi disarankan)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 800, (bitmap.height * (800.0 / bitmap.width)).toInt(), true)
            
            val outputStream = ByteArrayOutputStream()
            // Kompresi kualitas ke 60% agar ukuran string tidak terlalu besar
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()
            
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    private fun setupDropdowns() {
        val categories = arrayOf("Fasilitas", "Kebersihan", "Keamanan", "Lainnya")
        val locations = arrayOf("Gedung A", "Gedung B", "Gedung C", "Gedung D", "Gedung F")
        val priorities = arrayOf("Rendah", "Sedang", "Darurat")

        binding.etCategory.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, categories))
        binding.etLocation.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, locations))
        binding.etPriority.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, priorities))
    }

    private fun setupClickListeners() {
        binding.btnPickImage.setOnClickListener {
            launcherGallery.launch("image/*")
        }

        binding.btnSubmit.setOnClickListener {
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

            // Ubah gambar ke Base64 string sebelum dikirim
            val base64Image = getBase64Image(savedImagePath)

            val report = Report(
                id = UUID.randomUUID().toString(),
                userId = sessionManager.getUserId() ?: "",
                userName = sessionManager.getUserName() ?: "",
                title = title,
                category = category,
                location = location,
                priority = if (priority.isEmpty()) "Sedang" else priority,
                priorityValue = pValue,
                description = description,
                status = "Pending",
                createdAt = Date()
            )

            // Kirim laporan dengan string gambar (Base64)
            viewModel.createReport(report, base64Image)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.submitState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.btnSubmit.isEnabled = false
                            binding.btnSubmit.text = "Mengirim..."
                        }
                        is UiState.Success -> {
                            Toast.makeText(this@CreateReportActivity, "Laporan terkirim!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        is UiState.Error -> {
                            binding.btnSubmit.isEnabled = true
                            binding.btnSubmit.text = "Kirim Laporan"
                            Toast.makeText(this@CreateReportActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}