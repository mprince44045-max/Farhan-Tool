package com.example.videoup2k

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
    private var outputPath by mutableStateOf<String?>(null)
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
                outputPath = null
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

                    // Jab video 2K ban jaye, tab yeh button dikhega
                    if (outputPath != null && !processing) {
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { saveToGalleryAndOpen(File(outputPath!!)) },
                            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                        ) {
                            Text("💾 Save to Gallery & Play")
                        }
                    }
                }
            }
        }
    }

    private fun upscale() {
        val input = inputPath ?: return
        processing = true
        outputPath = null
        status = "2K me convert ho raha hai... (Thoda time lagega)"

        val outputFile = File(filesDir, "output_2k_${System.currentTimeMillis()}.mp4")
        val output = outputFile.absolutePath

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // mpeg4 encoder crash nahi karta
                val cmd = "-y -i \"$input\" -vf \"scale=2560:1440\" -c:v mpeg4 -crf 23 \"$output\""

                FFmpegKit.executeAsync(cmd) { session ->
                    runOnUiThread {
                        processing = false
                        if (ReturnCode.isSuccess(session.returnCode)) {
                            outputPath = output
                            status = "✅ Convert Complete!\nAb gallery me save karein"
                        } else {
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

    private fun saveToGalleryAndOpen(file: File) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "FarhanTool_2K_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/FarhanTool")
            }
        }

        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            status = "✅ Gallery me save ho gaya!"
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(it, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                status = "Saved, par play nahi ho paya."
            }
        }
    }
}
