package com.example.ui.screens.alarm

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.RegisteredCodeEntity
import com.example.ui.MainViewModel
import com.example.util.QrCardDesignConfig
import com.example.util.QrCodeStudioUtil
import com.example.util.QrStudioTemplate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyCodesLibraryScreen(
    viewModel: MainViewModel,
    onNavigateToStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val registeredCodes by viewModel.registeredCodes.collectAsStateWithLifecycle()
    val allAlarms by viewModel.allAlarms.collectAsStateWithLifecycle()

    var showScannerForNewItem by remember { mutableStateOf(false) }
    var codeToTest by remember { mutableStateOf<RegisteredCodeEntity?>(null) }
    var codeToEdit by remember { mutableStateOf<RegisteredCodeEntity?>(null) }
    var codeToDelete by remember { mutableStateOf<RegisteredCodeEntity?>(null) }

    // Test Scan Success state
    var testScanSuccessMessage by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize().testTag("my_codes_library_screen")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Card with Action Buttons
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Registered Code Library",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Scan existing household barcodes (toothpaste, coffee box, books) or generate custom printable cards.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showScannerForNewItem = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Item Barcode", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onNavigateToStudio,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Custom Studio", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Test scan success banner
            if (testScanSuccessMessage != null) {
                item {
                    Surface(
                        color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = testScanSuccessMessage ?: "",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { testScanSuccessMessage = null }) {
                                Text("✕", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Codes List
            if (registeredCodes.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No registered codes yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Grab any product around your home (toothpaste, book, cereal box) and scan its barcode, or create a custom card in QR Studio.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showScannerForNewItem = true }) {
                                Text("Scan First Household Code")
                            }
                        }
                    }
                }
            } else {
                items(registeredCodes, key = { it.id }) { codeEntity ->
                    val linkedAlarms = allAlarms.filter {
                        it.registeredCodeId == codeEntity.id ||
                                (it.targetBarcode != null && it.targetBarcode.equals(codeEntity.codeValue, ignoreCase = true))
                    }

                    RegisteredCodeCard(
                        code = codeEntity,
                        linkedAlarmTitles = linkedAlarms.map { it.title },
                        onTestScan = { codeToTest = codeEntity },
                        onEdit = { codeToEdit = codeEntity },
                        onDelete = { codeToDelete = codeEntity },
                        onShareOrPrint = {
                            val config = QrCardDesignConfig(
                                template = QrStudioTemplate.fromId(codeEntity.designTemplate),
                                title = codeEntity.label,
                                locationHint = codeEntity.locationHint,
                                codeValue = codeEntity.codeValue
                            )
                            val uri = QrCodeStudioUtil.exportPng(context, config)
                            if (uri != null) {
                                QrCodeStudioUtil.shareFile(context, uri, "image/png", "Share QR Code")
                            } else {
                                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // Camera Scanner Modal: Scanning a new household item
        if (showScannerForNewItem) {
            BarcodeScannerView(
                scannerMode = ScannerMode.REGISTER_EXISTING,
                existingCodes = registeredCodes.map { it.codeValue },
                onCodeRegistered = { value, format, label, location ->
                    val newCode = RegisteredCodeEntity(
                        codeValue = value,
                        codeFormat = format,
                        label = label,
                        locationHint = location,
                        isCustomGenerated = false,
                        createdAt = System.currentTimeMillis()
                    )
                    viewModel.saveRegisteredCode(newCode)
                    showScannerForNewItem = false
                    Toast.makeText(context, "Saved barcode '$label'", Toast.LENGTH_SHORT).show()
                },
                onCancelOrSkip = {
                    showScannerForNewItem = false
                }
            )
        }

        // Camera Scanner Modal: Testing an existing code
        codeToTest?.let { testTarget ->
            BarcodeScannerView(
                targetBarcode = testTarget.codeValue,
                targetLabel = testTarget.label,
                targetFormat = testTarget.codeFormat,
                scannerMode = ScannerMode.DISMISS_VERIFY,
                onBarcodeMatched = {
                    viewModel.recordCodeScanned(testTarget.id)
                    testScanSuccessMessage = "✓ Verified: '${testTarget.label}' scans perfectly!"
                    codeToTest = null
                },
                onCancelOrSkip = {
                    codeToTest = null
                }
            )
        }

        // Edit Code Dialog
        codeToEdit?.let { target ->
            var editLabel by remember { mutableStateOf(target.label) }
            var editLocation by remember { mutableStateOf(target.locationHint ?: "") }

            AlertDialog(
                onDismissRequest = { codeToEdit = null },
                title = { Text("Edit Code Details") },
                text = {
                    Column {
                        Text(
                            text = "Code: ${target.codeValue}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editLabel,
                            onValueChange = { editLabel = it },
                            label = { Text("Label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editLocation,
                            onValueChange = { editLocation = it },
                            label = { Text("Location Hint") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveRegisteredCode(
                                target.copy(
                                    label = editLabel.ifBlank { target.label },
                                    locationHint = editLocation.ifBlank { null }
                                )
                            )
                            codeToEdit = null
                        }
                    ) {
                        Text("Save Changes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { codeToEdit = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        codeToDelete?.let { target ->
            val linkedAlarms = allAlarms.filter {
                it.registeredCodeId == target.id ||
                        (it.targetBarcode != null && it.targetBarcode.equals(target.codeValue, ignoreCase = true))
            }

            AlertDialog(
                onDismissRequest = { codeToDelete = null },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Delete Code?") },
                text = {
                    Column {
                        Text("Are you sure you want to remove \"${target.label}\" from your library?")
                        if (linkedAlarms.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Warning: This code is currently used by ${linkedAlarms.size} alarm(s): ${linkedAlarms.joinToString { it.title }}.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteRegisteredCode(target)
                            codeToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { codeToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisteredCodeCard(
    code: RegisteredCodeEntity,
    linkedAlarmTitles: List<String>,
    onTestScan: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShareOrPrint: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Label and Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = code.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!code.locationHint.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = code.locationHint,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = code.codeFormat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Code value display
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = code.codeValue,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (code.isCustomGenerated) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Studio Card",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Linked alarms badge
            if (linkedAlarmTitles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Used in: ${linkedAlarmTitles.joinToString()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Test Scan Button
                OutlinedButton(onClick = onTestScan) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Scan", fontSize = 12.sp)
                }

                Row {
                    if (code.isCustomGenerated) {
                        IconButton(onClick = onShareOrPrint) {
                            Icon(Icons.Default.Share, contentDescription = "Share Card", modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Details", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Code", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
