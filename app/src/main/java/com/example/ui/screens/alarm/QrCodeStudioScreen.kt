package com.example.ui.screens.alarm

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.RegisteredCodeEntity
import com.example.ui.MainViewModel
import com.example.util.PdfPrintPageSize
import com.example.util.QrCardDesignConfig
import com.example.util.QrCodeStudioUtil
import com.example.util.QrStudioTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QrCodeStudioScreen(
    viewModel: MainViewModel,
    initialCodeValue: String? = null,
    initialLabel: String? = null,
    onNavigateToMyCodes: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTemplate by remember { mutableStateOf(QrStudioTemplate.MINIMAL_BW) }
    var codeValue by remember {
        mutableStateOf(initialCodeValue ?: QrCodeStudioUtil.generateUniqueCodeString())
    }
    var cardTitle by remember { mutableStateOf(initialLabel ?: "Morning Wake-Up Mission") }
    var locationHint by remember { mutableStateOf("Kitchen Coffee Maker") }
    var subtext by remember { mutableStateOf("Scan barcode to dismiss alarm & awaken") }
    var showCutLines by remember { mutableStateOf(true) }

    // Live rendered bitmap
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(false) }

    // PDF Export Dialog
    var showPdfDialog by remember { mutableStateOf(false) }
    var selectedPageSize by remember { mutableStateOf(PdfPrintPageSize.A4) }

    val currentConfig = remember(selectedTemplate, codeValue, cardTitle, locationHint, subtext, showCutLines) {
        QrCardDesignConfig(
            template = selectedTemplate,
            title = cardTitle.ifBlank { "Alarm Dismissal Code" },
            locationHint = locationHint.ifBlank { null },
            subtext = subtext.ifBlank { "Scan to dismiss alarm" },
            codeValue = codeValue.ifBlank { "RHYTHM-FOCUS" },
            showCutLines = showCutLines
        )
    }

    // Re-render preview bitmap whenever configuration changes
    LaunchedEffect(currentConfig) {
        isRendering = true
        withContext(Dispatchers.Default) {
            val bmp = QrCodeStudioUtil.renderStyledCardBitmap(currentConfig, 600, 825)
            withContext(Dispatchers.Main) {
                previewBitmap = bmp
                isRendering = false
            }
        }
    }

    val contrastRatio = remember(currentConfig) {
        QrCodeStudioUtil.calculateContrastRatio(currentConfig.fgColor, currentConfig.bgColor)
    }
    val isContrastGood = contrastRatio >= 4.5

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("qr_code_studio_screen")
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Studio Banner Header
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom QR Code Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Generate and design 100% offline, print-ready mission cards for your bedroom or kitchen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        // 2. Interactive Live Card Preview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Print Preview",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        // Contrast Safety Indicator
                        Surface(
                            color = if (isContrastGood) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFC62828).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isContrastGood) Icons.Default.Check else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isContrastGood) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isContrastGood) "Contrast Pass (%.1f:1)".format(contrastRatio) else "Low Contrast!",
                                    color = if (isContrastGood) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rendered Card Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(800f / 1100f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = "QR Code Card Preview",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text("Rendering card…", color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Action Buttons Under Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val entity = RegisteredCodeEntity(
                                    codeValue = currentConfig.codeValue,
                                    codeFormat = "QR_CODE",
                                    label = currentConfig.title,
                                    locationHint = currentConfig.locationHint,
                                    isCustomGenerated = true,
                                    designTemplate = selectedTemplate.templateId,
                                    createdAt = System.currentTimeMillis()
                                )
                                viewModel.saveRegisteredCode(entity) {
                                    Toast.makeText(context, "Saved to My Codes library!", Toast.LENGTH_SHORT).show()
                                    onNavigateToMyCodes?.invoke()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to My Codes", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val uri = QrCodeStudioUtil.exportPng(context, currentConfig)
                                if (uri != null) {
                                    QrCodeStudioUtil.shareFile(context, uri, "image/png", "Share QR Card")
                                } else {
                                    Toast.makeText(context, "Failed to generate PNG", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        }

                        OutlinedButton(
                            onClick = { showPdfDialog = true }
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Print PDF", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3. Design Templates Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FormatPaint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Design Templates (${QrStudioTemplate.entries.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(QrStudioTemplate.entries) { tmpl ->
                            val isSelected = selectedTemplate == tmpl
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .width(135.dp)
                                    .clickable { selectedTemplate = tmpl }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Mini Color Dot Preview
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(tmpl.defaultBgColor))
                                                .border(0.5.dp, Color.Gray, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(tmpl.defaultFgColor))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(Color(tmpl.accentColor))
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = tmpl.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = tmpl.category,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Code Value & Content Customization
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Card Information & Code Value",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Code Value Field with Regenerate Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = codeValue,
                            onValueChange = { codeValue = it },
                            label = { Text("Unique Code Text / ID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                codeValue = QrCodeStudioUtil.generateUniqueCodeString()
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate Random ID")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = cardTitle,
                        onValueChange = { cardTitle = it },
                        label = { Text("Card Title") },
                        placeholder = { Text("e.g. Morning Wake-Up Target") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = locationHint,
                        onValueChange = { locationHint = it },
                        label = { Text("Room / Location Tag") },
                        placeholder = { Text("e.g. Kitchen Coffee Box, Bathroom Mirror") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = subtext,
                        onValueChange = { subtext = it },
                        label = { Text("Footer Instruction Subtext") },
                        placeholder = { Text("e.g. Scan to dismiss alarm and start morning") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Show Scissor Cutting Guidelines", fontSize = 13.sp)
                        Switch(checked = showCutLines, onCheckedChange = { showCutLines = it })
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // PDF Page Size & Print Sheet Modal Dialog
    if (showPdfDialog) {
        AlertDialog(
            onDismissRequest = { showPdfDialog = false },
            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Print & Export PDF") },
            text = {
                Column {
                    Text(
                        text = "Choose paper format for direct home printing:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    PdfPrintPageSize.entries.forEach { sizeOption ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPageSize = sizeOption }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPageSize == sizeOption,
                                onClick = { selectedPageSize = sizeOption }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(sizeOption.displayName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                val desc = when (sizeOption) {
                                    PdfPrintPageSize.A4 -> "Standard full page print"
                                    PdfPrintPageSize.A5 -> "Compact half sheet print"
                                    PdfPrintPageSize.STICKER_SINGLE -> "Single index card / sticker size"
                                    PdfPrintPageSize.STICKER_SHEET_A4 -> "4 cards tiled on 1 page with cutting guidelines"
                                }
                                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pdfUri = QrCodeStudioUtil.exportPdf(context, currentConfig, selectedPageSize)
                        if (pdfUri != null) {
                            showPdfDialog = false
                            // Open Android print manager
                            QrCodeStudioUtil.printPdf(context, pdfUri, "Rhythm_QR_${currentConfig.title}")
                        } else {
                            Toast.makeText(context, "PDF export failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print Directly")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val pdfUri = QrCodeStudioUtil.exportPdf(context, currentConfig, selectedPageSize)
                        if (pdfUri != null) {
                            showPdfDialog = false
                            QrCodeStudioUtil.shareFile(context, pdfUri, "application/pdf", "Share PDF Document")
                        } else {
                            Toast.makeText(context, "PDF export failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share PDF")
                }
            }
        )
    }
}
