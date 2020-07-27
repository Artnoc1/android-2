package com.toggl.domain.reducers

import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.backStackOf
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import com.toggl.domain.Tab
import com.toggl.domain.extensions.CoroutineTest
import com.toggl.domain.extensions.testReduceNoEffects
import com.toggl.domain.extensions.testReduceState
import com.toggl.settings.domain.SettingsAction
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The NavigationReducer")
class NavigationReducerTests : CoroutineTest() {
    private val initialState = AppState(
        calendarPermissionWasGranted = true,
        timeEntries = mapOf(1L to mockk()),
        workspaces = mapOf(1L to mockk()),
        projects = mapOf(1L to mockk()),
        tasks = mapOf(1L to mockk()),
        clients = mapOf(1L to mockk()),
        tags = mapOf(1L to mockk())
    )
    private val reducer = NavigationReducer()

    @Nested
    @DisplayName("When receiving a back button pressed action")
    inner class BackPressedActionTests {

        @Test
        fun `pops back stack`() = runBlockingTest {
            reducer.testReduceState(initialState.copy(backStack = backStackOf(Route.Timer, Route.Reports)), AppAction.BackButtonPressed) {
                state -> state shouldBe initialState.copy(
                    backStack = backStackOf(Route.Timer)
                )
            }
        }
    }

    @Nested
    @DisplayName("When receiving a tab selected action")
    inner class TabSelectedActionTests {

        @Test
        fun `sets the back stack to timer when selecting the timer tab`() = runBlockingTest {
            reducer.testReduceState(initialState, AppAction.TabSelected(Tab.Timer)) {
                state -> state shouldBe initialState.copy(
                    backStack = backStackOf(Route.Timer)
                )
            }
        }

        @Test
        fun `sets the back stack to timer and calendar when selecting the calendar tab`() = runBlockingTest {
            reducer.testReduceState(initialState, AppAction.TabSelected(Tab.Calendar)) {
                state -> state shouldBe initialState.copy(
                    backStack = backStackOf(Route.Timer, Route.Calendar)
                )
            }
        }

        @Test
        fun `sets the back stack to timer and reports when selecting the reports tab`() = runBlockingTest {
            reducer.testReduceState(initialState, AppAction.TabSelected(Tab.Calendar)) { state ->
                state shouldBe initialState.copy(
                    backStack = backStackOf(Route.Timer, Route.Calendar)
                )
            }
        }

        @Test
        fun `returns no effects`() = runBlockingTest {
            reducer.testReduceNoEffects(initialState, AppAction.Settings(SettingsAction.SignOutCompleted))
        }
    }
}
