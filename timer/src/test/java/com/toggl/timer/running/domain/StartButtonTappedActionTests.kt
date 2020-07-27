package com.toggl.timer.running.domain

import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.common.services.time.TimeService
import com.toggl.timer.common.FreeCoroutineSpec
import com.toggl.timer.common.shouldEmitTimeEntryAction
import com.toggl.timer.common.testReduce
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.OffsetDateTime

@ExperimentalCoroutinesApi
class StartButtonTappedActionTests : FreeCoroutineSpec() {
    init {
        val timeService = mockk<TimeService> { every { now() } returns OffsetDateTime.MAX }
        val reducer = RunningTimeEntryReducer(timeService)
        val initialState = createInitialState()

        "The StartButtonTapped action" - {
            reducer.testReduce(
                initialState = initialState,
                action = RunningTimeEntryAction.StartButtonTapped
            ) { state, effect ->
                "shouldn't change the state" {
                    state shouldBe initialState
                }
                "should emit StartTimeEntry effect" {
                    effect.shouldBeSingleton()
                    effect.first()
                        .shouldEmitTimeEntryAction<RunningTimeEntryAction.TimeEntryHandling, TimeEntryAction.StartTimeEntry>()
                }
            }
        }
    }
}