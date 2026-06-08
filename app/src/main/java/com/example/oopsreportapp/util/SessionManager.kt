package com.example.oopsreportapp.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("OopsReportPrefs", Context.MODE_PRIVATE)

    fun saveUserSession(userId: String, userName: String, role: String) {
        prefs.edit().apply {
            putString("USER_ID", userId)
            putString("USER_NAME", userName)
            putString("USER_ROLE", role)
            putBoolean("IS_LOGGED_IN", true)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("IS_LOGGED_IN", false)
    fun getUserId(): String? = prefs.getString("USER_ID", null)
    fun getUserName(): String? = prefs.getString("USER_NAME", null)
    fun getUserRole(): String? = prefs.getString("USER_ROLE", "student")

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
