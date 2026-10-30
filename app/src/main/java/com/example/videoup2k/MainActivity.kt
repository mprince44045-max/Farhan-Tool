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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

// Custom Colors
val DarkBg = Color(0xFF121212)
val CardBg = Color(0xFF1E1E1E)
val AccentPurple = Color(0xFFBB86FC)
val AccentGreen = Color(0xFF03DAC6)

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
                status = "Video Ready ✓\n2K Convert par click karein"
            } catch (e: Exception) {
                status = "Error: ${e.message}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(background = DarkBg, surface = CardBg)
            ) {
                MainScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen() {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Farhan Tool",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // Status Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = status,
                        color = Color.LightGray,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Video Button (Fancy)
                Button(
                    onClick = {
                        pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Icon(Icons.Filled.VideoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Select Video for 2K", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Convert Button
                if (inputPath != null && !processing) {
                    Button(
                        onClick = { upscale() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Convert to 2K", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (processing) {
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(color = AccentPurple)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Processing... Please wait", color = Color.Gray)
                }

                // Save Button
                if (outputPath != null && !processing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { saveToGalleryAndOpen(File(outputPath!!)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Save to Gallery & Play", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Divider(color = CardBg, thickness = 1.dp)
                Spacer(modifier = Modifier.height(24.dp))

                // Fancy Grid Options (Camera, Photo, History)
                Text("More Tools", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Camera Option
                    FancyOptionCard(icon = Icons.Filled.CameraAlt, title = "Camera") {
                        // TODO: Add Camera feature later
                        status = "Camera feature coming soon!"
                    }

                    // Photo Option
                    FancyOptionCard(icon = Icons.Filled.Photo, title = "Photo") {
                        // TODO: Add Photo feature later
                        status = "Photo feature coming soon!"
                    }

                    // History Option
                    FancyOptionCard(icon = Icons.Filled.History, title = "History") {
                        // TODO: Add History feature later
                        status = "History feature coming soon!"
                    }
                }
            }
        }
    }

    @Composable
    fun FancyOptionCard(icon: ImageVector, title: String, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .width(100.dp)
                .height(100.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(AccentPurple, AccentGreen))),
            onClick = onClick
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2C2C)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = AccentPurple)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    private fun upscale() {
        val input = inputPath ?: return
        processing = true
        outputPath = null
        status = "Converting video to 2K... Please wait."

        val outputFile = File(filesDir, "output_2k_${System.currentTimeMillis()}.mp4")
        val output = outputFile.absolutePath

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cmd = "-y -i \"$input\" -vf \"scale=2560:1440\" -c:v mpeg4 -crf 23 \"$output\""

                FFmpegKit.executeAsync(cmd) { session ->
                    runOnUiThread {
                        processing = false
                        if (ReturnCode.isSuccess(session.returnCode)) {
                            outputPath = output
                            status = "✅ Convert Complete!\nSave to gallery par click karein."
                        } else {
                            status = "❌ Conversion Failed. Try another video."
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
            
            status = "✅ Saved to Gallery successfully!"
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(it, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                status = "Saved, but failed to open player."
            }
        }
    }
}
