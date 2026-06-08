package com.example.oopsreportapp.data.source.local.dao

import androidx.room.*
import com.example.oopsreportapp.data.source.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    // Admin: Urgent first, then by date
    @Query("""
        SELECT * FROM reports 
        ORDER BY 
            CASE WHEN priority = 'Darurat' THEN 1 WHEN priority = 'Sedang' THEN 2 ELSE 3 END ASC, 
            createdAt DESC
    """)
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE userId = :userId ORDER BY createdAt DESC")
    fun getReportsByUser(userId: String): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE id = :reportId")
    suspend fun getReportById(reportId: String): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Delete
    suspend fun deleteReport(report: ReportEntity)

    @Query("DELETE FROM reports WHERE id = :reportId")
    suspend fun deleteReportById(reportId: String)
}
