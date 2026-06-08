package com.example.oopsreportapp.data.model

import java.io.Serializable
import java.util.Date

data class Report(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String = "",
    val imageUrl: String? = null,
    val status: String = "Pending",
    val priority: String = "Sedang", // Rendah, Sedang, Darurat
    val adminResponse: String = "",
    val createdAt: Date? = null,
    val readAt: Date? = null,
    val processedAt: Date? = null,
    val completedAt: Date? = null,
    val updatedAt: Date? = null
) : Serializable
