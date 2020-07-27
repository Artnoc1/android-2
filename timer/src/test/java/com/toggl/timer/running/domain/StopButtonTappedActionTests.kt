package com.toggl.timer.running.domain

import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.Workspace
import com.toggl.timer.common.FreeCoroutineSpec
import com.toggl.timer.common.shouldEmitTimeEntryAction
import com.toggl.timer.common.testReduce
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class StopButtonTappedActionTests : FreeCoroutineSpec() {
    init {
        val workspace = mockk<Workspace>()
        val timeService = mockk<TimeService>()
        val reducer = RunningTimeEntryReducer(timeService)
        val initialState = createInitialState()

        coEvery { workspace.id } returns 1

        "The StopButtonTapped action" - {
            reducer.testReduce(
                initialState = initialState,
                action = RunningTimeEntryAction.StopButtonTapped
            ) { state, effect ->
                "shouldn't change the state" {
                    state shouldBe initialState
                }
                "should emit StopTimeEntry effect" {
                    effect.shouldBeSingleton()
                    effect.single()
                        .shouldEmitTimeEntryAction<RunningTimeEntryAction.TimeEntryHandling, TimeEntryAction.StopRunningTimeEntry>()
                }
            }
        }
    }
}