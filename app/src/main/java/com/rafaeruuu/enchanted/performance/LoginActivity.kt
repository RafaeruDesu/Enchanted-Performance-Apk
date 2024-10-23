package com.rafaeruuu.enchanted.performance

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rafaeruuu.enchanted.performance.ui.GovernorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var sharedPreferences: SharedPreferences

    // Hardcoded users and their passwords
    private val userPasswords = mapOf(
        "Fox" to "kitsune",
        "Rafaeru" to "Raphiel",
        "Tester" to "Meong",
        "user" to "RafaeruUPDATE",
        "Perma-Tester" to "Ryuu"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        val btnTelegram: Button = findViewById(R.id.btnTelegram)

        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        btnTelegram.setOnClickListener {
            // Intent to open Telegram
            val telegramIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://t.me/rafaeruuu") // Telegram link
            }
            startActivity(telegramIntent)
        }

        // Handle login button click
        btnLogin.setOnClickListener {
            // Tambahkan pengecekan koneksi internet
            if (!isInternetAvailable()) {
                Toast.makeText(this, "No internet connection. Please check your connection.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateLogin(username, password)) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                // Check for time user
                if (username == "Tester") {
                    // Set target date and time
                    saveTargetDateAndTime(2024, 10, 23, 24, 15)
                }
                else if (username == "user") {
                    // Set target date and time
                    saveTargetDateAndTime(2024, 11, 1, 24, 1)
                }


                // Check if login has expired
                if (isLoginExpired()) {
                    Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show()
                    logout() // Logout the user if session expired
                    return@setOnClickListener
                }

                // Pass the username to MainActivity
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("USERNAME", username)
                startActivity(intent)
                finish() // Close LoginActivity
            } else {
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Function to validate login
    private fun validateLogin(username: String, password: String): Boolean {
        return userPasswords[username] == password
    }

    // Function to check if internet is available
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            return networkInfo.isConnected
        }
    }


    // Function to save target date and time to SharedPreferences
    private fun saveTargetDateAndTime(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt("TARGET_YEAR", year)
        editor.putInt("TARGET_MONTH", month)
        editor.putInt("TARGET_DAY", day)
        editor.putInt("TARGET_HOUR", hour)
        editor.putInt("TARGET_MINUTE", minute)
        editor.putLong("LOGIN_TIME", System.currentTimeMillis()) // Save current login time
        editor.apply()
    }

    // Function to check if login has expired (based on target date and time)
    private fun isLoginExpired(): Boolean {
        val targetDate = getTargetDateAndTime()
        val currentTime = Calendar.getInstance()
        return currentTime.after(targetDate) // Check if current time is after target date
    }

    // Function to get the target date and time (returns Calendar object)
    private fun getTargetDateAndTime(): Calendar {
        val year = sharedPreferences.getInt("TARGET_YEAR", 0)
        val month = sharedPreferences.getInt("TARGET_MONTH", 0)
        val day = sharedPreferences.getInt("TARGET_DAY", 0)
        val hour = sharedPreferences.getInt("TARGET_HOUR", 0)
        val minute = sharedPreferences.getInt("TARGET_MINUTE", 0)

        return if (year != 0 && month != 0 && day != 0) {
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1) // Month is 0-indexed in Calendar
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                timeZone = java.util.TimeZone.getTimeZone("GMT+8") // Set timezone to GMT+8
            }
        } else {
            Calendar.getInstance().apply {
                timeZone = java.util.TimeZone.getTimeZone("GMT+8") // Return current time in GMT+8
            }
        }
    }

    // Function to logout the user
    private fun logout() {
        val editor = sharedPreferences.edit()
        editor.clear() // Clear saved data
        editor.apply()

        // Redirect to the login screen
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close current activity
    }
}

