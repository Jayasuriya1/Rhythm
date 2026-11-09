package com.example.ui.screens.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.HealthConnectDailySummaryEntity
import com.example.health.HealthCategory
import com.example.health.HealthConnectAvailability
import com.example.ui.MainViewModel
import com.example.ui.theme.HealthMetricEnergy
import com.example.ui.theme.HealthMetricExercise
import com.example.ui.theme.HealthMetricHeart
import com.example.ui.theme.HealthMetricHrv
import com.example.ui.theme.HealthMetricHydration
import com.example.ui.theme.HealthMetricOxygen
import com.example.ui.theme.HealthMetricRespiratory
import com.example.ui.theme.HealthMetricRestingHeart
import com.example.ui.theme.HealthMetricSleep
import com.example.ui.theme.HealthMetricSteps
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class HealthDateFilter(val label: String, val daysBack: Long) {
    TODAY("Today", 0),
    YESTERDAY("Yesterday", 1),
    LAST_7_DAYS("7 Days", 7),
    LAST_30_DAYS("30 Days", 30)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectDashboardScreen(
    viewModel: MainViewModel,
    isExpandedScreen: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateSleepAnalytics: () -> Unit,
    onNavigateHeartRateAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val healthManager = viewModel.healthConnectManager
    val healthState by healthManager.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var activeDateFilter by remember { mutableStateOf(HealthDateFilter.TODAY) }
    var syncFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // Query summary from Room database for selected view date
    val viewDateStr = remember(activeDateFilter, selectedDate) {
        when (activeDateFilter) {
            HealthDateFilter.TODAY -> LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            HealthDateFilter.YESTERDAY -> LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            else -> selectedDate
        }
    }

    val dailySummary by viewModel.getHealthConnectSummaryForDate(viewDateStr).collectAsStateWithLifecycle(initialValue = null)

    // Permission launcher for Health Connect
    val requestPermissionActivityContract = remember {
        PermissionController.createRequestPermissionResultContract()
    }
    val permissionLauncher = rememberLauncherForActivityResult(requestPermissionActivityContract) { grantedPermissions ->
        scope.launch {
            healthManager.refreshGrantedPermissions()
            val today = LocalDate.now()
            healthManager.syncDateRange(today.minusDays(activeDateFilter.daysBack), today)
        }
    }

    LaunchedEffect(Unit) {
        healthManager.checkAvailabilityAndPermissions()
        healthManager.refreshGrantedPermissions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Health & Wearables",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Google Health Connect Verified Integration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("health_dashboard_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                syncFeedbackMessage = "Syncing with Health Connect..."
                                val today = LocalDate.now()
                                val start = today.minusDays(activeDateFilter.daysBack)
                                val result = healthManager.syncDateRange(start, today)
                                syncFeedbackMessage = if (result.isSuccess) {
                                    "Synced ${result.syncedDatesCount} day(s), ${result.recordsReadCount} record(s)"
                                } else {
                                    result.errorMessage ?: "Sync encountered an error"
                                }
                            }
                        },
                        enabled = !healthState.isSyncing,
                        modifier = Modifier.testTag("health_dashboard_manual_sync_button")
                    ) {
                        if (healthState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Health Connect",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (isExpandedScreen) {
            // Tablet 2-column landscape layout (Xiaomi Pad 7)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: Status overview, date filters, core metrics
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        HealthConnectConnectionBanner(
                            state = healthState,
                            onConnectClick = {
                                val perms = healthManager.getRequiredPermissions()
                                permissionLauncher.launch(perms)
                            },
                            onOpenPlayStore = { healthManager.openHealthConnectPlayStore(context) }
                        )
                    }

                    item {
                        DateFilterRow(
                            activeFilter = activeDateFilter,
                            onSelectFilter = { filter ->
                                activeDateFilter = filter
                                scope.launch {
                                    val today = LocalDate.now()
                                    healthManager.syncDateRange(today.minusDays(filter.daysBack), today)
                                }
                            }
                        )
                    }

                    if (syncFeedbackMessage != null) {
                        item {
                            SyncFeedbackPill(message = syncFeedbackMessage!!)
                        }
                    }

                    item {
                        PrimaryMetricsOverview(
                            summary = dailySummary,
                            onNavigateSleep = onNavigateSleepAnalytics,
                            onNavigateHeart = onNavigateHeartRateAnalytics
                        )
                    }
                }

                // Right Column: Cardiovascular, respiratory, hydration & source attribution
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SecondaryHealthMetricsList(
                            summary = dailySummary,
                            onNavigateHeart = onNavigateHeartRateAnalytics
                        )
                    }

                    item {
                        ContributingSourcesCard(
                            sources = healthState.contributingSources
                        )
                    }
                }
            }
        } else {
            // Phone single-column layout
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    HealthConnectConnectionBanner(
                        state = healthState,
                        onConnectClick = {
                            val perms = healthManager.getRequiredPermissions()
                            permissionLauncher.launch(perms)
                        },
                        onOpenPlayStore = { healthManager.openHealthConnectPlayStore(context) }
                    )
                }

                item {
                    DateFilterRow(
                        activeFilter = activeDateFilter,
                        onSelectFilter = { filter ->
                            activeDateFilter = filter
                            scope.launch {
                                val today = LocalDate.now()
                                healthManager.syncDateRange(today.minusDays(filter.daysBack), today)
                            }
                        }
                    )
                }

                if (syncFeedbackMessage != null) {
                    item {
                        SyncFeedbackPill(message = syncFeedbackMessage!!)
                    }
                }

                item {
                    PrimaryMetricsOverview(
                        summary = dailySummary,
                        onNavigateSleep = onNavigateSleepAnalytics,
                        onNavigateHeart = onNavigateHeartRateAnalytics
                    )
                }

                item {
                    SecondaryHealthMetricsList(
                        summary = dailySummary,
                        onNavigateHeart = onNavigateHeartRateAnalytics
                    )
                }

                item {
                    ContributingSourcesCard(
                        sources = healthState.contributingSources
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun HealthConnectConnectionBanner(
    state: com.example.health.HealthConnectState,
    onConnectClick: () -> Unit,
    onOpenPlayStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = when (state.availability) {
                            HealthConnectAvailability.AVAILABLE -> if (state.grantedPermissions.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (state.availability) {
                                    HealthConnectAvailability.AVAILABLE -> if (state.grantedPermissions.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.Watch
                                    else -> Icons.Default.ErrorOutline
                                },
                                contentDescription = null,
                                tint = when (state.availability) {
                                    HealthConnectAvailability.AVAILABLE -> if (state.grantedPermissions.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.error
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = when (state.availability) {
                                HealthConnectAvailability.AVAILABLE -> if (state.grantedPermissions.isNotEmpty()) "Connected to Health Connect" else "Permissions Required"
                                HealthConnectAvailability.UPDATE_REQUIRED -> "Health Connect Update Needed"
                                else -> "Health Connect Not Installed"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (state.availability) {
                                HealthConnectAvailability.AVAILABLE -> if (state.grantedPermissions.isNotEmpty()) "${state.grantedPermissions.size} data permissions authorized" else "Authorize read access for wearable metrics"
                                else -> "Install or update Google Health Connect via Play Store"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action button if not yet granted or missing
            if (state.availability == HealthConnectAvailability.AVAILABLE && state.grantedPermissions.isEmpty()) {
                Button(
                    onClick = onConnectClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("health_connect_authorize_button")
                ) {
                    Icon(imageVector = Icons.Default.Watch, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Authorize Health Connect Permissions")
                }
            } else if (state.availability != HealthConnectAvailability.AVAILABLE) {
                OutlinedButton(
                    onClick = onOpenPlayStore,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Health Connect on Google Play")
                }
            }
        }
    }
}

@Composable
fun DateFilterRow(
    activeFilter: HealthDateFilter,
    onSelectFilter: (HealthDateFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HealthDateFilter.values()) { filter ->
            FilterChip(
                selected = activeFilter == filter,
                onClick = { onSelectFilter(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

@Composable
fun SyncFeedbackPill(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PrimaryMetricsOverview(
    summary: HealthConnectDailySummaryEntity?,
    onNavigateSleep: () -> Unit,
    onNavigateHeart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Steps & Distance
        MetricCard(
            title = "Steps & Distance",
            valueText = summary?.let { String.format(Locale.US, "%,d steps", it.steps) } ?: "No data",
            subText = summary?.let { String.format(Locale.US, "%.2f km", it.distanceMeters / 1000.0) } ?: "Connect wearable",
            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
            accentColor = HealthMetricSteps,
            sourceApp = summary?.primarySourceApp
        )

        // Sleep Duration & Stages
        MetricCard(
            title = "Sleep Duration",
            valueText = summary?.let {
                val hrs = it.sleepDurationMinutes / 60
                val mins = it.sleepDurationMinutes % 60
                "${hrs}h ${mins}m"
            } ?: "No sleep record",
            subText = if (summary?.hasSleepStages == true) "Hypnogram stages recorded" else "Single interval (stages unrecorded)",
            icon = Icons.Default.Bedtime,
            accentColor = HealthMetricSleep,
            actionLabel = "View Sleep Hypnogram",
            onActionClick = onNavigateSleep,
            sourceApp = summary?.primarySourceApp
        )

        // Heart Rate
        MetricCard(
            title = "Heart Rate",
            valueText = summary?.avgHeartRateBpm?.let { String.format(Locale.US, "%.0f bpm avg", it) } ?: "No HR samples",
            subText = summary?.let {
                if (it.minHeartRateBpm != null && it.maxHeartRateBpm != null) {
                    String.format(Locale.US, "Range: %.0f – %.0f bpm", it.minHeartRateBpm, it.maxHeartRateBpm)
                } else null
            },
            icon = Icons.Default.Favorite,
            accentColor = HealthMetricHeart,
            actionLabel = "View HR Chart",
            onActionClick = onNavigateHeart,
            sourceApp = summary?.primarySourceApp
        )
    }
}

@Composable
fun SecondaryHealthMetricsList(
    summary: HealthConnectDailySummaryEntity?,
    onNavigateHeart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Resting Heart Rate (Only if genuine record exists)
        MetricCard(
            title = "Resting Heart Rate",
            valueText = summary?.restingHeartRateBpm?.let { String.format(Locale.US, "%.0f bpm", it) } ?: "Unavailable",
            subText = if (summary?.restingHeartRateBpm != null) "Verified resting measurement" else "No resting HR record logged by source",
            icon = Icons.Default.Favorite,
            accentColor = HealthMetricRestingHeart,
            sourceApp = summary?.primarySourceApp
        )

        // HRV (Heart Rate Variability RMSSD)
        MetricCard(
            title = "Heart Rate Variability (HRV)",
            valueText = summary?.hrvRmssdMs?.let { String.format(Locale.US, "%.1f ms", it) } ?: "Unavailable",
            subText = if (summary?.hrvRmssdMs != null) "RMSSD metric from wearable" else "Source device does not report HRV to Health Connect",
            icon = Icons.Default.Timeline,
            accentColor = HealthMetricHrv,
            sourceApp = summary?.primarySourceApp
        )

        // Active & Total Calories
        MetricCard(
            title = "Energy Burned",
            valueText = summary?.let { String.format(Locale.US, "%.0f kcal active", it.activeCaloriesKcal) } ?: "No data",
            subText = summary?.let { String.format(Locale.US, "%.0f kcal total", it.totalCaloriesKcal) },
            icon = Icons.Default.LocalFireDepartment,
            accentColor = HealthMetricEnergy,
            sourceApp = summary?.primarySourceApp
        )

        // Blood Oxygen (SpO2) & Respiratory Rate
        MetricCard(
            title = "Oxygen & Breathing",
            valueText = summary?.oxygenSaturationPercent?.let { String.format(Locale.US, "%.1f%% SpO₂", it) } ?: "SpO₂ not reported",
            subText = summary?.respiratoryRateRpm?.let { String.format(Locale.US, "%.1f breaths/min", it) } ?: "Respiratory rate not reported",
            icon = Icons.Default.Air,
            accentColor = HealthMetricOxygen,
            sourceApp = summary?.primarySourceApp
        )

        // Imported Hydration
        MetricCard(
            title = "Imported Water Intake",
            valueText = summary?.let { "${it.waterMl} mL" } ?: "0 mL",
            subText = "Imported from connected tracking apps",
            icon = Icons.Default.LocalDrink,
            accentColor = HealthMetricHydration,
            sourceApp = summary?.primarySourceApp
        )

        // Exercise & Workouts
        MetricCard(
            title = "Exercise Sessions",
            valueText = summary?.let { "${it.exerciseSessionsCount} session(s) (${it.exerciseMinutes}m)" } ?: "0 sessions",
            subText = "Active workout sessions",
            icon = Icons.Default.FitnessCenter,
            accentColor = HealthMetricExercise,
            sourceApp = summary?.primarySourceApp
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    valueText: String,
    subText: String?,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    sourceApp: String? = null
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onActionClick != null) Modifier.clickable { onActionClick() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.18f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }

                if (sourceApp != null) {
                    val friendlyName = sourceApp.substringAfterLast(".").replaceFirstChar { it.uppercase() }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = friendlyName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = valueText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (valueText.contains("No") || valueText.contains("Unavailable")) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
            )

            if (subText != null) {
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (actionLabel != null && onActionClick != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ContributingSourcesCard(
    sources: Set<String>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Connected Source Applications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (sources.isEmpty()) {
                Text(
                    text = "No wearable or fitness sources have synced records yet. Connect your wearable app (such as Samsung Health, Garmin Connect, Google Fit, or Pixel Watch) to Health Connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Deduplication active across ${sources.size} reporting app(s):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (src in sources) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = src.substringAfterLast("."),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
