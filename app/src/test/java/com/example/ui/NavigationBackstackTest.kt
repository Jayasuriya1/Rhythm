package com.example.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationBackstackTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db)
        viewModel = MainViewModel(repository, context)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun initialDestination_isToday_cannotNavigateBack() {
        assertEquals(AppDestination.TODAY, viewModel.currentDestination.value)
        assertFalse(viewModel.canNavigateBack.value)
        assertFalse(viewModel.navigateBack())
    }

    @Test
    fun navigatingToTopLevelTab_updatesDestination_andCanNavigateBackToToday() {
        viewModel.navigateTo(AppDestination.ROUTINE)
        assertEquals(AppDestination.ROUTINE, viewModel.currentDestination.value)
        assertTrue(viewModel.canNavigateBack.value)

        val handled = viewModel.navigateBack()
        assertTrue(handled)
        assertEquals(AppDestination.TODAY, viewModel.currentDestination.value)
        assertFalse(viewModel.canNavigateBack.value)
    }

    @Test
    fun switchingBetweenTopLevelTabs_doesNotAccumulateLoopingBackstack() {
        viewModel.navigateTo(AppDestination.ROUTINE)
        viewModel.navigateTo(AppDestination.STUDY)
        viewModel.navigateTo(AppDestination.MEDITATION)
        assertEquals(AppDestination.MEDITATION, viewModel.currentDestination.value)
        assertTrue(viewModel.canNavigateBack.value)

        // Back returns directly to TODAY without looping through Routine and Study
        val handled = viewModel.navigateBack()
        assertTrue(handled)
        assertEquals(AppDestination.TODAY, viewModel.currentDestination.value)
        assertFalse(viewModel.canNavigateBack.value)
        assertFalse(viewModel.navigateBack())
    }

    @Test
    fun hierarchicalSubScreenNavigation_popsStepByStep() {
        viewModel.navigateTo(AppDestination.HEALTH)
        viewModel.navigateTo(AppDestination.HEALTH_CONNECT)
        viewModel.navigateTo(AppDestination.SLEEP_ANALYTICS)
        assertEquals(AppDestination.SLEEP_ANALYTICS, viewModel.currentDestination.value)
        assertTrue(viewModel.canNavigateBack.value)

        // Step 1: Sleep Analytics -> Health Connect
        assertTrue(viewModel.navigateBack())
        assertEquals(AppDestination.HEALTH_CONNECT, viewModel.currentDestination.value)
        assertTrue(viewModel.canNavigateBack.value)

        // Step 2: Health Connect -> Health
        assertTrue(viewModel.navigateBack())
        assertEquals(AppDestination.HEALTH, viewModel.currentDestination.value)
        assertTrue(viewModel.canNavigateBack.value)

        // Step 3: Health -> Today
        assertTrue(viewModel.navigateBack())
        assertEquals(AppDestination.TODAY, viewModel.currentDestination.value)
        assertFalse(viewModel.canNavigateBack.value)

        // Step 4: Today has nowhere back to go
        assertFalse(viewModel.navigateBack())
    }

    @Test
    fun secondaryQuickAccess_fromAnyScreen_restoresPreviousScreen() {
        viewModel.navigateTo(AppDestination.STUDY)
        viewModel.navigateTo(AppDestination.ALARMS)
        assertEquals(AppDestination.ALARMS, viewModel.currentDestination.value)

        // Popping Alarms returns to Study
        assertTrue(viewModel.navigateBack())
        assertEquals(AppDestination.STUDY, viewModel.currentDestination.value)

        // Popping Study returns to Today
        assertTrue(viewModel.navigateBack())
        assertEquals(AppDestination.TODAY, viewModel.currentDestination.value)
        assertFalse(viewModel.canNavigateBack.value)
    }

    @Test
    fun navigateToToday_clearsStack() {
        viewModel.navigateTo(AppDestination.HEALTH)
        viewModel.navigateTo(AppDestination.HEALTH_CONNECT)
        viewModel.navigateTo(AppDestination.TODAY)

        assertEquals(AppDestination.TODAY, viewModel.currentDestination.value)
        assertFalse(viewModel.canNavigateBack.value)
        assertFalse(viewModel.navigateBack())
    }

    @Test
    fun duplicateNavigateTo_doesNotDuplicateStack() {
        viewModel.navigateTo(AppDestination.SETTINGS)
        viewModel.navigateTo(AppDestination.SETTINGS)
        assertEquals(AppDestination.SETTINGS, viewModel.currentDestination.value)

        assertTrue(viewModel.navigateBack())
        assertEquals(AppDestination.TODAY, viewModel.currentDestination.value)
        assertFalse(viewModel.canNavigateBack.value)
    }
}
