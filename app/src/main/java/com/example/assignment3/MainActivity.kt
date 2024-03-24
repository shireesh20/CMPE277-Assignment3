package com.example.assignment3

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.assignment3.ui.theme.Assignment3Theme
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.FileInputStream
import java.io.IOException
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import java.io.InputStream
import okhttp3.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Assignment3Theme {
                OCRHomePage()
            }
        }
    }
}

@Composable
fun OCRHomePage() {
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var ocrText by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "OCR Analysis",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { pickImageLauncher.launch("image/*") },
            modifier = Modifier.padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBBDEFB), contentColor = Color.Black)
        ) { Text("Upload") }

        selectedImageUri?.let { uri ->
            Text("Image selected: ${uri.lastPathSegment}")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                selectedImageUri?.let { uri ->
                    isAnalyzing = true
                    analyzeImageWithOCR(context, uri) { result ->
                        isAnalyzing = false
                        ocrText = result
                    }
                }
            },
            modifier = Modifier.padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBBDEFB), contentColor = Color.Black),
            enabled = !isAnalyzing
        ) { Text(if (isAnalyzing) "Analyzing..." else "Analyze") }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = ocrText,
            onValueChange = {},
            label = { Text("Response") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            readOnly = true
        )
    }
}

fun analyzeImageWithOCR(context: Context, imageUri: Uri, onResult: (String) -> Unit) {
    val fileDescriptor = context.contentResolver.openFileDescriptor(imageUri, "r")?.fileDescriptor
    fileDescriptor?.let { fd ->
        val imageInputStream: InputStream = FileInputStream(fd)
        val httpClient = OkHttpClient()

        val mediaTypeOctetStream = "application/octet-stream".toMediaTypeOrNull()
        val imageRequestBody = imageInputStream.readBytes().toRequestBody(mediaTypeOctetStream)

        val ocrApiUrl = "<YOUR_URL>"

        val request = Request.Builder()
            .url(ocrApiUrl)
            .post(imageRequestBody)
            .addHeader("Ocp-Apim-Subscription-Key", "YOUR_KEYS") // Replace with your actual subscription key
            .addHeader("Content-Type", "application/octet-stream")
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult("Failed to analyze image: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        onResult("Unexpected code $resp")
                    } else {
                        val ocrResponseBody = resp.body?.string()
                        onResult(ocrResponseBody ?: "No OCR result found")
                    }
                }
            }
        })
    } ?: onResult("Could not open file descriptor for the image.")
}

