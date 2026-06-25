package com.example.oopsreportapp.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.oopsreportapp.databinding.ActivityEditProfileBinding
import com.example.oopsreportapp.util.SessionManager
import com.example.oopsreportapp.viewmodel.AuthViewModel
import com.example.oopsreportapp.viewmodel.UiState
import kotlinx.coroutines.launch

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        val userId = sessionManager.getUserId() ?: ""
        viewModel.getUser(userId)

        binding.btnSave.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            intent.putExtra("IS_UPDATING", true)
            viewModel.updateProfile(userId, name, phone)
        }

        binding.btnChangePassword.setOnClickListener {
            val intent = android.content.Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnSave.isEnabled = false
                        }
                        is UiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnSave.isEnabled = true
                            
                            val user = state.data
                            // Set initial values if empty
                            if (binding.etFullName.text.isEmpty()) {
                                binding.etFullName.setText(user.name)
                            }
                            if (binding.etPhone.text.isEmpty()) {
                                binding.etPhone.setText(user.phone)
                            }

                            sessionManager.updateUserName(user.name)
                            
                            if (intent.getBooleanExtra("IS_UPDATING", false)) {
                                Toast.makeText(this@EditProfileActivity, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                                intent.putExtra("IS_UPDATING", false)
                            }
                        }
                        is UiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnSave.isEnabled = true
                            Toast.makeText(this@EditProfileActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
