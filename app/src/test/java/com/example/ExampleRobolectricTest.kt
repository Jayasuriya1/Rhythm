package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches Rhythm and Focus`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Rhythm & Focus", appName)
    }

    @Test
    fun `dateTimeUtils correctly calculates deltas`() {
        // Target 07:00 AM (420m), Actual 07:15 AM (435m) -> +15m
        val deltaPositive = DateTimeUtils.calculateDelta(420, 435)
        assertEquals("+15m", deltaPositive)

        // Target 07:00 AM (420m), Actual 06:50 AM (410m) -> -10m
        val deltaNegative = DateTimeUtils.calculateDelta(420, 410)
        assertEquals("-10m", deltaNegative)

        // Exact time
        val deltaOnTime = DateTimeUtils.calculateDelta(420, 420)
        assertEquals("On time", deltaOnTime)
    }

    @Test
    fun `defaultAnchors contains 5 social rhythm anchors`() {
        val anchors = AppDatabase.DEFAULT_ANCHORS
        assertEquals(5, anchors.size)
        val anchorIds = anchors.map { it.id }
        assertTrue(anchorIds.contains("out_of_bed"))
        assertTrue(anchorIds.contains("first_contact"))
        assertTrue(anchorIds.contains("main_activity"))
        assertTrue(anchorIds.contains("dinner"))
        assertTrue(anchorIds.contains("getting_into_bed"))
    }
}
