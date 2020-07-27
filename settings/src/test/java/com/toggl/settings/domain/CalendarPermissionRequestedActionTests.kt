package com.toggl.settings.domain

import com.toggl.settings.common.CoroutineTest
import com.toggl.settings.common.createSettingsReducer
import com.toggl.settings.common.createSettingsState
import com.toggl.settings.common.testReduceNoEffects
import com.toggl.settings.common.testReduceState
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The CalendarPermissionRequested action")
class CalendarPermissionRequestedActionTests : CoroutineTest() {
    private val initialState = createSettingsState()
    private val reducer = createSettingsReducer(dispatcherProvider = dispatcherProvider)

    @Test
    fun `Shouldn't return any effects`() = runBlockingTest {
        reducer.testReduceNoEffects(
            initialState,
            SettingsAction.CalendarPermissionRequested
        )
    }

    @Test
    fun `Should set the state shouldRequestCalendarPermission to true`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            SettingsAction.CalendarPermissionRequested
        ) { state ->
            state.shouldRequestCalendarPermission shouldBe true
        }
    }
}