package com.example.oopsreportapp.data.repository

import com.example.oopsreportapp.data.model.User
import kotlinx.coroutines.delay

class AuthRepository {

    suspend fun login(email: String, password: String): Result<User> {
        delay(1000)
        return if (email.isNotEmpty() && password.length >= 6) {
            val role = if (email.contains("admin")) "admin" else "student"
            // Mengambil nama dari email (contoh: ametys@gmail.com -> Ametys)
            val nameFromEmail = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            
            val user = User(
                id = "user-${System.currentTimeMillis()}",
                name = nameFromEmail,
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
            Result.success("user-${System.currentTimeMillis()}")
        } else {
            Result.failure(Exception("Data tidak valid"))
        }
    }
}
