package com.toggl.timer.log.domain

import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.shouldEmitTimeEntryAction
import com.toggl.timer.common.testReduceEffects
import io.kotest.matchers.collections.shouldBeSingleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The ContinueButtonTappedAction")
class ContinueButtonTappedActionTests : CoroutineTest() {

    private val reducer = TimeEntriesLogReducer()

    @Test
    fun `should emit start time entry effect`() = runBlockingTest {
        reducer.testReduceEffects(
            createInitialState(),
            TimeEntriesLogAction.ContinueButtonTapped(1)
        ) { effects ->
            effects.shouldBeSingleton()
            effects.single()
                .shouldEmitTimeEntryAction<TimeEntriesLogAction.TimeEntryHandling, TimeEntryAction.ContinueTimeEntry>()
        }
    }
}