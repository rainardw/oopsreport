package com.example.oopsreportapp.data.repository

import com.example.oopsreportapp.data.model.User
import kotlinx.coroutines.delay

class AuthRepository {

    suspend fun login(email: String, password: String): Result<User> {
        delay(1000)
        return if (email.isNotEmpty() && password.length >= 6) {
            val role = if (email.contains("admin")) "admin" else "student"
            
            // Ambil nama depan saja (contoh: amethis.siregar@gmail.com -> Amethis)
            val cleanName = email.substringBefore("@")
                .substringBefore(".")
                .substringBefore("_")
                .replaceFirstChar { it.uppercase() }
            
            // Gunakan format ID yang konsisten (Garis bawah _)
            val userId = "user_" + email.lowercase().trim().hashCode()
            
            val user = User(
                id = userId,
                name = cleanName,
                email = email,
                role = role
            )
            Result.success(user)
        } else {
            Result.failure(Exception("Email atau password tidak valid"))
        }
    }

    suspend fun register(email: String, password: String, user: User): Result<String> {
        delay(1000)
        return if (email.contains("@") && password.length >= 6) {
            // Samakan format ID dengan Login
            Result.success("user_" + email.lowercase().trim().hashCode())
        } else {
            Result.failure(Exception("Data tidak valid"))
        }
    }
}
