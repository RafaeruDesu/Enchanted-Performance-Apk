package com.rafaeruuu.enchanted.performance.ui


import android.util.Log
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GovernorManager {

    // Set GPU governor for Snapdragon and MediaTek devices
    companion object {
        suspend fun setGpuGovernor(governor: String, statusRootText: TextView) =
            withContext(Dispatchers.IO) {
                val gpuGovernorPaths = arrayOf(
                    // Snapdragon paths
                    "/sys/class/kgsl/kgsl-3d0/devfreq/governor",    // Snapdragon GPU governor path
                    "/sys/class/devfreq/devfreq1/governor",         // Snapdragon fallback
                    "/sys/kernel/gpu/gpu_governor",                 // Snapdragon alternative path
                    "/sys/class/devfreq/soc:qcom,gpubw/governor",  // Snapdragon alternative path
                    "/sys/class/devfreq/devfreq2/governor",         // Another possible Snapdragon path
                    "/sys/class/kgsl/kgsl-3d0/devfreq/min_freq",    // Minimum frequency setting for Snapdragon
                    "/sys/class/kgsl/kgsl-3d0/devfreq/max_freq",    // Maximum frequency setting for Snapdragon
                    "/sys/class/devfreq/soc:qcom,gpu/governor",     // Another common Snapdragon path

                    // MediaTek paths
                    "/sys/class/devfreq/devfreq0/governor",         // MediaTek GPU governor path
                    "/sys/class/devfreq/devfreq1/governor",         // MediaTek additional governor path
                    "/sys/class/devfreq/devfreq2/governor",         // Another MediaTek path
                    "/sys/class/devfreq/devfreq3/governor",         // Yet another MediaTek path
                    "/sys/class/devfreq/devfreq4/governor",         // Additional MediaTek GPU governor path
                    "/sys/class/gpu/gpu0/governor",                 // MediaTek GPU governor
                    "/sys/class/devfreq/soc:mtk-gpu/governor",       // MediaTek specific GPU path
                    "/sys/class/devfreq/mtk-dvfsrc-devfreq/governor" // MediaTek specific GPU path
                )


                var governorSet = false

                // Iterate through each possible governor path and attempt to set the governor
                for (path in gpuGovernorPaths) {
                    if (File(path).exists()) {
                        if (setGovernor(path, governor)) {
                            withContext(Dispatchers.Main) {
                                statusRootText.text = "Governor set to $governor at $path"
                            }
                            governorSet = true
                            break // Stop after successfully setting governor on one path
                        } else {
                            Log.e("GpuGovernorManager", "Failed to set governor at path: $path")
                        }
                    } else {
                        Log.e("GpuGovernorManager", "Governor path not found: $path")
                    }
                }

                if (!governorSet) {
                    withContext(Dispatchers.Main) {
                        statusRootText.text =
                            "Failed to set governor $governor on any available paths"
                    }
                }
            }

        // Function to set the governor at a given path
        private suspend fun setGovernor(path: String, governor: String): Boolean =
            withContext(Dispatchers.IO) {
                return@withContext try {
                    // Create the command to set the governor
                    val command = "echo $governor > $path"
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))

                    // Wait for the command to finish and check exit value
                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        Log.i("GpuGovernorManager", "Governor set to $governor at $path")
                        true
                    } else {
                        Log.e(
                            "GpuGovernorManager",
                            "Failed to set governor with exit code: $exitCode"
                        )
                        false
                    }
                } catch (e: Exception) {
                    Log.e("GpuGovernorManager", "Error setting governor: ${e.message}")
                    false
                }
            }

    }
}
