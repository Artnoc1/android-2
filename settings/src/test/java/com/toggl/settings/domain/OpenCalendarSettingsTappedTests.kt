package com.toggl.settings.domain

import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.push
import com.toggl.settings.common.CoroutineTest
import com.toggl.settings.common.createSettingsReducer
import com.toggl.settings.common.createSettingsState
import com.toggl.settings.common.testReduceNoEffects
import com.toggl.settings.common.testReduceState
import io.kotlintest.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The OpenCalendarSettingsTapped action")
class OpenCalendarSettingsTappedTests : CoroutineTest() {
    private val initialState = createSettingsState()
    private val reducer = createSettingsReducer(dispatcherProvider = dispatcherProvider)

    @Test
    fun `returns no effects`() = runBlockingTest {
        reducer.testReduceNoEffects(initialState, SettingsAction.OpenCalendarSettingsTapped)
    }

    @Test
    fun `should navigate to the calendar settings page`() = runBlockingTest {
        reducer.testReduceState(initialState, SettingsAction.OpenCalendarSettingsTapped) { state ->
            state shouldBe initialState.copy(
                backStack = initialState.backStack.push(Route.CalendarSettings)
            )
        }
    }
}