package com.example.oopsreportapp.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userName: String,
    val title: String,
    val description: String,
    val category: String,
    val location: String,
    val imageUrl: String?,
    val status: String,
    val priority: String,
    val adminResponse: String,
    val createdAt: Long?,
    val readAt: Long? = null,
    val processedAt: Long? = null,
    val completedAt: Long? = null,
    val updatedAt: Long?
)
