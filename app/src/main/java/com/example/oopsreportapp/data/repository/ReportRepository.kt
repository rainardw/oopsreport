package com.example.oopsreportapp.data.repository

import com.example.oopsreportapp.data.model.Report
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class ReportRepository {
    private val db = FirebaseFirestore.getInstance()
    private val reportsCollection = db.collection("reports")

    fun getAllReports(): Flow<List<Report>> = callbackFlow {
        val subscription = reportsCollection
            .orderBy("priorityValue", Query.Direction.ASCENDING)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reports = snapshot?.toObjects(Report::class.java) ?: emptyList()
                trySend(reports)
            }
        awaitClose { subscription.remove() }
    }

    fun getReportsByUser(userId: String): Flow<List<Report>> = callbackFlow {
        val subscription = reportsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reports = snapshot?.toObjects(Report::class.java) ?: emptyList()
                trySend(reports)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getReportById(reportId: String): Report? {
        return reportsCollection.document(reportId).get().await().toObject(Report::class.java)
    }

    // Plan B: Langsung simpan report (imageUrl di sini akan berisi string Base64)
    suspend fun createReport(report: Report, base64Image: String?): String {
        val finalReport = if (base64Image != null) {
            report.copy(imageUrl = base64Image)
        } else {
            report
        }
        reportsCollection.document(report.id).set(finalReport).await()
        return report.id
    }

    suspend fun updateReportStatus(reportId: String, status: String, response: String) {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "adminResponse" to response,
            "updatedAt" to Date()
        )
        if (status == "Proses") updates["processedAt"] = Date()
        if (status == "Selesai") updates["completedAt"] = Date()

        reportsCollection.document(reportId).update(updates).await()
    }

    suspend fun markAsRead(reportId: String) {
        try {
            val doc = reportsCollection.document(reportId).get().await()
            if (doc.exists() && doc.get("readAt") == null) {
                reportsCollection.document(reportId).update("readAt", Date()).await()
            }
        } catch (e: Exception) { }
    }

    suspend fun updateReport(report: Report, base64Image: String?): String {
        val finalReport = if (base64Image != null) {
            report.copy(imageUrl = base64Image, updatedAt = Date())
        } else {
            report.copy(updatedAt = Date())
        }
        reportsCollection.document(report.id).set(finalReport).await()
        return report.id
    }

    suspend fun deleteReport(reportId: String) {
        reportsCollection.document(reportId).delete().await()
    }
}