package com.example.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ChangesResponse
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthConnectPaginationAndSyncTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository
    private lateinit var manager: HealthConnectManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db)
        manager = HealthConnectManager(context, repository)
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun createFakeStepsRecord(count: Long): StepsRecord {
        return StepsRecord(
            startTime = Instant.ofEpochMilli(1700000000000L),
            startZoneOffset = null,
            endTime = Instant.ofEpochMilli(1700003600000L),
            endZoneOffset = null,
            count = count,
            metadata = Metadata.manualEntry()
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun createMockClient(
        readRecordsHandler: ((ReadRecordsRequest<out Record>) -> ReadRecordsResponse<out Record>)? = null,
        getChangesTokenHandler: ((ChangesTokenRequest) -> String)? = null,
        getChangesHandler: ((String) -> ChangesResponse)? = null
    ): HealthConnectClient {
        val handler = InvocationHandler { _, method: Method, args: Array<out Any?>? ->
            when (method.name) {
                "readRecords" -> {
                    val request = args?.get(0) as ReadRecordsRequest<out Record>
                    readRecordsHandler?.invoke(request)
                        ?: ReadRecordsResponse(emptyList<Record>(), null)
                }
                "getChangesToken" -> {
                    val request = args?.get(0) as ChangesTokenRequest
                    getChangesTokenHandler?.invoke(request) ?: "default_test_token"
                }
                "getChanges" -> {
                    val token = args?.get(0) as String
                    getChangesHandler?.invoke(token)
                        ?: ChangesResponse(
                            changes = emptyList(),
                            nextChangesToken = "next_token",
                            hasMore = false,
                            changesTokenExpired = false
                        )
                }
                else -> null
            }
        }

        return Proxy.newProxyInstance(
            HealthConnectClient::class.java.classLoader,
            arrayOf(HealthConnectClient::class.java),
            handler
        ) as HealthConnectClient
    }

    @Test
    fun readAllRecords_singlePage_returnsAllRecords() = runBlocking {
        val record1 = createFakeStepsRecord(500)
        val record2 = createFakeStepsRecord(1200)

        val client = createMockClient(
            readRecordsHandler = { request ->
                assertEquals(null, request.pageToken)
                ReadRecordsResponse(listOf(record1, record2), null)
            }
        )

        val results = manager.readAllRecords(
            client = client,
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH)
        )

        assertEquals(2, results.size)
        assertEquals(500L, results[0].count)
        assertEquals(1200L, results[1].count)
    }

    @Test
    fun readAllRecords_multiPage_aggregatesAcrossAllPages() = runBlocking {
        val page1Records = listOf(
            createFakeStepsRecord(100),
            createFakeStepsRecord(200)
        )
        val page2Records = listOf(
            createFakeStepsRecord(300),
            createFakeStepsRecord(400)
        )
        val page3Records = listOf(
            createFakeStepsRecord(500)
        )

        val requestedPages = mutableListOf<String?>()

        val client = createMockClient(
            readRecordsHandler = { request ->
                requestedPages.add(request.pageToken)
                when (request.pageToken) {
                    null -> ReadRecordsResponse(page1Records, "token_page_2")
                    "token_page_2" -> ReadRecordsResponse(page2Records, "token_page_3")
                    "token_page_3" -> ReadRecordsResponse(page3Records, null)
                    else -> ReadRecordsResponse(emptyList<StepsRecord>(), null)
                }
            }
        )

        val results = manager.readAllRecords(
            client = client,
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH)
        )

        // Verifies all 3 pages were fetched sequentially
        assertEquals(listOf(null, "token_page_2", "token_page_3"), requestedPages)
        // Verifies all 5 records across pages were aggregated
        assertEquals(5, results.size)
        assertEquals(100L, results[0].count)
        assertEquals(200L, results[1].count)
        assertEquals(300L, results[2].count)
        assertEquals(400L, results[3].count)
        assertEquals(500L, results[4].count)
    }

    @Test
    fun readAllRecords_emptyResponse_returnsEmptyList() = runBlocking {
        val client = createMockClient(
            readRecordsHandler = {
                ReadRecordsResponse(emptyList<StepsRecord>(), null)
            }
        )

        val results = manager.readAllRecords(
            client = client,
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH)
        )

        assertTrue(results.isEmpty())
    }

    @Test
    fun initChangesToken_storesTokenInRepository() = runBlocking {
        val expectedToken = "test_changes_token_alpha"
        val client = createMockClient(
            getChangesTokenHandler = { request ->
                assertTrue(request.recordTypes.contains(StepsRecord::class))
                expectedToken
            }
        )

        val token = manager.initChangesToken(client)
        assertEquals(expectedToken, token)

        val storedToken = repository.getSetting(HealthConnectManager.SETTING_KEY_CHANGES_TOKEN).first()?.value
        assertEquals(expectedToken, storedToken)
    }

    @Test
    fun getOrInitChangesToken_reusesExistingToken() = runBlocking {
        val existingToken = "existing_persisted_token"
        repository.saveSetting(HealthConnectManager.SETTING_KEY_CHANGES_TOKEN, existingToken)

        var initCalled = false
        val client = createMockClient(
            getChangesTokenHandler = {
                initCalled = true
                "new_token"
            }
        )

        val token = manager.getOrInitChangesToken(client)
        assertEquals(existingToken, token)
        assertEquals(false, initCalled)
    }

    @Test
    fun getOrInitChangesToken_generatesTokenWhenMissing() = runBlocking {
        val client = createMockClient(
            getChangesTokenHandler = {
                "generated_new_token"
            }
        )

        val token = manager.getOrInitChangesToken(client)
        assertEquals("generated_new_token", token)

        val stored = repository.getSetting(HealthConnectManager.SETTING_KEY_CHANGES_TOKEN).first()?.value
        assertEquals("generated_new_token", stored)
    }

    @Test
    fun syncIncremental_withValidToken_advancesTokenAndSavesState() = runBlocking {
        val initialToken = "initial_token_123"
        repository.saveSetting(HealthConnectManager.SETTING_KEY_CHANGES_TOKEN, initialToken)

        val nextToken = "updated_token_456"
        val client = createMockClient(
            getChangesHandler = { token ->
                assertEquals(initialToken, token)
                ChangesResponse(
                    changes = emptyList(),
                    nextChangesToken = nextToken,
                    hasMore = false,
                    changesTokenExpired = false
                )
            }
        )

        val result = manager.syncIncremental(
            startDateFallback = LocalDate.now(),
            endDateFallback = LocalDate.now(),
            client = client
        )

        assertTrue(result.isSuccess)
        val storedToken = repository.getSetting(HealthConnectManager.SETTING_KEY_CHANGES_TOKEN).first()?.value
        assertEquals(nextToken, storedToken)
    }

    @Test
    fun syncIncremental_whenTokenExpired_refreshesToken() = runBlocking {
        val expiredToken = "expired_token_000"
        repository.saveSetting(HealthConnectManager.SETTING_KEY_CHANGES_TOKEN, expiredToken)

        val freshToken = "brand_new_token_789"
        var tokenGenerated = false

        val client = createMockClient(
            getChangesHandler = { token ->
                ChangesResponse(
                    changes = emptyList(),
                    nextChangesToken = "ignored",
                    hasMore = false,
                    changesTokenExpired = true
                )
            },
            getChangesTokenHandler = {
                tokenGenerated = true
                freshToken
            }
        )

        manager.syncIncremental(
            startDateFallback = LocalDate.now(),
            endDateFallback = LocalDate.now(),
            client = client
        )

        assertTrue("New changes token should be requested when expired", tokenGenerated)
        val storedToken = repository.getSetting(HealthConnectManager.SETTING_KEY_CHANGES_TOKEN).first()?.value
        assertEquals(freshToken, storedToken)
    }
}

