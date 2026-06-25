package com.example.oopsreportapp.data.repository

import com.example.oopsreportapp.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                val snapshot = db.collection("users").document(firebaseUser.uid).get().await()
                val user = snapshot.toObject(User::class.java) ?: User(
                    id = firebaseUser.uid,
                    name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = email,
                    // Konsisten menggunakan lowercase untuk internal logic
                    role = if (email.contains("admin", ignoreCase = true)) "admin" else "student"
                )
                Result.success(user)
            } else {
                Result.failure(Exception("User tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String, user: User): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                // Pastikan role disimpan dalam format yang kita inginkan (misal lowercase)
                val finalUser = user.copy(
                    id = firebaseUser.uid,
                    role = user.role.lowercase()
                )
                db.collection("users").document(firebaseUser.uid).set(finalUser).await()
                Result.success(firebaseUser.uid)
            } else {
                Result.failure(Exception("Gagal mendaftarkan akun"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            db.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserData(userId: String): Result<User> {
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            val user = snapshot.toObject(User::class.java)
            if (user != null) Result.success(user)
            else Result.failure(Exception("User data not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Sesi berakhir, silakan login kembali"))
            val email = user.email ?: return Result.failure(Exception("Email tidak ditemukan"))

            // Re-authenticate user
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPassword)
            user.reauthenticate(credential).await()

            // Update password
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}