package com.example.oopsreportapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oopsreportapp.data.model.Report
import com.example.oopsreportapp.data.repository.ReportRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ReportViewModel(private val repository: ReportRepository) : ViewModel() {

    private val _reportsState = MutableStateFlow<UiState<List<Report>>>(UiState.Idle)
    val reportsState: StateFlow<UiState<List<Report>>> = _reportsState.asStateFlow()

    private val _singleReportState = MutableStateFlow<UiState<Report>>(UiState.Idle)
    val singleReportState: StateFlow<UiState<Report>> = _singleReportState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val submitState: StateFlow<UiState<String>> = _submitState.asStateFlow()

    private val _updateState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val updateState: StateFlow<UiState<String>> = _updateState.asStateFlow()

    private val _deleteState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val deleteState: StateFlow<UiState<String>> = _deleteState.asStateFlow()

    private var reportsJob: Job? = null

    fun loadReports(userId: String, isAdmin: Boolean = false) {
        reportsJob?.cancel()
        reportsJob = viewModelScope.launch {
            _reportsState.value = UiState.Loading
            val flow = if (isAdmin) {
                repository.getAllReports()
            } else {
                repository.getReportsByUser(userId)
            }
            
            flow.catch { e ->
                _reportsState.value = UiState.Error(e.message ?: "Gagal memuat data")
            }.collect { reports ->
                _reportsState.value = UiState.Success(reports)
            }
        }
    }

    fun loadReportById(reportId: String) {
        viewModelScope.launch {
            _singleReportState.value = UiState.Loading
            try {
                val report = repository.getReportById(reportId)
                if (report != null) {
                    _singleReportState.value = UiState.Success(report)
                } else {
                    _singleReportState.value = UiState.Error("Laporan tidak ditemukan")
                }
            } catch (e: Exception) {
                _singleReportState.value = UiState.Error(e.message ?: "Gagal memuat detail laporan")
            }
        }
    }

    fun createReport(report: Report, base64Image: String?) {
        viewModelScope.launch {
            _submitState.value = UiState.Loading
            try {
                val id = repository.createReport(report, base64Image)
                _submitState.value = UiState.Success(id)
            } catch (e: Exception) {
                _submitState.value = UiState.Error(e.message ?: "Gagal membuat laporan")
            }
        }
    }

    fun updateReportStatus(reportId: String, status: String, response: String) {
        viewModelScope.launch {
            _updateState.value = UiState.Loading
            try {
                repository.updateReportStatus(reportId, status, response)
                loadReportById(reportId) // Refresh data detail
                _updateState.value = UiState.Success("Status berhasil diperbarui")
            } catch (e: Exception) {
                _updateState.value = UiState.Error(e.message ?: "Gagal memperbarui status")
            }
        }
    }

    fun markAsRead(reportId: String) {
        viewModelScope.launch {
            try {
                repository.markAsRead(reportId)
            } catch (e: Exception) { }
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading
            try {
                repository.deleteReport(reportId)
                _deleteState.value = UiState.Success("Laporan berhasil dihapus")
            } catch (e: Exception) {
                _deleteState.value = UiState.Error(e.message ?: "Gagal menghapus laporan")
            }
        }
    }
}
