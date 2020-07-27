package com.toggl.settings.domain

import com.toggl.architecture.Loadable
import com.toggl.settings.common.CoroutineTest
import com.toggl.settings.common.createSettingsReducer
import com.toggl.settings.common.createSettingsState
import com.toggl.settings.common.testReduceNoEffects
import com.toggl.settings.common.testReduceState
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The SendFeedbackResultSeen action")
class SendFeedbackResultSeenActionTests : CoroutineTest() {
    private val initialState = createSettingsState()
    private val reducer = createSettingsReducer(dispatcherProvider = dispatcherProvider)

    @Test
    fun `Should modify the state to indicate that there is no feedback being sent (or that it has been sent)`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            SettingsAction.SendFeedbackResultSeen
        ) { state -> state.localState.sendFeedbackRequest.shouldBeTypeOf<Loadable.Uninitialized>() }
    }

    @Test
    fun `Should not produce any effects`() = runBlockingTest {
        reducer.testReduceNoEffects(initialState, SettingsAction.FeedbackSent)
    }
}