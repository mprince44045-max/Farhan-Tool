package com.example.videoup2k

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private var inputPath by mutableStateOf<String?>(null)
    private var status by mutableStateOf("Video select karein")
    private var processing by mutableStateOf(false)

    private val pickVideo = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                val file = File(cacheDir, "input.mp4")
                contentResolver.openInputStream(it)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                inputPath = file.absolutePath
                status = "Video Ready ✓"
            } catch (e: Exception) {
                status = "Error selecting video: ${e.message}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Farhan tool", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    
                    // Text ko scrollable banaya hai taake error saaf dikhe
                    Text(status, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = {
                            pickVideo.launch(PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.VideoOnly
                            ))
                        },
                        enabled = !processing
                    ) { Text("Video Select Karein") }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { upscale() },
                        enabled = inputPath != null && !processing
                    ) { Text(if (processing) "Processing..." else "2K Convert Karein") }

                    if (processing) {
                        Spacer(Modifier.height(24.dp))
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    private fun upscale() {
        val input = inputPath ?: return
        processing = true
        status = "2K me convert ho raha hai... (Thoda time lagega)"

        val output = File(filesDir, "output_2k_${System.currentTimeMillis()}.mp4").absolutePath

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Simple command taake error na aaye
                val cmd = "-y -i \"$input\" -vf \"scale=2560:1440\" -c:v libx264 -crf 23 \"$output\""

                FFmpegKit.executeAsync(cmd) { session ->
                    runOnUiThread {
                        processing = false
                        if (ReturnCode.isSuccess(session.returnCode)) {
                            status = "✅ Complete!\nFile saved in App Folder"
                        } else {
                            // Asli error message screen par dikhane ke liye
                            val errorLogs = session.logs?.joinToString("\n") { it.message }?.takeLast(200) ?: "Unknown Error"
                            status = "❌ Failed:\n$errorLogs"
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    processing = false
                    status = "❌ Error: ${e.message}"
                }
            }
        }
    }
}
