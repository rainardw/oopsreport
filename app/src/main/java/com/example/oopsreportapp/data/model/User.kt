package com.example.oopsreportapp.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "student",
    val photoUrl: String = ""
)
