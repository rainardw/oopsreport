package com.example.oopsreportapp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.oopsreportapp.databinding.ActivityLoginBinding
import com.example.oopsreportapp.ui.dashboard.DashboardActivity
import com.example.oopsreportapp.util.SessionManager
import com.example.oopsreportapp.viewmodel.AuthViewModel
import com.example.oopsreportapp.viewmodel.UiState
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        observeViewModel()
        setupClickListeners()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    when (state) {
                        is UiState.Idle -> {
                            binding.btnLogin.isEnabled = true
                            binding.btnLogin.text = "Login"
                        }
                        is UiState.Loading -> {
                            binding.btnLogin.isEnabled = false
                            binding.btnLogin.text = "Loading..."
                        }
                        is UiState.Success -> {
                            val user = state.data
                            sessionManager.saveUserSession(
                                user.id,
                                user.name,
                                user.role
                            )

                            val message = if (user.role == "admin") "Login berhasil sebagai Administrator" else "Login berhasil"
                            Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()

                            startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                            finish()
                        }
                        is UiState.Error -> {
                            binding.btnLogin.isEnabled = true
                            binding.btnLogin.text = "Login"
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan password harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(email, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
