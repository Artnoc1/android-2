package com.toggl.settings.domain

import com.toggl.architecture.Loadable
import com.toggl.settings.common.CoroutineTest
import com.toggl.settings.common.createSettingsReducer
import com.toggl.settings.common.createSettingsState
import com.toggl.settings.common.testReduceState
import io.kotlintest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The FeedbackSent action")
class FeedbackSentActionTests : CoroutineTest() {
    private val initialState = createSettingsState()
    private val reducer = createSettingsReducer(dispatcherProvider = dispatcherProvider)

    @Test
    fun `Should modify the state to indicate that the feedback was sent`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            SettingsAction.FeedbackSent
        ) { state -> state.localState.sendFeedbackRequest.shouldBeTypeOf<Loadable.Loaded<Unit>>() }
    }
}