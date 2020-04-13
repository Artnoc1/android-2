package com.toggl.timer.startedit.domain

import com.toggl.models.domain.Workspace
import com.toggl.repository.interfaces.StartTimeEntryResult
import com.toggl.repository.interfaces.TimeEntryRepository
import com.toggl.timer.common.FreeCoroutineSpec
import com.toggl.timer.common.createTimeEntry
import com.toggl.timer.common.domain.EditableTimeEntry
import com.toggl.timer.common.toSettableValue
import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class DoneButtonTappedActionTests : FreeCoroutineSpec() {
    val repository = mockk<TimeEntryRepository>()
    val startTimeEntryResult = mockk<StartTimeEntryResult>()
    val timeEntry = createTimeEntry(1, "old description")
    val timeEntry2 = createTimeEntry(2, "old description")
    val workspace = mockk<Workspace>()
    val editableTimeEntry =
        EditableTimeEntry.fromSingle(createTimeEntry(1, description = "Test"))
    val state = StartEditState(mapOf(1L to timeEntry, 2L to timeEntry2), mapOf(1L to workspace), editableTimeEntry)
    val reducer = StartEditReducer(repository, dispatcherProvider)

    init {
        coEvery { workspace.id } returns 1
        every { startTimeEntryResult.startedTimeEntry } returns mockk()
        every { startTimeEntryResult.stoppedTimeEntry } returns mockk()

        "The DoneButtonTapped action" - {
            "should throw when there's no editable entry" {
                var initialState = state.copy(editableTimeEntry = null)
                val settableValue = initialState.toSettableValue { initialState = it }

                shouldThrow<NullPointerException> {
                    reducer.reduce(settableValue, StartEditAction.DoneButtonTapped)
                }
            }

            "should start the TE if the editable has no ids" {
                var initialState = state.copy(editableTimeEntry = editableTimeEntry.copy(ids = listOf()))
                val settableValue = initialState.toSettableValue { initialState = it }

                val result = reducer.reduce(settableValue, StartEditAction.DoneButtonTapped)
                result[0].execute()

                result.size shouldBe 1
                coVerify {
                    repository.startTimeEntry(1, "Test")
                }
            }

            "should update the TE if the editable has one id" {
                var initialState = state.copy()
                val settableValue = initialState.toSettableValue { initialState = it }

                val result = reducer.reduce(settableValue, StartEditAction.DoneButtonTapped)
                result[0].execute()

                result.size shouldBe 1
                coVerify {
                    repository.editTimeEntry(timeEntry.copy(description = "Test"))
                }
            }

            "should update each TE in a group if the editable has several ids" {
                var initialState = state.copy(editableTimeEntry = editableTimeEntry.copy(ids = listOf(1L, 2L)))
                val settableValue = initialState.toSettableValue { initialState = it }

                val result = reducer.reduce(settableValue, StartEditAction.DoneButtonTapped)
                result.forEach { it.execute() }

                result.size shouldBe 2
                coVerify {
                    repository.editTimeEntry(timeEntry.copy(description = "Test"))
                    repository.editTimeEntry(timeEntry2.copy(description = "Test"))
                }
            }

            "should update no TEs if they're not in state" {
                var initialState = state.copy(editableTimeEntry = editableTimeEntry.copy(ids = listOf(3L, 4L)))
                val settableValue = initialState.toSettableValue { initialState = it }

                val result = reducer.reduce(settableValue, StartEditAction.DoneButtonTapped)
                result.forEach { it.execute() }

                result.size shouldBe 0
                coVerify(exactly = 0) {
                    repository.editTimeEntry(any())
                }
            }
        }
    }

    override fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        clearMocks(repository)
        coEvery { repository.startTimeEntry(any(), any()) } returns startTimeEntryResult
        coEvery { repository.editTimeEntry(any()) } returns timeEntry
    }
}