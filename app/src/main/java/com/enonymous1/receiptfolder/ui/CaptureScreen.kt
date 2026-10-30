package com.enonymous1.receiptfolder.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.enonymous1.receiptfolder.domain.ReceiptParser
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.launch

/**
 * CameraX preview plus gallery pick. v1's camera intent returned only the
 * thumbnail ("data" extra), which is why captured images did not survive
 * through the app; here ImageCapture writes a full-resolution JPEG into
 * the app's private receipts directory before OCR runs on it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    viewModel: ReceiptViewModel,
    onCaptured: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    fun processImageFile(file: File) {
        scope.launch {
            isProcessing = true
            errorMessage = null
            try {
                val rawText = recogniseReceiptText(context, file)
                val parsed = ReceiptParser.parse(rawText)
                viewModel.setPendingCapture(
                    ReceiptViewModel.PendingCapture(
                        imagePath = file.absolutePath,
                        merchant = parsed.merchant,
                        dateMillis = parsed.dateMillis,
                        totalCents = parsed.totalCents,
                    ),
                )
                isProcessing = false
                onCaptured()
            } catch (e: Exception) {
                isProcessing = false
                errorMessage = "Could not read that photo: ${e.message}"
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            val file = copyPickedImage(context, uri)
            if (file != null) processImageFile(file)
            else errorMessage = "Could not copy that image."
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!hasPermission) {
                Text("Camera permission is needed to take a receipt photo.")
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Allow camera")
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val providerFuture = ProcessCameraProvider.getInstance(ctx)
                        providerFuture.addListener(
                            {
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageCapture,
                                    )
                                } catch (_: Exception) {
                                    errorMessage = "Camera could not start on this device."
                                }
                            },
                            ContextCompat.getMainExecutor(ctx),
                        )
                        previewView
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                Button(
                    onClick = {
                        val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
                        val file = File(receiptsDir, "receipt-${System.currentTimeMillis()}.jpg")
                        val options = ImageCapture.OutputFileOptions.Builder(file).build()
                        imageCapture.takePicture(
                            options,
                            cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    processImageFile(file)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    errorMessage = "Photo failed: ${exception.message}"
                                }
                            },
                        )
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Take photo")
                }
            }
            OutlinedButton(
                onClick = { galleryLauncher.launch("image/*") },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Choose from gallery")
            }
            if (isProcessing) CircularProgressIndicator()
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(
                "The photo is recognised on-device. Check the merchant, date, " +
                    "and total on the next screen before saving.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
