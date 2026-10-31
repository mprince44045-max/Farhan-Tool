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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    private var inputPath by mutableStateOf<String?>(null)
    private var outputPath by mutableStateOf<String?>(null)
    private var status by mutableStateOf("Select Video")
    private var processing by mutableStateOf(false)

    private val pickVideo = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            try {
                val file = File(cacheDir, "input.mp4")
                contentResolver.openInputStream(it)?.use { input -> 
                    file.outputStream().use { output -> 
                        input.copyTo(output) 
                    } 
                }
                inputPath = file.absolutePath
                outputPath = null
                status = "Video Ready ✓"
            } catch (e: Exception) {
                status = "Error loading video: ${e.message}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .background(Color(0xFF121212)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(status, color = Color.LightGray, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { 
                            try {
                                pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) 
                            } catch (e: Exception) {
                                status = "Error: ${e.message}"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
                    ) { 
                        Text("Select Video", fontSize = 18.sp, fontWeight = FontWeight.Bold) 
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (inputPath != null && !processing) {
                        // FHD Button (1920x1080)
                        Button(
                            onClick = { convertVideo("1920:1080") },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC6))
                        ) { 
                            Text("FHD (1080p)", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) 
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 2K Button (2560x1440)
                        Button(
                            onClick = { convertVideo("2560:1440") },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC6))
                        ) { 
                            Text("2K (1440p)", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold) 
                        }
                    }

                    if (processing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(color = Color(0xFFBB86FC))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Processing... Please wait", color = Color.Gray)
                    }

                    if (outputPath != null && !processing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { saveToGallery(File(outputPath!!)) },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) { 
                            Text("Save to Gallery", fontSize = 18.sp) 
                        }
                    }
                }
            }
        }
    }

    private fun convertVideo(scaleParam: String) {
        val input = inputPath ?: return
        processing = true
        outputPath = null
        status = "Converting to $scaleParam..."
        val output = File(filesDir, "out_${System.currentTimeMillis()}.mp4").absolutePath

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // -preset ultrafast RAM bachata hai aur crash nahi hone deta
                // -threads 2 se processor par load kam hota hai
                val cmd = "-y -i \"$input\" -vf \"scale=$scaleParam\" -c:v libx264 -preset ultrafast -threads 2 \"$output\""
                
                FFmpegKit.executeAsync(cmd) { session ->
                    runOnUiThread {
                        processing = false
                        try {
                            if (ReturnCode.isSuccess(session.returnCode)) {
                                outputPath = output
                                status = "✅ Convert Complete!"
                            } else {
                                status = "❌ Failed. Try smaller video."
                            }
                        } catch (e: Exception) {
                            status = "❌ Error: ${e.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    processing = false
                    status = "❌ FFmpeg Error: ${e.message}"
                }
            }
        }
    }

    private fun saveToGallery(file: File) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "Farhan_HD_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
                }
            }
            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { out -> 
                    file.inputStream().use { 
                        it.copyTo(out) 
                    } 
                }
                status = "✅ Saved to Gallery!"
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(it, "video/mp4")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try { startActivity(intent) } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            status = "❌ Save Failed: ${e.message}"
        }
    }
}
