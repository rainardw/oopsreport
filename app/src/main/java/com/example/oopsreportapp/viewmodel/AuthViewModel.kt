package com.example.oopsreportapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oopsreportapp.data.model.User
import com.example.oopsreportapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _authState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val authState: StateFlow<UiState<User>> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        Log.d("AuthViewModel", "Login requested for: $email")
        viewModelScope.launch {
            _authState.value = UiState.Loading
            val result = repository.login(email, password)
            _authState.value = if (result.isSuccess) {
                Log.d("AuthViewModel", "Login success")
                UiState.Success(result.getOrNull()!!)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Login gagal"
                Log.e("AuthViewModel", "Login error: $error")
                UiState.Error(error)
            }
        }
    }

    fun register(email: String, password: String, user: User) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            val result = repository.register(email, password, user)
            _authState.value = if (result.isSuccess) {
                UiState.Success(user)
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Register gagal")
            }
        }
    }

    fun updateProfile(userId: String, name: String, phone: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            val updates = mapOf(
                "name" to name,
                "phone" to phone
            )
            val result = repository.updateProfile(userId, updates)
            if (result.isSuccess) {
                // Fetch updated user data
                val userResult = repository.getUserData(userId)
                _authState.value = if (userResult.isSuccess) {
                    UiState.Success(userResult.getOrNull()!!)
                } else {
                    UiState.Error("Profile updated but failed to refresh data")
                }
            } else {
                _authState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Update profile gagal")
            }
        }
    }

    fun getUser(userId: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            val result = repository.getUserData(userId)
            _authState.value = if (result.isSuccess) {
                UiState.Success(result.getOrNull()!!)
            } else {
                UiState.Error(result.exceptionOrNull()?.message ?: "Gagal mengambil data user")
            }
        }
    }

    private val _passwordState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val passwordState: StateFlow<UiState<Unit>> = _passwordState.asStateFlow()

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _passwordState.value = UiState.Loading
            val result = repository.changePassword(oldPassword, newPassword)
            if (result.isSuccess) {
                _passwordState.value = UiState.Success(Unit)
            } else {
                _passwordState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Gagal ganti password")
            }
        }
    }
}
