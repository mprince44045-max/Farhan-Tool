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
    private var showTools by mutableStateOf(false)

    private val pickVideo = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val file = File(cacheDir, "input.mp4")
            contentResolver.openInputStream(it)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            inputPath = file.absolutePath
            outputPath = null
            status = "Ready ✓"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Scaffold(
                    topBar = { TopAppBar(title = { Text("Farhan Tool", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))) }
                ) { padding ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).background(Color(0xFF121212)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(status, color = Color.LightGray, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC))
                        ) { Text("Select Video", fontSize = 18.sp, fontWeight = FontWeight.Bold) }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (inputPath != null && !processing) {
                            Button(
                                onClick = { upscale() },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC6))
                            ) { Text("Convert to 2K", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }

                        if (processing) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(color = Color(0xFFBB86FC))
                        }

                        if (outputPath != null && !processing) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { saveToGallery(File(outputPath!!)) },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) { Text("Save to Gallery", fontSize = 18.sp) }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { showTools = !showTools },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E))
                        ) { Text(if (showTools) "Hide Tools" else "More Tools", color = Color.White) }

                        if (showTools) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                Text("📷 Camera", color = Color.White)
                                Text("🖼️ Photo", color = Color.White)
                                Text("🕒 History", color = Color.White)
                            }
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
        status = "Converting to 2K..."
        val output = File(filesDir, "out_${System.currentTimeMillis()}.mp4").absolutePath

        CoroutineScope(Dispatchers.IO).launch {
            val cmd = "-y -i \"$input\" -vf \"scale=2560:1440\" -c:v mpeg4 -crf 23 \"$output\""
            FFmpegKit.executeAsync(cmd) { session ->
                runOnUiThread {
                    processing = false
                    if (ReturnCode.isSuccess(session.returnCode)) {
                        outputPath = output
                        status = "✅ Convert Complete!"
                    } else {
                        status = "❌ Conversion Failed."
                    }
                }
            }
        }
    }

    private fun saveToGallery(file: File) {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "Farhan_2K.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            status = "✅ Saved!"
            startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(it, "video/mp4"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) })
        }
    }
}
