package com.example.oopsreportapp.ui.report

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import java.util.Date
import java.util.UUID

class CreateReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateReportBinding
    private val viewModel: ReportViewModel by viewModels {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var sessionManager: SessionManager
    private var currentImageUri: Uri? = null

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            binding.ivPreview.visibility = View.VISIBLE
            binding.ivPreview.setImageURI(uri)
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

    private fun setupDropdowns() {
        val categories = arrayOf("Fasilitas", "Kebersihan", "Keamanan", "Lainnya")
        val locations = arrayOf("Gedung A", "Gedung B", "Gedung C", "Gedung D", "Gedung F")
        val priorities = arrayOf("Rendah", "Sedang", "Darurat")

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        binding.etCategory.setAdapter(categoryAdapter)

        val locationAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, locations)
        binding.etLocation.setAdapter(locationAdapter)

        val priorityAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, priorities)
        binding.etPriority.setAdapter(priorityAdapter)
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

            val report = Report(
                id = UUID.randomUUID().toString(),
                userId = sessionManager.getUserId() ?: "",
                userName = sessionManager.getUserName() ?: "",
                title = title,
                category = category,
                location = location,
                priority = if (priority.isEmpty()) "Sedang" else priority,
                description = description,
                imageUrl = currentImageUri?.toString(),
                status = "Pending",
                createdAt = Date()
            )

            viewModel.createReport(report)
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
                            Toast.makeText(this@CreateReportActivity, "Laporan berhasil dikirim", Toast.LENGTH_SHORT).show()
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
