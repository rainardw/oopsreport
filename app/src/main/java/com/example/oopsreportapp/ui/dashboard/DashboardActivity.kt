package com.example.oopsreportapp.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.oopsreportapp.R
import com.example.oopsreportapp.databinding.ActivityDashboardBinding
import com.example.oopsreportapp.ui.adapter.ReportAdapter
import com.example.oopsreportapp.ui.auth.LoginActivity
import com.example.oopsreportapp.ui.report.CreateReportActivity
import com.example.oopsreportapp.ui.report.ReportDetailActivity
import com.example.oopsreportapp.util.SessionManager
import com.example.oopsreportapp.viewmodel.ReportViewModel
import com.example.oopsreportapp.viewmodel.UiState
import com.example.oopsreportapp.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: ReportViewModel by viewModels {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: ReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        sessionManager = SessionManager(this)

        setupUI()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUI() {
        val role = sessionManager.getUserRole() ?: "student"
        val name = sessionManager.getUserName()
        
        val isAdmin = role.equals("admin", ignoreCase = true)
        
        val roleDisplay = if (isAdmin) "\nRole: Administrator" else ""
        binding.tvWelcome.text = "Selamat Datang, $name!$roleDisplay"
        
        binding.fabCreateReport.visibility = if (isAdmin) View.GONE else View.VISIBLE
    }

    private fun setupRecyclerView() {
        adapter = ReportAdapter { report ->
            val intent = Intent(this, ReportDetailActivity::class.java).apply {
                putExtra(ReportDetailActivity.EXTRA_REPORT_ID, report.id)
            }
            startActivity(intent)
        }

        binding.rvReports.layoutManager = LinearLayoutManager(this)
        binding.rvReports.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.fabCreateReport.setOnClickListener {
            startActivity(Intent(this, CreateReportActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        val statsItem = menu?.findItem(R.id.action_stats)
        statsItem?.isVisible = sessionManager.getUserRole().equals("admin", ignoreCase = true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                sessionManager.clearSession()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            R.id.action_stats -> {
                startActivity(Intent(this, StatsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reportsState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is UiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            adapter.submitList(state.data)

                            if (state.data.isEmpty()) {
                                binding.emptyStateLayout.visibility = View.VISIBLE
                                binding.rvReports.visibility = View.GONE
                            } else {
                                binding.emptyStateLayout.visibility = View.GONE
                                binding.rvReports.visibility = View.VISIBLE
                            }
                        }
                        is UiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(this@DashboardActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun loadReports() {
        val userId = sessionManager.getUserId() ?: ""
        val isAdmin = sessionManager.getUserRole().equals("admin", ignoreCase = true)
        viewModel.loadReports(userId, isAdmin = isAdmin)
    }

    override fun onResume() {
        super.onResume()
        loadReports()
    }
}