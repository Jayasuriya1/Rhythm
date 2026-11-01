package com.example.ui.screens.alarm

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

enum class ScannerMode {
    DISMISS_VERIFY,
    REGISTER_EXISTING
}

@Composable
fun BarcodeScannerView(
    targetBarcode: String? = null,
    targetLabel: String? = null,
    targetFormat: String? = null,
    scannerMode: ScannerMode = ScannerMode.DISMISS_VERIFY,
    existingCodes: List<String> = emptyList(),
    onBarcodeMatched: () -> Unit = {},
    onCodeRegistered: ((codeValue: String, format: String, label: String, locationHint: String?) -> Unit)? = null,
    onCancelOrSkip: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var isTorchEnabled by remember { mutableStateOf(false) }

    // Detection States
    var manualCodeInput by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var lastScannedText by remember { mutableStateOf<String?>(null) }
    var lastScannedFormat by remember { mutableStateOf<String?>(null) }
    var matchError by remember { mutableStateOf(false) }

    // Registration Dialog State
    var showRegistrationSheet by remember { mutableStateOf(false) }
    var registrationLabel by remember { mutableStateOf("") }
    var registrationLocation by remember { mutableStateOf("") }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color(0xFF64B5F6),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Camera access is needed to scan barcodes and QR codes for your morning alarm dismissal mission.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                    ) {
                        Text("Grant Camera Permission", color = Color.White)
                    }
                    if (onCancelOrSkip != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onCancelOrSkip) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("barcode_scanner_view")
    ) {
        // 1. Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val analyzer = AdvancedBarcodeAnalyzer { scannedValue, formatName ->
                            if (!showRegistrationSheet) {
                                lastScannedText = scannedValue
                                lastScannedFormat = formatName

                                if (scannerMode == ScannerMode.REGISTER_EXISTING) {
                                    // Pause scanner and open registration sheet
                                    registrationLabel = defaultLabelSuggestion(formatName)
                                    showRegistrationSheet = true
                                } else {
                                    // Dismiss verify mode: compare value and optional format
                                    val valueMatches = targetBarcode.isNullOrBlank() ||
                                            scannedValue.trim().equals(targetBarcode.trim(), ignoreCase = true)
                                    val formatMatches = targetFormat.isNullOrBlank() ||
                                            formatName.equals(targetFormat.trim(), ignoreCase = true)

                                    if (valueMatches && formatMatches) {
                                        matchError = false
                                        onBarcodeMatched()
                                    } else {
                                        matchError = true
                                    }
                                }
                            }
                        }

                        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        cameraInstance = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("BarcodeScannerView", "Camera binding failed", e)
                        showManualInput = true
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Viewfinder Reticle Overlay & Animated Laser
        val infiniteTransition = rememberInfiniteTransition(label = "laser")
        val laserPosition by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "laser_y"
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(2.5.dp, if (matchError) Color(0xFFFF5252) else Color(0xFF64B5F6), RoundedCornerShape(20.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height * laserPosition
                drawLine(
                    color = if (matchError) Color(0xFFFF5252) else Color(0xFFFF4081),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 3f
                )
            }
        }

        // 3. Top Header Bar with Flashlight Toggle and Close Button
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scanner",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (scannerMode == ScannerMode.REGISTER_EXISTING) "Scan Existing Household Code" else "Scan Barcode to Dismiss",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row {
                        // Flashlight Toggle
                        IconButton(
                            onClick = {
                                isTorchEnabled = !isTorchEnabled
                                cameraInstance?.cameraControl?.enableTorch(isTorchEnabled)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isTorchEnabled) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Flashlight",
                                tint = if (isTorchEnabled) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (onCancelOrSkip != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onCancelOrSkip,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (scannerMode == ScannerMode.DISMISS_VERIFY) {
                    if (!targetLabel.isNullOrBlank()) {
                        Text(
                            text = "Target: $targetLabel",
                            color = Color(0xFFFFD54F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (!targetBarcode.isNullOrBlank()) {
                        Text(
                            text = "Required Code: $targetBarcode",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    Text(
                        text = "Point camera at any item: toothpaste, cereal box, coffee, or book",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // 4. Bottom Controls & Feedback
        Surface(
            color = Color.Black.copy(alpha = 0.85f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (matchError && lastScannedText != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scanned '${lastScannedText?.take(18)}' does not match target!",
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!showManualInput) {
                    Button(
                        onClick = { showManualInput = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238))
                    ) {
                        Text("Having trouble? Type digits manually", color = Color.White, fontSize = 13.sp)
                    }
                } else {
                    OutlinedTextField(
                        value = manualCodeInput,
                        onValueChange = { manualCodeInput = it },
                        label = { Text("Enter barcode digits", color = Color.LightGray) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(
                            onClick = {
                                if (scannerMode == ScannerMode.REGISTER_EXISTING) {
                                    lastScannedText = manualCodeInput.trim()
                                    lastScannedFormat = "MANUAL_INPUT"
                                    registrationLabel = "Manual Code"
                                    showRegistrationSheet = true
                                } else {
                                    val valueMatches = targetBarcode.isNullOrBlank() ||
                                            manualCodeInput.trim().equals(targetBarcode.trim(), ignoreCase = true)
                                    if (valueMatches) {
                                        onBarcodeMatched()
                                    } else {
                                        matchError = true
                                        lastScannedText = manualCodeInput
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (scannerMode == ScannerMode.REGISTER_EXISTING) "Register Code" else "Verify Code", color = Color.White)
                        }
                    }
                }
            }
        }

        // 5. Registration Modal Dialog (when scanning a new household item)
        if (showRegistrationSheet && lastScannedText != null) {
            val scannedVal = lastScannedText ?: ""
            val isDuplicate = existingCodes.any { it.equals(scannedVal.trim(), ignoreCase = true) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Save Household Barcode",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Scanned Details Tag
                        Surface(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Detected Format: ${lastScannedFormat ?: "UNKNOWN"}",
                                    color = Color(0xFF64B5F6),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = scannedVal,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (isDuplicate) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Note: This code value is already registered in your library.",
                                    color = Color(0xFFFFB74D),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = registrationLabel,
                            onValueChange = { registrationLabel = it },
                            label = { Text("Memorable Label *", color = Color.LightGray) },
                            placeholder = { Text("e.g. Kitchen Coffee Box, Toothpaste") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = registrationLocation,
                            onValueChange = { registrationLocation = it },
                            label = { Text("Location Hint (Optional)", color = Color.LightGray) },
                            placeholder = { Text("e.g. Kitchen Pantry, Bathroom Shelf") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showRegistrationSheet = false
                                    lastScannedText = null
                                }
                            ) {
                                Text("Scan Another", color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    val finalLabel = registrationLabel.ifBlank { "Household Item" }
                                    onCodeRegistered?.invoke(
                                        scannedVal,
                                        lastScannedFormat ?: "QR_CODE",
                                        finalLabel,
                                        registrationLocation.ifBlank { null }
                                    )
                                    showRegistrationSheet = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Save Code", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun defaultLabelSuggestion(format: String?): String {
    return when (format) {
        "EAN_13", "UPC_A", "EAN_8", "UPC_E" -> "Kitchen Product"
        "CODE_128", "CODE_39" -> "Packaged Item"
        else -> "Household QR Code"
    }
}

class AdvancedBarcodeAnalyzer(
    private val onBarcodeDetected: (value: String, format: String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val formats = listOf(
            BarcodeFormat.QR_CODE,
            BarcodeFormat.UPC_A,
            BarcodeFormat.UPC_E,
            BarcodeFormat.EAN_13,
            BarcodeFormat.EAN_8,
            BarcodeFormat.CODE_128,
            BarcodeFormat.CODE_39,
            BarcodeFormat.ITF
        )
        val hints = mapOf<DecodeHintType, Any>(
            DecodeHintType.POSSIBLE_FORMATS to formats,
            DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE
        )
        setHints(hints)
    }

    override fun analyze(imageProxy: ImageProxy) {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val width = imageProxy.width
        val height = imageProxy.height

        val source = PlanarYUVLuminanceSource(
            bytes, width, height, 0, 0, width, height, false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        try {
            val result = reader.decodeWithState(binaryBitmap)
            val codeText = result.text
            val formatName = result.barcodeFormat?.name ?: "QR_CODE"
            if (!codeText.isNullOrBlank()) {
                onBarcodeDetected(codeText, formatName)
            }
        } catch (e: NotFoundException) {
            // Frame does not contain code
        } catch (e: Exception) {
            Log.d("BarcodeAnalyzer", "Decode error", e)
        } finally {
            reader.reset()
            imageProxy.close()
        }
    }
}
