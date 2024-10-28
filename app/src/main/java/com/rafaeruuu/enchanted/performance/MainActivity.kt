package com.rafaeruuu.enchanted.performance

import android.app.ActivityManager
import android.app.Dialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.rafaeruuu.enchanted.performance.ui.GovernorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var cpuGovernorText: TextView
    private lateinit var cpuSpeedText: TextView
    private lateinit var toggleGpuMediatek1: ToggleButton
    private lateinit var toggleGpuMediatek2: ToggleButton
    private lateinit var toggleGpuSnapdragon1: ToggleButton
    private lateinit var toggleGpuSnapdragon2: ToggleButton
    private lateinit var statusroottext: TextView
    private lateinit var gpuGovernorManager: GovernorManager
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var selinuxStatusTextView: TextView
    private lateinit var toggleSelinuxButton: Button
    private lateinit var vibrator: Vibrator
    private lateinit var openSettingsButton: Button
    private lateinit var tvRemainingTime: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private var remainingTimeInSeconds: Long = 0L
    private var handler = Handler(Looper.getMainLooper())
    private var isClockBoosted = false

    private val GPU_MAX_FREQUENCY = "900000000"
    private val GPU_GOVERNOR = "performance"
    private var originalGpuFrequency: String? = null
    private val updateInterval = 1000L
    private val userPasswords = mapOf(
        "Fox" to "kitsune",
        "Rafaeru" to "rafael",
        "Tester" to "tester"
    )
    private val googleApps = listOf(
        // List of Google Apps
        "com.android.chrome",
        "com.google.android.googlequicksearchbox",
        "com.android.vending",
        "com.google.android.gms",
        "com.google.android.apps.maps",
        "com.google.android.gm",
        "com.google.android.youtube",
        "com.google.android.apps.docs",
        "com.google.android.apps.photos",
        "com.google.android.calendar",
        "com.google.android.contacts",
        "com.google.android.apps.tachyon",
        "com.google.android.apps.messaging",
        "com.google.android.apps.keep",
        "com.google.android.apps.translate",
        "com.google.android.apps.magazines",
        "com.google.android.apps.podcasts",
        "com.google.android.apps.walletnfcrel",
        "com.google.android.apps.chromecast.app",
        "com.google.android.apps.fitness",
        "com.google.android.apps.googlevoice",
        "com.google.android.apps.subscriptions.red",
        "com.google.earth",
        "com.google.android.music",
        "com.google.android.videos",
        "com.google.android.apps.meetings",
        "com.google.android.apps.accessibility.soundamplifier",
        "com.google.android.apps.accessibility.voiceaccess",
        "com.google.android.apps.blogger",
        "com.google.android.apps.youtube.music",
        "com.google.android.apps.youtube.kids",
        "com.google.android.apps.books",
        "com.google.android.apps.magazines",
        "com.google.android.apps.wallet",
        "com.google.android.apps.turbo",
        "com.google.android.apps.googleassistant",
        "com.google.android.apps.dynamite",
        "com.google.android.apps.accessibility.braille",
        "com.google.android.apps.subscriptions.red"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        // Initialize views
        statusText = findViewById(R.id.status)
        statusroottext = findViewById(R.id.statusroot)
        cpuGovernorText = findViewById(R.id.cpuGovernorText)
        cpuSpeedText = findViewById(R.id.cpuSpeedText)
        tvRemainingTime = findViewById(R.id.tvRemainingTime)
        toggleGpuMediatek1 = findViewById(R.id.toggleGpuMediatek1)
        toggleGpuMediatek2 = findViewById(R.id.toggleGpuMediatek2)
        toggleGpuSnapdragon1 = findViewById(R.id.toggleGpuSnapdragon1)
        toggleGpuSnapdragon2 = findViewById(R.id.toggleGpuSnapdragon2)
        selinuxStatusTextView = findViewById(R.id.selinuxStatusTextView)
        toggleSelinuxButton = findViewById(R.id.toggleSelinuxButton)


        // Initialize other buttons
        val btnLogout: Button = findViewById(R.id.btnLogout)
        val btnShutdown = findViewById<Button>(R.id.btnShutdown)
        val toggleButton = findViewById<ToggleButton>(R.id.toggleButton)
        val btnBoostCpu = findViewById<Button>(R.id.btnBoostCpu)
        val btnBalanceCpu = findViewById<Button>(R.id.btnBalanceCpu)
        val btnUnlockML = findViewById<Button>(R.id.btnUnlockML)
        val toggleClockBoost = findViewById<ToggleButton>(R.id.toggleClockBoost)
        val btnBoostGpu: Button = findViewById(R.id.btnBoostGpu)
        val btnBalanceGpu: Button = findViewById(R.id.btnBalanceGpu)
        val availableRam = getAvailableRamInMB()
        Log.d("RAM_INFO", "Available RAM: $availableRam MB")

        // Initialize SharedPreferences again if needed
        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)

        // Display the remaining time
        displayRemainingTime()

        // Toolbar setup
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)


        // Retrieve the username from Intent
        val username = intent.getStringExtra("USERNAME")
        val textView = findViewById<TextView>(R.id.welcome)

// Check user type and display remaining time or messages accordingly
        if (username == "Tester") {
            displayRemainingTime() // Display countdown timer for Tester
        } else if (username == "Perma-Tester") {
            tvRemainingTime.text = "Time Remaining: You Are Permanent Tester, You Have Privilege"
        } else if (username == "Rafaeru") {
            tvRemainingTime.text = "Time Remaining: Hello [Rafaeru]"
        } else if (username == "Fox") {
            tvRemainingTime.text = "Time Remaining: Hello Donator you dont have time remaining to use this app"
        } else if (username == "User") {
            tvRemainingTime.text = "Time Remaining: "
        } else {
            tvRemainingTime.text = "Time Remaining: How did you get here?"
            lifecycleScope.launch {
                welcome()
            }
        }


// Set the appropriate welcome message based on the username
        when (username) {
            "Fox" -> textView.text = "Welcome Donator"
            "Donation" -> textView.text = "ty for small donation!"
            "donation" -> textView.text = "ty for small donation!"
            "Tester" -> textView.text = "App Test only, please give feedback"
            "Rafaeru" -> textView.text = "Welcome Developer [Rafaeruuu]"
            "Perma-Tester" -> textView.text = "Welcome Privilege People"
            else -> {
                Toast.makeText(this, "APK has been tampered with!", Toast.LENGTH_LONG).show()
                lifecycleScope.launch {
                    welcome()
                }
            }
        }

        // Check SELinux status
        CoroutineScope(Dispatchers.Main).launch {
            val status = getSelinuxStatus()
            selinuxStatusTextView.text = status
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        // Show loading dialog
        showLoadingDialog()

        toggleSelinuxButton.setOnClickListener {
            vibratePhone()
            statusText.text="wait 5-10sec"
            CoroutineScope(Dispatchers.Main).launch {
                val currentStatus = getSelinuxStatus()
                if (currentStatus.contains("Enforcing")) {
                    setSelinuxStatus(0) // Set to Permissive
                } else {
                    setSelinuxStatus(1) // Set to Enforcing
                }

                val newStatus = getSelinuxStatus()
                selinuxStatusTextView.text = newStatus
                Toast.makeText(this@MainActivity, "SELinux status changed to $newStatus", Toast.LENGTH_SHORT).show()
            }
        }

        // Handler to update CPU information periodically
        handler = Handler(Looper.getMainLooper())
        handler.post(updateRunnable)

        btnShutdown.setOnClickListener {
            lifecycleScope.launch {
                shutdownDevice()
            }
        }

        // Check root access and update UI accordingly
        lifecycleScope.launch {
            delay(2000) // Add 2-second delay before checking root access
            if (!isRootGranted()) {
                statusroottext.text = "Status Root: Access not granted. Closing app in 20 seconds..."
                handler.postDelayed({
                    finishAffinity()
                    exitProcess(0)
                }, 20000)
            } else {
                statusroottext.text = "Status Root: Access granted. You can use the app."
            }
        }

        val ramUsageText = findViewById<TextView>(R.id.ramUsageText)
        val availableRamText = findViewById<TextView>(R.id.availableRamText)
        val clearRamButton = findViewById<Button>(R.id.clearRamButton)


        updateRamInfo(ramUsageText, availableRamText)

        clearRamButton.setOnClickListener {
            vibratePhone()
            clearRam()
            updateRamInfo(ramUsageText, availableRamText)
            Toast.makeText(this@MainActivity, "Ram Cleaned using Root Access", Toast.LENGTH_SHORT).show()
        }

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            vibratePhone()
            lifecycleScope.launch {
                if (isChecked) {
                    disableGoogleApps()
                    toggleButton.text = "Enable GMS"
                    statusroottext.text = "Status Root: GMS Disabled"
                } else {
                    enableGoogleApps()
                    toggleButton.text = "Disable GMS"
                }
            }
        }

        btnBoostCpu.setOnClickListener {
            vibratePhone()
            statusText.text = "Boosting CPU Performance..."
            lifecycleScope.launch {
                val command = (0..7).joinToString(" && ") {
                    "echo performance > /sys/devices/system/cpu/cpu$it/cpufreq/scaling_governor"
                }
                val result = executeRootCommand(command)
                result?.let { statusText.text = it }
            }
        }

        btnBalanceCpu.setOnClickListener {
            vibratePhone()
            statusText.text = "Setting CPU to Balanced Mode..."
            lifecycleScope.launch {
                val command = (0..7).joinToString(" && ") {
                    "echo schedutil > /sys/devices/system/cpu/cpu$it/cpufreq/scaling_governor"
                }
                val result = executeRootCommand(command)
                result?.let { statusText.text = it }
            }
        }

        checkRootAccess(statusroottext)

        btnBoostGpu.setOnClickListener {
            vibratePhone()
            CoroutineScope(Dispatchers.Main).launch {
                GovernorManager.setGpuGovernor("performance", statusroottext)
            }
        }

        btnBalanceGpu.setOnClickListener {
            vibratePhone()
            CoroutineScope(Dispatchers.Main).launch {
                GovernorManager.setGpuGovernor("userspace", statusroottext)
            }
        }

        btnUnlockML.setOnClickListener {
            vibratePhone()
            statusText.text = "Unlocking ML Graphics..."
            lifecycleScope.launch {
                unlockMLGraphics()
                statusText.text = "Mobile Legends Graphics Unlocked"
            }
        }

        btnLogout.setOnClickListener {
            vibratePhone()
            logout()
        }

        toggleClockBoost.setOnCheckedChangeListener { _, isChecked ->
            vibratePhone()
            if (isChecked) {
                startClockBoost()
                toggleClockBoost.text = "Stop Clock Boost"
                statusText.text = "Status : Lock CPU"
            } else {
                stopClockBoost()
                toggleClockBoost.text = "Start Clock Boost"
            }
        }

        toggleGpuMediatek1.setOnCheckedChangeListener { _, isChecked ->
            vibratePhone()
            lifecycleScope.launch {
                val gpuPath = "/sys/devices/platform/mtk_gpu.0/devfreq/"
                if (isChecked) {
                    setGpuFrequency(gpuPath, GPU_MAX_FREQUENCY, GPU_GOVERNOR)
                } else {
                    resetGpuFrequency(gpuPath)
                }
            }
        }

        toggleGpuMediatek2.setOnCheckedChangeListener { _, isChecked ->
            vibratePhone()
            lifecycleScope.launch {
                val gpuPath = "/sys/class/devfreq/mtk-dvfsrc-devfreq/"
                if (isChecked) {
                    setGpuFrequency(gpuPath, GPU_MAX_FREQUENCY, GPU_GOVERNOR)
                } else {
                    resetGpuFrequency(gpuPath)
                }
            }
        }

        toggleGpuSnapdragon1.setOnCheckedChangeListener { _, isChecked ->
            vibratePhone()
            lifecycleScope.launch {
                val gpuPath = "/sys/class/kgsl/kgsl-3d0/devfreq/"
                if (isChecked) {
                    setGpuFrequency(gpuPath, GPU_MAX_FREQUENCY, GPU_GOVERNOR)
                } else {
                    resetGpuFrequency(gpuPath)
                }
            }
        }

        toggleGpuSnapdragon2.setOnCheckedChangeListener { _, isChecked ->
            vibratePhone()
            lifecycleScope.launch {
                val gpuPath = "/sys/kernel/gpu/"
                if (isChecked) {
                    setGpuFrequency(gpuPath, GPU_MAX_FREQUENCY, GPU_GOVERNOR)
                } else {
                    resetGpuFrequency(gpuPath)
                }
            }
        }

        toggleSelinuxButton.setOnClickListener {
            vibratePhone()
            CoroutineScope(Dispatchers.Main).launch {
                val currentStatus = getSelinuxStatus()
                val newStatus = if (currentStatus.contains("Enforcing")) {
                    setSelinuxStatus(0) // Set to Permissive
                } else {
                    setSelinuxStatus(1) // Set to Enforcing
                }

                // Update the displayed status
                selinuxStatusTextView.text = newStatus
                Toast.makeText(this@MainActivity, "SELinux status changed", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun startClockBoost() {
        isClockBoosted = true
        lifecycleScope.launch {
            while (isClockBoosted) {
                executeRootCommand((0..7).joinToString(" && ") {
                    """
                    echo 4100000 > /sys/devices/system/cpu/cpu$it/cpufreq/scaling_max_freq && 
                    echo 4000000 > /sys/devices/system/cpu/cpu$it/cpufreq/scaling_min_freq
                    """.trimIndent()
                })
                delay(100)
            }
        }
    }

    private fun stopClockBoost() {
        isClockBoosted = false
        lifecycleScope.launch {
            val command = (0..7).joinToString(" && ") {
                "echo schedutil > /sys/devices/system/cpu/cpu$it/cpufreq/scaling_governor"
            }
            executeRootCommand(command)
        }
    }

    private fun getRamUsagePercentage(): Int {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem.toDouble()
        val availableRam = memoryInfo.availMem.toDouble()
        val usedRam = totalRam - availableRam

        return ((usedRam / totalRam) * 100).roundToInt()
    }

    private fun getAvailableRamInMB(): Long {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        Log.d("RAM_INFO", "Available Memory (bytes): ${memoryInfo.availMem}")

        return memoryInfo.availMem / (1024 * 1024)
    }

    private fun clearRam() {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val packageManager = packageManager
            val runningProcesses = activityManager.runningAppProcesses

            for (processInfo in runningProcesses) {
                val packageName = processInfo.processName
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val isSystemApp = (appInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0

                    if (processInfo.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND && !isSystemApp) {
                        val pid = processInfo.pid

                        Runtime.getRuntime().exec(arrayOf("su", "-c", "kill -9 $pid"))
                        Runtime.getRuntime().exec(arrayOf("su", "-c", "am force-stop $packageName"))
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }

            Runtime.getRuntime().exec(arrayOf("su", "-c", "sync; echo 3 > /proc/sys/vm/drop_caches"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateRamInfo(ramUsageText: TextView, availableRamText: TextView) {
        val ramUsagePercentage = getRamUsagePercentage()
        val availableRamInMB = getAvailableRamInMB()
        ramUsageText.text = "Ram Usage: $ramUsagePercentage%"
        availableRamText.text = "Available RAM: $availableRamInMB MB"
    }



    private fun displayRemainingTime() {
        val targetDate = getTargetDateAndTime()
        val currentTime = Calendar.getInstance().apply {
            timeZone = java.util.TimeZone.getTimeZone("GMT+8") // Set current time to GMT+8
        }

        if (targetDate.after(currentTime)) {
            val remainingMillis = targetDate.timeInMillis - currentTime.timeInMillis

            // Start a countdown timer
            object : CountDownTimer(remainingMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    // Calculate time units
                    val seconds = (millisUntilFinished / 1000).toInt()
                    val minutes = seconds / 60
                    val hours = minutes / 60
                    val days = hours / 24
                    val months = days / 30 // Approximate calculation for months

                    val remainingSeconds = seconds % 60
                    val remainingMinutes = minutes % 60
                    val remainingHours = hours % 24
                    val remainingDays = days % 30 // Remaining days after full months

                    val timerRemaining = String.format(
                        "%2dM - %02dD - %02dH - %02dM - %02dS",
                        months, remainingDays, remainingHours, remainingMinutes, remainingSeconds
                    )

                    tvRemainingTime.text = "Time Remaining: $timerRemaining"
                }

                override fun onFinish() {
                    tvRemainingTime.text = "Time Remaining: Expired"
                    // Optionally, handle expiration here
                }
            }.start()
        } else {
            tvRemainingTime.text = "Time Remaining: Expired"
        }
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
            }
        } else {
            Calendar.getInstance() // Return current time if values are not valid
        }
    }

    private fun welcome() {
        val intent = Intent(this, MainActivity::class.java) // Ganti dengan activity utama
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }


    // Function to handle logout
    private fun logout() {
        val editor = sharedPreferences.edit()
        editor.clear() // Clear saved data
        editor.apply()

        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }

    // Function to navigate back to login activity
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Close MainActivity
    }

    private fun showLoadingDialog() {
        // Create a dialog instance
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_loading)
        dialog.setCancelable(false) // Prevent the user from dismissing it

        // Set dialog window background to be transparent
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Show the dialog
        dialog.show()

        // Apply blur effect to the background (dim effect)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog.window?.setDimAmount(0.7f) // Adjust this for more or less blur

        // Automatically dismiss the dialog after 5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        }, 5000) // 5000 milliseconds = 5 seconds
    }

    private fun vibratePhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(
                200,
                VibrationEffect.DEFAULT_AMPLITUDE
            ) // 200ms vibration
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(200) // For older devices
        }
    }

    private suspend fun setGpuFrequency(path: String, frequency: String, governor: String) =
        withContext(Dispatchers.IO) {
            val frequencyFile = File(path, "max_freq")
            val governorFile = File(path, "governor")
            if (frequencyFile.exists()) {
                originalGpuFrequency = frequencyFile.readText()
                frequencyFile.writeText(frequency)
                governorFile.writeText(governor)
            }
        }

    private suspend fun resetGpuFrequency(path: String) = withContext(Dispatchers.IO) {
        val frequencyFile = File(path, "max_freq")
        if (frequencyFile.exists() && originalGpuFrequency != null) {
            frequencyFile.writeText(originalGpuFrequency!!)
        }
    }

    private suspend fun setSelinuxStatus(mode: Int): String {
        return withContext(Dispatchers.IO) {
            try {
                val command = if (mode == 0) "setenforce 0" else "setenforce 1"
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                process.waitFor() // Wait for the command to finish
                getSelinuxStatus() // Return the updated SELinux status
            } catch (e: IOException) {
                Log.e("SetSELinuxStatus", "Exception: ${e.message}")
                "Error: ${e.message}"
            }
        }
    }

    private suspend fun executeRootCommand(command: String): String? = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exitCode = process.waitFor()

            return@withContext if (exitCode == 0) {
                null // Command executed successfully
            } else {
                val errorStream = process.errorStream.bufferedReader().readText()
                Log.e("RootCommand", "Failed with exit code $exitCode. Error: $errorStream")
                errorStream
            }
        } catch (e: Exception) {
            Log.e("RootCommand", "Exception: ${e.message}")
            e.message
        }
    }

    private suspend fun shutdownDevice() {
        withContext(Dispatchers.IO) {
            try {
                val command = "reboot -p" // Command to power off the device
                executeRootCommand(command)
            } catch (e: Exception) {
                Log.e("ShutdownDevice", "Exception: ${e.message}")
            }
        }
    }


    private suspend fun isRootGranted(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun getSelinuxStatus(): String {
        delay(1000) // Add 5-second delay as per your requirement
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "getenforce"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine()?.trim() ?: "Unknown"
        } catch (e: IOException) {
            Log.e("SELinuxStatus", "Exception: ${e.message}")
            "Error: ${e.message}"
        }
    }

    private suspend fun disableGoogleApps() {
        val command = googleApps.joinToString("\n") { "pm disable $it" }
        executeRootCommand(command)
        withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Google Apps Disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun enableGoogleApps() {
        val command = googleApps.joinToString("\n") { "pm enable $it" }
        executeRootCommand(command)
        withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, "Google Apps Enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun unlockMLGraphics() = withContext(Dispatchers.IO) {
        val mlGraphicsFile = File("/data/data/com.mobile.legends/shared_prefs/com.mobile.legends.v2.playerprefs.xml")
        if (mlGraphicsFile.exists()) {
            val content = mlGraphicsFile.readText()
            var updatedContent = content.replace(
                "<int name=\"HighFpsModeSee\" value=\"2\" />",
                "<int name=\"HighFpsModeSee\" value=\"4\" />"
            )
            updatedContent = updatedContent.replace(
                "<int name=\"HighFpsMode_New\" value=\"2\" />",
                "<int name=\"HighFpsMode_New\" value=\"4\" />"
            )
            mlGraphicsFile.writeText(updatedContent)
        }
    }

    private fun readFile(path: String): String {
        return try {
            File(path).readText().trim()
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    private val updateRunnable: Runnable = object : Runnable {
        override fun run() {
            try {
                updateCpuInfo()
            } catch (e: Exception) {
                statusText.text = "Error updating CPU info: ${e.message}"
            }
            handler.postDelayed(this, updateInterval)
        }
    }

    private fun updateCpuInfo() {
        val cpuGovernorPath = "/sys/devices/system/cpu/cpu7/cpufreq/scaling_governor"
        val cpuSpeedPath = "/sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq"

        try {
            val governor = readFile(cpuGovernorPath)
            val speed = readFile(cpuSpeedPath)

            cpuGovernorText.text = governor.ifEmpty { "Governor not found" }
            cpuSpeedText.text = if (speed.isNotEmpty()) "${speed} Hz" else "Speed not found"
        } catch (e: Exception) {
            Log.e("CPUInfo", "Error reading CPU info: ${e.message}")
            cpuGovernorText.text = "Error: ${e.message}"
            cpuSpeedText.text = "Error: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopClockBoost()
        handler.removeCallbacks(updateRunnable)
    }

    class CPUService : Service() {
        override fun onBind(intent: Intent?): IBinder? {
            return null
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = "CPUServiceChannel"
                val channelName = "CPU Service Channel"
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(channelId, channelName, importance)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)

                val notification: Notification = NotificationCompat.Builder(this, channelId)
                    .setContentTitle("CPU Service Running")
                    .setContentText("This service is running in the background to monitor CPU.")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build()

                startForeground(1, notification)
            }
            return START_STICKY
        }
    }

    private fun checkRootAccess(statusroottext: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            val process = Runtime.getRuntime().exec("su")
            try {
                process.outputStream.write("exit\n".toByteArray())
                process.outputStream.flush()
                val exitCode = process.waitFor()
                withContext(Dispatchers.Main) {
                    if (exitCode == 0) {
                        statusroottext.text = "Status Root: Root access granted"
                    } else {
                        statusroottext.text = "Status Root: Root access denied"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusroottext.text = "Status Root: Error - ${e.message}"
                }
            }
        }
    }
}
