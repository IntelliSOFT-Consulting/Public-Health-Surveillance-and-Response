package com.icl.nphi.monitor

import org.hl7.fhir.r4.model.Resource
import java.time.Instant

data class PageInfo(
    val pageSize: Int = 50,
    val currentPage: Int = 0,
    val hasMore: Boolean = true
)

// ResourceWithSyncStatus.kt
data class ResourceWithSyncStatus(
    val resource: Resource,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncAttempt: Instant? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0
)

enum class SyncStatus {
    SYNCED,
    SYNCING,
    PENDING,
    FAILED,
    RETRYING
}

sealed class SyncResult {
    data class Success(val resourceId: String) : SyncResult()
    data class Failure(val error: String) : SyncResult()
}

data class SyncStats(
    val total: Int = 0,
    val synced: Int = 0,
    val failed: Int = 0,
    val pending: Int = 0,
    val retrying: Int = 0
)

data class SyncFailure(
    val resourceId: String,
    val resourceType: String,
    val errorMessage: String,
    val timestamp: Instant = Instant.now(),
    val retryCount: Int = 0
)

data class BulkSyncResult(
    val total: Int,
    val successful: Int,
    val failed: Int,
    val individualResults: List<SyncResult>
)