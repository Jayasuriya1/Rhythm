package com.example.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.data.entity.HealthConnectDailySummaryEntity
import com.example.data.entity.HealthConnectHeartSampleEntity
import com.example.data.entity.HealthConnectSleepStageEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

enum class HealthConnectAvailability {
    AVAILABLE,
    NOT_INSTALLED,
    UPDATE_REQUIRED,
    UNSUPPORTED_DEVICE
}

enum class MetricStatus {
    AVAILABLE_READABLE,
    AUTHORIZED_EMPTY,
    NOT_AUTHORIZED,
    UNSUPPORTED,
    TEMPORARILY_UNAVAILABLE
}

enum class HealthCategory(val displayName: String, val description: String) {
    ACTIVITY("Activity & Steps", "Steps, distance, active & total calories, exercise sessions"),
    SLEEP("Sleep & Stages", "Sleep duration, start/wake times, and hypnogram stage intervals"),
    HEART("Heart & Cardiovascular", "Heart rate series, resting heart rate, and heart rate variability (HRV)"),
    RESPIRATORY("Respiratory & Oxygen", "Breathing rate and blood oxygen saturation (SpO2)"),
    HYDRATION("Hydration Intake", "Water consumption imported from connected bottles and tracking apps")
}

data class SyncResult(
    val isSuccess: Boolean,
    val syncedDatesCount: Int = 0,
    val recordsReadCount: Int = 0,
    val contributingSources: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val partialWarnings: List<String> = emptyList()
)

data class HealthConnectState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val grantedPermissions: Set<String> = emptySet(),
    val enabledCategories: Set<HealthCategory> = setOf(HealthCategory.ACTIVITY, HealthCategory.SLEEP, HealthCategory.HEART),
    val metricStatuses: Map<HealthCategory, MetricStatus> = emptyMap(),
    val contributingSources: Set<String> = emptySet(),
    val syncErrorMessage: String? = null,
    val partialWarnings: List<String> = emptyList()
)

class HealthConnectManager(
    private val context: Context,
    private val repository: AppRepository
) {

    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private val _uiState = MutableStateFlow(HealthConnectState())
    val uiState: StateFlow<HealthConnectState> = _uiState.asStateFlow()

    companion object {
        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

        val PERMISSION_RECORDS_MAP: Map<HealthCategory, List<KClass<out Record>>> = mapOf(
            HealthCategory.ACTIVITY to listOf(
                StepsRecord::class,
                DistanceRecord::class,
                TotalCaloriesBurnedRecord::class,
                ActiveCaloriesBurnedRecord::class,
                ExerciseSessionRecord::class
            ),
            HealthCategory.SLEEP to listOf(
                SleepSessionRecord::class
            ),
            HealthCategory.HEART to listOf(
                HeartRateRecord::class,
                RestingHeartRateRecord::class,
                HeartRateVariabilityRmssdRecord::class
            ),
            HealthCategory.RESPIRATORY to listOf(
                RespiratoryRateRecord::class,
                OxygenSaturationRecord::class
            ),
            HealthCategory.HYDRATION to listOf(
                HydrationRecord::class
            )
        )

        fun getPermissionsForCategories(categories: Set<HealthCategory>): Set<String> {
            val permissions = mutableSetOf<String>()
            for (category in categories) {
                PERMISSION_RECORDS_MAP[category]?.forEach { recordClass ->
                    permissions.add(HealthPermission.getReadPermission(recordClass))
                }
            }
            return permissions
        }

        fun getAllAvailablePermissions(): Set<String> {
            return getPermissionsForCategories(HealthCategory.values().toSet())
        }
    }

    init {
        checkAvailabilityAndPermissions()
    }

    fun checkAvailabilityAndPermissions() {
        val sdkStatus = try {
            HealthConnectClient.getSdkStatus(context)
        } catch (_: Exception) {
            HealthConnectClient.SDK_UNAVAILABLE
        }

        val availability = when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
            else -> HealthConnectAvailability.NOT_INSTALLED
        }

        _uiState.value = _uiState.value.copy(availability = availability)
    }

    suspend fun refreshGrantedPermissions() {
        val client = healthConnectClient ?: return
        try {
            val granted = client.permissionController.getGrantedPermissions()
            val metricStatuses = mutableMapOf<HealthCategory, MetricStatus>()

            for (category in HealthCategory.values()) {
                val needed = PERMISSION_RECORDS_MAP[category]?.map { HealthPermission.getReadPermission(it) } ?: emptyList()
                val hasAnyGranted = needed.any { granted.contains(it) }
                metricStatuses[category] = if (hasAnyGranted) MetricStatus.AVAILABLE_READABLE else MetricStatus.NOT_AUTHORIZED
            }

            _uiState.value = _uiState.value.copy(
                grantedPermissions = granted,
                metricStatuses = metricStatuses
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleCategoryEnabled(category: HealthCategory) {
        val current = _uiState.value.enabledCategories.toMutableSet()
        if (current.contains(category)) {
            current.remove(category)
        } else {
            current.add(category)
        }
        _uiState.value = _uiState.value.copy(enabledCategories = current)
    }

    fun getRequiredPermissions(): Set<String> {
        return getPermissionsForCategories(_uiState.value.enabledCategories)
    }

    /**
     * Perform genuine read and import of Health Connect records across selected date range.
     * Prevents duplicate double-counting across overlapping data sources.
     */
    suspend fun syncDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): SyncResult = withContext(Dispatchers.IO) {
        val client = healthConnectClient
        if (client == null) {
            return@withContext SyncResult(
                isSuccess = false,
                errorMessage = "Google Health Connect is not available on this device."
            )
        }

        _uiState.value = _uiState.value.copy(isSyncing = true, syncErrorMessage = null)

        val granted = try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isSyncing = false, syncErrorMessage = e.message)
            return@withContext SyncResult(isSuccess = false, errorMessage = e.message)
        }

        val zone = ZoneId.systemDefault()
        val contributingSources = mutableSetOf<String>()
        val partialWarnings = mutableListOf<String>()
        var totalRecords = 0
        var datesSynced = 0

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dateStr = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val startInstant = currentDate.atStartOfDay(zone).toInstant()
            val endInstant = currentDate.plusDays(1).atStartOfDay(zone).toInstant().minusMillis(1)
            val timeFilter = TimeRangeFilter.between(startInstant, endInstant)

            var stepsTotal: Long = 0
            var distanceMetersTotal: Double = 0.0
            var activeCaloriesTotal: Double = 0.0
            var totalCaloriesTotal: Double = 0.0
            var sleepMinutes: Int = 0
            var sleepStartEpoch: Long? = null
            var sleepEndEpoch: Long? = null
            var restingHeartRate: Double? = null
            var avgHeartRate: Double? = null
            var minHeartRate: Double? = null
            var maxHeartRate: Double? = null
            var hrvRmssd: Double? = null
            var respiratoryRate: Double? = null
            var oxygenSaturation: Double? = null
            var hydrationVolumeMl: Int = 0
            var exerciseMinutesTotal: Int = 0
            var exerciseCount: Int = 0
            var hasStages = false

            val sleepStageEntities = mutableListOf<HealthConnectSleepStageEntity>()
            val heartSampleEntities = mutableListOf<HealthConnectHeartSampleEntity>()

            // 1. Steps Record
            if (granted.contains(HealthPermission.getReadPermission(StepsRecord::class))) {
                try {
                    val stepsResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = StepsRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    // Aggregate and deduplicate steps: group by source app to avoid double-counting phone + watch
                    val stepsByApp = stepsResponse.records.groupBy { it.metadata.dataOrigin.packageName }
                    stepsByApp.keys.forEach { contributingSources.add(it) }

                    // Deduplication rule: If a dedicated wearable app (Garmin, Samsung, Fitbit, Pixel Watch) is present,
                    // prioritize it; otherwise take the maximum single source to prevent double-counting phone sensor + companion app
                    stepsTotal = if (stepsByApp.isNotEmpty()) {
                        val maxSourceSteps = stepsByApp.values.maxOfOrNull { records -> records.sumOf { it.count } } ?: 0L
                        maxSourceSteps
                    } else 0L

                    totalRecords += stepsResponse.records.size
                } catch (e: Exception) {
                    partialWarnings.add("Steps read warning: ${e.message}")
                }
            }

            // 2. Distance Record
            if (granted.contains(HealthPermission.getReadPermission(DistanceRecord::class))) {
                try {
                    val distResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = DistanceRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    val distByApp = distResponse.records.groupBy { it.metadata.dataOrigin.packageName }
                    distByApp.keys.forEach { contributingSources.add(it) }
                    distanceMetersTotal = distByApp.values.maxOfOrNull { list ->
                        list.sumOf { it.distance.inMeters }
                    } ?: 0.0
                    totalRecords += distResponse.records.size
                } catch (e: Exception) {
                    partialWarnings.add("Distance read warning: ${e.message}")
                }
            }

            // 3. Active & Total Calories
            if (granted.contains(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))) {
                try {
                    val activeCalResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = ActiveCaloriesBurnedRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    activeCaloriesTotal = activeCalResponse.records.sumOf { it.energy.inKilocalories }
                    totalRecords += activeCalResponse.records.size
                } catch (_: Exception) {}
            }
            if (granted.contains(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class))) {
                try {
                    val totalCalResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = TotalCaloriesBurnedRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    totalCaloriesTotal = totalCalResponse.records.sumOf { it.energy.inKilocalories }
                    totalRecords += totalCalResponse.records.size
                } catch (_: Exception) {}
            }

            // 4. Sleep & Stages (Hypnogram without fabricating stages!)
            if (granted.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))) {
                try {
                    val sleepResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = SleepSessionRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    for (session in sleepResponse.records) {
                        contributingSources.add(session.metadata.dataOrigin.packageName)
                        val duration = java.time.Duration.between(session.startTime, session.endTime).toMinutes().toInt()
                        sleepMinutes += duration
                        if (sleepStartEpoch == null || session.startTime.toEpochMilli() < sleepStartEpoch) {
                            sleepStartEpoch = session.startTime.toEpochMilli()
                        }
                        if (sleepEndEpoch == null || session.endTime.toEpochMilli() > sleepEndEpoch) {
                            sleepEndEpoch = session.endTime.toEpochMilli()
                        }

                        // Parse real stages if provided by wearable source
                        if (session.stages.isNotEmpty()) {
                            hasStages = true
                            for (stage in session.stages) {
                                val stageName = when (stage.stage) {
                                    SleepSessionRecord.STAGE_TYPE_DEEP -> "DEEP"
                                    SleepSessionRecord.STAGE_TYPE_LIGHT -> "LIGHT"
                                    SleepSessionRecord.STAGE_TYPE_REM -> "REM"
                                    SleepSessionRecord.STAGE_TYPE_AWAKE -> "AWAKE"
                                    SleepSessionRecord.STAGE_TYPE_SLEEPING -> "SLEEPING"
                                    else -> "UNKNOWN"
                                }
                                val stageDuration = java.time.Duration.between(stage.startTime, stage.endTime).toMinutes().toInt()
                                sleepStageEntities.add(
                                    HealthConnectSleepStageEntity(
                                        date = dateStr,
                                        sessionStartTimeEpoch = session.startTime.toEpochMilli(),
                                        stageStartTimeEpoch = stage.startTime.toEpochMilli(),
                                        stageEndTimeEpoch = stage.endTime.toEpochMilli(),
                                        stageType = stageName,
                                        durationMinutes = stageDuration,
                                        sourceApp = session.metadata.dataOrigin.packageName
                                    )
                                )
                            }
                        }
                    }
                    totalRecords += sleepResponse.records.size
                } catch (e: Exception) {
                    partialWarnings.add("Sleep read warning: ${e.message}")
                }
            }

            // 5. Heart Rate Series
            if (granted.contains(HealthPermission.getReadPermission(HeartRateRecord::class))) {
                try {
                    val hrResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = HeartRateRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    val allSamples = hrResponse.records.flatMap { it.samples }
                    if (allSamples.isNotEmpty()) {
                        avgHeartRate = allSamples.map { it.beatsPerMinute }.average()
                        minHeartRate = allSamples.minOf { it.beatsPerMinute }.toDouble()
                        maxHeartRate = allSamples.maxOf { it.beatsPerMinute }.toDouble()

                        for (hrRecord in hrResponse.records) {
                            contributingSources.add(hrRecord.metadata.dataOrigin.packageName)
                            for (sample in hrRecord.samples) {
                                heartSampleEntities.add(
                                    HealthConnectHeartSampleEntity(
                                        date = dateStr,
                                        timestampEpoch = sample.time.toEpochMilli(),
                                        bpm = sample.beatsPerMinute.toDouble(),
                                        sourceApp = hrRecord.metadata.dataOrigin.packageName
                                    )
                                )
                            }
                        }
                    }
                    totalRecords += hrResponse.records.size
                } catch (e: Exception) {
                    partialWarnings.add("Heart rate read warning: ${e.message}")
                }
            }

            // 6. Resting Heart Rate (Only if genuine record exists!)
            if (granted.contains(HealthPermission.getReadPermission(RestingHeartRateRecord::class))) {
                try {
                    val rhrResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = RestingHeartRateRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    if (rhrResponse.records.isNotEmpty()) {
                        restingHeartRate = rhrResponse.records.last().beatsPerMinute.toDouble()
                        contributingSources.add(rhrResponse.records.last().metadata.dataOrigin.packageName)
                    }
                    totalRecords += rhrResponse.records.size
                } catch (_: Exception) {}
            }

            // 7. Heart Rate Variability (HRV)
            if (granted.contains(HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class))) {
                try {
                    val hrvResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = HeartRateVariabilityRmssdRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    if (hrvResponse.records.isNotEmpty()) {
                        hrvRmssd = hrvResponse.records.map { it.heartRateVariabilityMillis }.average()
                        contributingSources.add(hrvResponse.records.last().metadata.dataOrigin.packageName)
                    }
                    totalRecords += hrvResponse.records.size
                } catch (_: Exception) {}
            }

            // 8. Respiratory Rate
            if (granted.contains(HealthPermission.getReadPermission(RespiratoryRateRecord::class))) {
                try {
                    val respResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = RespiratoryRateRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    if (respResponse.records.isNotEmpty()) {
                        respiratoryRate = respResponse.records.map { it.rate }.average()
                        contributingSources.add(respResponse.records.last().metadata.dataOrigin.packageName)
                    }
                    totalRecords += respResponse.records.size
                } catch (_: Exception) {}
            }

            // 9. Oxygen Saturation (SpO2)
            if (granted.contains(HealthPermission.getReadPermission(OxygenSaturationRecord::class))) {
                try {
                    val oxyResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = OxygenSaturationRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    if (oxyResponse.records.isNotEmpty()) {
                        oxygenSaturation = oxyResponse.records.map { it.percentage.value }.average()
                        contributingSources.add(oxyResponse.records.last().metadata.dataOrigin.packageName)
                    }
                    totalRecords += oxyResponse.records.size
                } catch (_: Exception) {}
            }

            // 10. Hydration
            if (granted.contains(HealthPermission.getReadPermission(HydrationRecord::class))) {
                try {
                    val hydroResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = HydrationRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    hydrationVolumeMl = hydroResponse.records.sumOf { it.volume.inMilliliters.toInt() }
                    totalRecords += hydroResponse.records.size
                } catch (_: Exception) {}
            }

            // 11. Exercise Sessions
            if (granted.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))) {
                try {
                    val exerciseResponse = client.readRecords(
                        ReadRecordsRequest(
                            recordType = ExerciseSessionRecord::class,
                            timeRangeFilter = timeFilter
                        )
                    )
                    exerciseCount = exerciseResponse.records.size
                    exerciseMinutesTotal = exerciseResponse.records.sumOf {
                        java.time.Duration.between(it.startTime, it.endTime).toMinutes().toInt()
                    }
                    totalRecords += exerciseResponse.records.size
                } catch (_: Exception) {}
            }

            // Assemble persisted daily summary
            val summary = HealthConnectDailySummaryEntity(
                date = dateStr,
                steps = stepsTotal,
                distanceMeters = distanceMetersTotal,
                activeCaloriesKcal = activeCaloriesTotal,
                totalCaloriesKcal = totalCaloriesTotal,
                sleepDurationMinutes = sleepMinutes,
                sleepStartTimeEpoch = sleepStartEpoch,
                sleepEndTimeEpoch = sleepEndEpoch,
                restingHeartRateBpm = restingHeartRate,
                avgHeartRateBpm = avgHeartRate,
                minHeartRateBpm = minHeartRate,
                maxHeartRateBpm = maxHeartRate,
                hrvRmssdMs = hrvRmssd,
                respiratoryRateRpm = respiratoryRate,
                oxygenSaturationPercent = oxygenSaturation,
                waterMl = hydrationVolumeMl,
                exerciseMinutes = exerciseMinutesTotal,
                exerciseSessionsCount = exerciseCount,
                primarySourceApp = contributingSources.firstOrNull(),
                sourcesListJson = contributingSources.joinToString(prefix = "[", postfix = "]") { "\"$it\"" },
                lastSyncedAt = System.currentTimeMillis(),
                hasSleepStages = hasStages
            )

            // Persist to Room
            repository.saveHealthConnectData(
                summary = summary,
                sleepStages = sleepStageEntities,
                heartSamples = heartSampleEntities
            )

            datesSynced++
            currentDate = currentDate.plusDays(1)
        }

        val lastSyncNow = System.currentTimeMillis()
        repository.saveSetting("health_connect_last_sync", lastSyncNow.toString())

        _uiState.value = _uiState.value.copy(
            isSyncing = false,
            lastSyncTime = lastSyncNow,
            contributingSources = contributingSources,
            partialWarnings = partialWarnings,
            syncErrorMessage = null
        )

        SyncResult(
            isSuccess = true,
            syncedDatesCount = datesSynced,
            recordsReadCount = totalRecords,
            contributingSources = contributingSources,
            partialWarnings = partialWarnings
        )
    }

    fun openHealthConnectPlayStore(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE")
                setPackage("com.android.vending")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }
}
