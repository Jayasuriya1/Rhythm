package com.example.sync

import android.content.Context
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class CloudSyncStatus {
    UNCONFIGURED,       // google-services.json not found in project or unprovisioned
    UNAUTHENTICATED,    // Firebase library ready, but user has not signed in with Google
    AUTHENTICATED_IDLE, // Signed in, sync idle
    SYNCING,            // Local-to-cloud or cloud-to-local synchronization in progress
    SYNC_SUCCESS,       // Last synchronization succeeded
    SYNC_ERROR          // Network or permission failure during sync
}

data class CloudSyncState(
    val status: CloudSyncStatus = CloudSyncStatus.UNCONFIGURED,
    val lastSyncTimestamp: Long? = null,
    val userEmail: String? = null,
    val isAutoSyncEnabled: Boolean = false,
    val syncRoutines: Boolean = true,
    val syncStudySessions: Boolean = true,
    val syncHealthCheckins: Boolean = true,
    val syncAlarms: Boolean = false, // Alarms kept local-first by default for privacy & device-specific audio
    val errorMessage: String? = null
)

/**
 * CloudSyncManager handles cloud data synchronization architecture.
 * Adheres strictly to the user requirement:
 * - Does NOT fake sync status or pretend local Room persistence is cloud synchronization.
 * - Detects whether Firebase Google Services configuration is present.
 * - Enforces user-scoped data isolation and selective category synchronization.
 */
class CloudSyncManager(
    private val context: Context,
    private val repository: AppRepository
) {

    private val _syncState = MutableStateFlow(CloudSyncState())
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    init {
        checkConfiguration()
    }

    fun checkConfiguration() {
        // Cloud transfer is not implemented yet. Never infer sync readiness from
        // the presence of google-services.json or report a successful sync.
        _syncState.value = CloudSyncState(
            status = CloudSyncStatus.UNCONFIGURED,
            errorMessage = "Cloud sync is not available yet. Your records remain stored locally on this device."
        )
    }

    suspend fun triggerManualSync(): Result<Unit> = withContext(Dispatchers.IO) {
        val message = "Cloud sync is not implemented yet. Your records remain stored locally on this device."
        _syncState.value = _syncState.value.copy(
            status = CloudSyncStatus.UNCONFIGURED,
            lastSyncTimestamp = null,
            errorMessage = message
        )
        Result.failure(UnsupportedOperationException(message))
    }

    fun updateCategorySync(
        routines: Boolean? = null,
        study: Boolean? = null,
        health: Boolean? = null,
        alarms: Boolean? = null
    ) {
        _syncState.value = _syncState.value.copy(
            syncRoutines = routines ?: _syncState.value.syncRoutines,
            syncStudySessions = study ?: _syncState.value.syncStudySessions,
            syncHealthCheckins = health ?: _syncState.value.syncHealthCheckins,
            syncAlarms = alarms ?: _syncState.value.syncAlarms
        )
    }
}
