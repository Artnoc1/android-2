package com.toggl.domain.reducers

import com.toggl.architecture.core.Reducer
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import com.toggl.domain.extensions.CoroutineTest
import com.toggl.domain.extensions.testReduceState
import com.toggl.settings.domain.SettingsAction
import io.kotest.matchers.boolean.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The SignOutReducer")
class SignOutReducerTests : CoroutineTest() {
    private val initialState = AppState(
        calendarPermissionWasGranted = true,
        timeEntries = mapOf(1L to mockk()),
        workspaces = mapOf(1L to mockk()),
        projects = mapOf(1L to mockk()),
        tasks = mapOf(1L to mockk()),
        clients = mapOf(1L to mockk()),
        tags = mapOf(1L to mockk())
    )
    private val reducer: Reducer<AppState, AppAction> = SignOutReducer(mockk(relaxed = true))

    @Test
    fun `should handle sign out action from settings`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            AppAction.Settings(SettingsAction.SignOutCompleted)
        ) { state ->
            state.calendarPermissionWasGranted.shouldBeTrue()
            state.timeEntries shouldBe mapOf()
            state.workspaces shouldBe mapOf()
            state.projects shouldBe mapOf()
            state.tasks shouldBe mapOf()
            state.clients shouldBe mapOf()
            state.tags shouldBe mapOf()
        }
    }
}
