package com.toggl.settings.domain

import com.toggl.architecture.Loadable
import com.toggl.settings.common.CoroutineTest
import com.toggl.settings.common.createSettingsReducer
import com.toggl.settings.common.createSettingsState
import com.toggl.settings.common.testReduceState
import io.kotlintest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.lang.Exception

@ExperimentalCoroutinesApi
@DisplayName("The SetSendFeedbackError action")
class SetSendFeedbackErrorActionTests : CoroutineTest() {
    private val initialState = createSettingsState()
    private val reducer = createSettingsReducer(dispatcherProvider = dispatcherProvider)

    @Test
    fun `Should modify the state to indicate that there was an error while sending the feedback`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            SettingsAction.SetSendFeedbackError(mockk())
        ) { state -> state.localState.sendFeedbackRequest.shouldBeTypeOf<Loadable.Error<Exception>>() }
    }
}