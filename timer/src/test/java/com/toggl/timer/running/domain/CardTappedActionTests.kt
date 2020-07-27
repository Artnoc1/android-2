package com.toggl.timer.running.domain

import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.Workspace
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.assertNoEffectsWereReturned
import com.toggl.timer.common.createTimeEntry
import com.toggl.timer.common.testReduce
import io.kotest.matchers.types.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime

@ExperimentalCoroutinesApi
@DisplayName("The CardTapped action")
class CardTappedActionTests : CoroutineTest() {
    private val workspace = mockk<Workspace>()
    private val timeService = mockk<TimeService>()
    private val reducer = RunningTimeEntryReducer(timeService)

    @Test
    fun `should init editableTimeEntry with an empty id list when no TE is running`() = runBlockingTest {
        val initialState = createInitialState()
        coEvery { workspace.id } returns 1

        reducer.testReduce(
            initialState = initialState,
            action = RunningTimeEntryAction.CardTapped
        ) { state, _ ->
            state.editableTimeEntry.shouldNotBeNull()
            state.editableTimeEntry!!.startTime shouldBe null
            state.editableTimeEntry!!.ids shouldBe emptyList()
        }
    }

    @Test
    fun `should init editableTimeEntry with the currently running time entry when there is one`() = runBlockingTest {
        val initialState = createInitialState(
            timeEntries = mapOf(
                1L to createTimeEntry(1, description = "Test", duration = Duration.ofHours(1)),
                2L to createTimeEntry(2, description = "Running", duration = null)
            )
        )
        coEvery { workspace.id } returns 1

        reducer.testReduce(
            initialState = initialState,
            action = RunningTimeEntryAction.CardTapped
        ) { state, _ ->
            state.editableTimeEntry.shouldNotBeNull()
            state.editableTimeEntry!!.description shouldBe "Running"
        }
    }

    @Test
    fun `shouldn't emit any effect effect`() = runBlockingTest {
        val initialState = createInitialState()
        coEvery { workspace.id } returns 1
        every { timeService.now() } returns OffsetDateTime.MAX

        reducer.testReduce(
            initialState = initialState,
            action = RunningTimeEntryAction.CardTapped,
            testCase = ::assertNoEffectsWereReturned
        )
    }
}