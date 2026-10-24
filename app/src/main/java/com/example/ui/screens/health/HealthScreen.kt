package com.example.ui.screens.health

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.HealthCheckinEntity
import com.example.data.entity.MedicationEntity
import com.example.data.entity.MedicationIntakeEntity
import com.example.data.entity.SleepEntryEntity
import com.example.data.entity.WaterEntryEntity
import com.example.ui.MainViewModel
import com.example.ui.components.CalmEmptyState
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.util.DateTimeUtils

@Composable
fun HealthScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Daily State", "Sleep", "Hydration", "Medications")

    val sleepEntry by viewModel.sleepForDate.collectAsStateWithLifecycle()
    val healthCheckin by viewModel.healthForDate.collectAsStateWithLifecycle()
    val waterEntries by viewModel.waterEntriesForDate.collectAsStateWithLifecycle()
    val allMeds by viewModel.allMedications.collectAsStateWithLifecycle()
    val intakes by viewModel.medicationIntakesForDate.collectAsStateWithLifecycle()

    var showSleepDialog by remember { mutableStateOf(false) }
    var showAddMedDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal)
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> MoodAndDailyStateSection(
                health = healthCheckin,
                onSave = { m, e, s, i, a, notes ->
                    viewModel.saveHealthCheckin(m, e, s, i, a, notes)
                }
            )
            1 -> SleepSection(
                sleep = sleepEntry,
                onOpenDialog = { showSleepDialog = true },
                onDeleteSleep = { viewModel.deleteSleep() }
            )
            2 -> HydrationSection(
                entries = waterEntries,
                targetMl = 2000,
                onAddWater = { viewModel.addWater(it) },
                onDeleteEntry = { viewModel.removeWaterEntry(it) }
            )
            3 -> MedicationSection(
                medications = allMeds,
                intakes = intakes,
                onAddMedClick = { showAddMedDialog = true },
                onRecordIntake = { med, status, notes ->
                    viewModel.recordMedicationIntake(med, status, notes)
                },
                onDeleteMed = { viewModel.deleteMedication(it) }
            )
        }
    }

    if (showSleepDialog) {
        SleepLogDialog(
            existing = sleepEntry,
            onDismiss = { showSleepDialog = false },
            onConfirm = { bed, wake, quality, notes ->
                viewModel.saveSleepRecord(bed, wake, quality, notes)
                showSleepDialog = false
            }
        )
    }

    if (showAddMedDialog) {
        AddMedicationDialog(
            onDismiss = { showAddMedDialog = false },
            onConfirm = { name, dose, unit, instructions, schedMin, notes ->
                viewModel.addMedication(name, dose, unit, instructions, schedMin, false, notes)
                showAddMedDialog = false
            }
        )
    }
}

@Composable
fun MoodAndDailyStateSection(
    health: HealthCheckinEntity?,
    onSave: (Int?, Int?, Int?, Int?, Int?, String?) -> Unit
) {
    var mood by remember(health) { mutableStateOf(health?.mood) }
    var energy by remember(health) { mutableStateOf(health?.energy) }
    var stress by remember(health) { mutableStateOf(health?.stress) }
    var irritability by remember(health) { mutableStateOf(health?.irritability) }
    var activation by remember(health) { mutableStateOf(health?.activationEnergy) }
    var notes by remember(health) { mutableStateOf(health?.symptomsNotes ?: "") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Measurements are kept distinct rather than combined into a single score, allowing you to notice nuanced patterns without diagnostic judgment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            DiscreteRatingCard(
                title = "Mood",
                description = "Overall emotional valence today",
                currentValue = mood,
                labels = listOf("Very Low", "Low", "Neutral", "Good", "Great"),
                onSelect = { mood = it }
            )
        }

        item {
            DiscreteRatingCard(
                title = "Energy",
                description = "Physical and mental stamina",
                currentValue = energy,
                labels = listOf("Exhausted", "Low", "Moderate", "High", "Very High"),
                onSelect = { energy = it }
            )
        }

        item {
            DiscreteRatingCard(
                title = "Stress Level",
                description = "Internal or environmental pressure felt",
                currentValue = stress,
                labels = listOf("None", "Mild", "Moderate", "High", "Severe"),
                onSelect = { stress = it }
            )
        }

        item {
            DiscreteRatingCard(
                title = "Irritability",
                description = "Shortness of temper or low frustration tolerance",
                currentValue = irritability,
                labels = listOf("Low", "Mild", "Moderate", "High", "Severe"),
                onSelect = { irritability = it }
            )
        }

        item {
            DiscreteRatingCard(
                title = "Activation / Unusual Energy",
                description = "Elevated drive, racing thoughts, or restless surge",
                currentValue = activation,
                labels = listOf("Normal", "Mildly Elevated", "Energetic", "Restless", "Intensely Elevated"),
                onSelect = { activation = it }
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "General Symptoms & Clinician Notes",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Record notes about physical symptoms, headaches, or questions you wish to discuss with your healthcare professional.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("e.g. Mild headache after lunch, felt calm in afternoon") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        maxLines = 4
                    )
                }
            }
        }

        item {
            Button(
                onClick = { onSave(mood, energy, stress, irritability, activation, notes.ifBlank { null }) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_health_checkin_button")
            ) {
                Text("Save Daily State Check-In")
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun DiscreteRatingCard(
    title: String,
    description: String,
    currentValue: Int?,
    labels: List<String>,
    onSelect: (Int?) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (currentValue != null) {
                    Text(
                        text = labels.getOrElse(currentValue - 1) { "" },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                labels.forEachIndexed { index, label ->
                    val rating = index + 1
                    val isSelected = currentValue == rating
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onSelect(if (isSelected) null else rating)
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$rating",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SleepSection(
    sleep: SleepEntryEntity?,
    onOpenDialog: () -> Unit,
    onDeleteSleep: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Previous night's sleep is recorded relative to the morning you woke up. Bedtime and wake time are distinctly recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            if (sleep != null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recorded Sleep",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = onOpenDialog) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Sleep", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = onDeleteSleep) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Sleep", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Duration",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = DateTimeUtils.formatDurationMinutes(sleep.durationMinutes),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Quality (${sleep.qualityRating}/5)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row {
                                        for (i in 1..5) {
                                            Icon(
                                                imageVector = if (i <= sleep.qualityRating) Icons.Default.Star else Icons.Outlined.StarOutline,
                                                contentDescription = null,
                                                tint = if (i <= sleep.qualityRating) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Bedtime: ${DateTimeUtils.formatMinutesToTime(sleep.bedtimeMinutes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Wake up: ${DateTimeUtils.formatMinutesToTime(sleep.wakeTimeMinutes)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!sleep.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Notes: ${sleep.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                CalmEmptyState(
                    icon = Icons.Default.Bedtime,
                    title = "No Sleep Logged for Today",
                    message = "Logging your sleep duration and perceived quality helps anchor your circadian cycle.",
                    actionLabel = "Record Sleep",
                    onActionClick = onOpenDialog
                )
            }
        }
    }
}

@Composable
fun SleepLogDialog(
    existing: SleepEntryEntity?,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, String?) -> Unit
) {
    var bedHour by remember { mutableIntStateOf(existing?.let { it.bedtimeMinutes / 60 } ?: 23) }
    var bedMin by remember { mutableIntStateOf(existing?.let { it.bedtimeMinutes % 60 } ?: 0) }

    var wakeHour by remember { mutableIntStateOf(existing?.let { it.wakeTimeMinutes / 60 } ?: 7) }
    var wakeMin by remember { mutableIntStateOf(existing?.let { it.wakeTimeMinutes % 60 } ?: 0) }

    var quality by remember { mutableIntStateOf(existing?.qualityRating ?: 3) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Sleep Entry" else "Record Sleep Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Bedtime: ${DateTimeUtils.formatMinutesToTime(bedHour * 60 + bedMin)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Slider(
                    value = bedHour.toFloat(),
                    onValueChange = { bedHour = it.toInt() },
                    valueRange = 18f..24f,
                    steps = 5
                )

                Text(
                    text = "Wake up: ${DateTimeUtils.formatMinutesToTime(wakeHour * 60 + wakeMin)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Slider(
                    value = wakeHour.toFloat(),
                    onValueChange = { wakeHour = it.toInt() },
                    valueRange = 4f..12f,
                    steps = 7
                )

                Text(
                    text = "Sleep Quality: $quality / 5",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..5) {
                        IconButton(onClick = { quality = i }) {
                            Icon(
                                imageVector = if (i <= quality) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Rating $i",
                                tint = if (i <= quality) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Sleep notes (dreams, awakenings)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        bedHour * 60 + bedMin,
                        wakeHour * 60 + wakeMin,
                        quality,
                        notes.ifBlank { null }
                    )
                }
            ) {
                Text("Save Sleep")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun HydrationSection(
    entries: List<WaterEntryEntity>,
    targetMl: Int,
    onAddWater: (Int) -> Unit,
    onDeleteEntry: (Long) -> Unit
) {
    val totalMl = entries.sumOf { it.volumeMl }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$totalMl ml",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Daily Target: $targetMl ml",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { (totalMl.toFloat() / targetMl.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Quick Add Serving",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(150, 250, 350, 500).forEach { size ->
                            FilledTonalButton(
                                onClick = { onAddWater(size) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("quick_water_${size}ml")
                            ) {
                                Text("+${size}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Logged Water Servings (${entries.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (entries.isEmpty()) {
            item {
                CalmEmptyState(
                    icon = Icons.Default.LocalDrink,
                    title = "No Water Logged Today",
                    message = "Tap any quick-add button above to start tracking your daily hydration.",
                    actionLabel = null
                )
            }
        } else {
            items(entries) { entry ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.LocalDrink, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "${entry.volumeMl} ml",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { onDeleteEntry(entry.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Entry", tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun MedicationSection(
    medications: List<MedicationEntity>,
    intakes: List<MedicationIntakeEntity>,
    onAddMedClick: () -> Unit,
    onRecordIntake: (MedicationEntity, String, String?) -> Unit,
    onDeleteMed: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Medication records are stored strictly locally and privately. Dismissing a reminder does not automatically mark a dose taken.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scheduled Medications",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = onAddMedClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_medication_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Medication")
                }
            }
        }

        if (medications.isEmpty()) {
            item {
                CalmEmptyState(
                    icon = Icons.Default.Medication,
                    title = "No Medications Configured",
                    message = "Add your prescribed medications, dosages, and target times to maintain a clear intake history.",
                    actionLabel = "Add First Medication",
                    onActionClick = onAddMedClick
                )
            }
        } else {
            items(medications, key = { it.id }) { med ->
                val intake = intakes.firstOrNull { it.medicationId == med.id }
                val status = intake?.status ?: "UNRECORDED"

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${med.name} • ${med.dosage} ${med.unit}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Scheduled: ${DateTimeUtils.formatMinutesToTime(med.scheduledTimeMinutes)} (${med.instructions})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(status = status)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onRecordIntake(med, "TAKEN", null) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Taken", style = MaterialTheme.typography.labelSmall)
                            }
                            FilledTonalButton(
                                onClick = { onRecordIntake(med, "SKIPPED", null) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Skip", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onRecordIntake(med, "DELAYED", null) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Delayed", style = MaterialTheme.typography.labelSmall)
                            }
                            IconButton(onClick = { onDeleteMed(med.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Int, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("mg") }
    var instructions by remember { mutableStateOf("Take with meal") }
    var hour by remember { mutableIntStateOf(8) }
    var min by remember { mutableIntStateOf(0) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Medication") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medication Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dose,
                        onValueChange = { dose = it },
                        label = { Text("Dose") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Prescribed Instructions") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Scheduled Time: ${DateTimeUtils.formatMinutesToTime(hour * 60 + min)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = hour.toFloat(),
                    onValueChange = { hour = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 22
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, dose, unit, instructions, hour * 60 + min, notes.ifBlank { null })
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Medication")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
