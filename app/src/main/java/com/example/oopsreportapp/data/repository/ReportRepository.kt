package com.example.oopsreportapp.data.repository

import com.example.oopsreportapp.data.model.Report
import com.example.oopsreportapp.data.source.local.dao.ReportDao
import com.example.oopsreportapp.data.source.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class ReportRepository(private val reportDao: ReportDao) {

    fun getReportsByUser(userId: String): Flow<List<Report>> {
        return reportDao.getReportsByUser(userId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun getAllReports(): Flow<List<Report>> {
        return reportDao.getAllReports().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun getReportById(reportId: String): Report? {
        return reportDao.getReportById(reportId)?.toModel()
    }

    suspend fun createReport(report: Report): String {
        reportDao.insertReport(report.toEntity())
        return report.id
    }

    suspend fun updateReportStatus(reportId: String, status: String, response: String) {
        val entity = reportDao.getReportById(reportId)
        entity?.let {
            val currentTime = System.currentTimeMillis()
            val updated = it.copy(
                status = status,
                adminResponse = response,
                processedAt = if (status == "Proses") currentTime else it.processedAt,
                completedAt = if (status == "Selesai") currentTime else it.completedAt,
                updatedAt = currentTime
            )
            reportDao.updateReport(updated)
        }
    }

    suspend fun markAsRead(reportId: String) {
        val entity = reportDao.getReportById(reportId)
        if (entity != null && entity.readAt == null) {
            val updated = entity.copy(readAt = System.currentTimeMillis())
            reportDao.updateReport(updated)
        }
    }

    suspend fun deleteReport(reportId: String) {
        reportDao.deleteReportById(reportId)
    }

    // Mapper extensions
    private fun ReportEntity.toModel() = Report(
        id = id,
        userId = userId,
        userName = userName,
        title = title,
        description = description,
        category = category,
        location = location,
        imageUrl = imageUrl,
        status = status,
        priority = priority,
        adminResponse = adminResponse,
        createdAt = createdAt?.let { Date(it) },
        readAt = readAt?.let { Date(it) },
        processedAt = processedAt?.let { Date(it) },
        completedAt = completedAt?.let { Date(it) },
        updatedAt = updatedAt?.let { Date(it) }
    )

    private fun Report.toEntity() = ReportEntity(
        id = id,
        userId = userId,
        userName = userName,
        title = title,
        description = description,
        category = category,
        location = location,
        imageUrl = imageUrl,
        status = status,
        priority = priority,
        adminResponse = adminResponse,
        createdAt = createdAt?.time,
        readAt = readAt?.time,
        processedAt = processedAt?.time,
        completedAt = completedAt?.time,
        updatedAt = updatedAt?.time
    )
}
