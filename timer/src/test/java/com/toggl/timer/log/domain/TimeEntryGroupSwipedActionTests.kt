package com.toggl.timer.log.domain

import com.toggl.models.common.SwipeDirection
import com.toggl.repository.interfaces.StartTimeEntryResult
import com.toggl.repository.interfaces.TimeEntryRepository
import com.toggl.timer.common.FreeCoroutineSpec
import com.toggl.timer.common.createTimeEntry
import com.toggl.timer.common.toSettableValue
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.types.shouldBeTypeOf
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest

@ExperimentalCoroutinesApi
class TimeEntryGroupSwipedActionTests : FreeCoroutineSpec() {
    init {
        val repository = mockk<TimeEntryRepository>()
        val entryInDatabase = createTimeEntry(1, "test")
        val entryToBeStarted = createTimeEntry(2, "test")
        coEvery { repository.startTimeEntry(1, "test") } returns StartTimeEntryResult(
            entryToBeStarted,
            null
        )
        val reducer = TimeEntriesLogReducer(repository, dispatcherProvider)

        "The TimeEntryGroupSwiped action" - {

            "when swiping right" - {
                "should continue the swiped time entries" {
                    val initialState = createInitialState(listOf(entryInDatabase))
                    var state = initialState
                    val settableValue = state.toSettableValue { state = it }
                    val action = TimeEntriesLogAction.TimeEntryGroupSwiped(listOf(1, 2), SwipeDirection.Right)
                    val effects = reducer.reduce(settableValue, action)
                    val startedTimeEntry =
                        (effects.single().execute() as TimeEntriesLogAction.TimeEntryStarted).startedTimeEntry
                    startedTimeEntry shouldBe entryToBeStarted
                }

                "should throw when there are no matching TEs" {
                    val initialState = createInitialState(listOf(entryInDatabase))
                    var state = initialState
                    val settableValue = state.toSettableValue { state = it }
                    val action = TimeEntriesLogAction.TimeEntryGroupSwiped(listOf(2, 3), SwipeDirection.Right)

                    shouldThrow<IllegalStateException> {
                        reducer.reduce(settableValue, action)
                    }
                }
            }

            "when swiping left" - {
                "should delete TEs pending deletion (ignoring ids not in state) and put the swiped TEs to pending deletion in state" {
                    val timeEntries = (1L..10L).map { createTimeEntry(it, "testing") }
                    val timeEntriesToSwipe = timeEntries.take(4)
                    for (te in timeEntries.drop(4)) {
                        coEvery { repository.deleteTimeEntry(te) } returns te.copy(isDeleted = true)
                    }

                    val initialState = createInitialState(timeEntries, entriesPendingDeletion = setOf(8, 9, 11))
                    var state = initialState
                    val settableValue = state.toSettableValue { state = it }
                    val action =
                        TimeEntriesLogAction.TimeEntryGroupSwiped(timeEntriesToSwipe.map { it.id }, SwipeDirection.Left)

                    val effectActions = reducer.reduce(settableValue, action)
                    val deletedTimeEntryIds = effectActions.dropLast(1)
                        .map { it.execute() as TimeEntriesLogAction.TimeEntryDeleted }
                        .map { it.deletedTimeEntry.id }
                    val waitForUndoEffect = effectActions.last()

                    deletedTimeEntryIds shouldContainExactlyInAnyOrder listOf(8L, 9L)
                    state.entriesPendingDeletion shouldBe setOf(1L, 2L, 3L, 4L)
                    waitForUndoEffect.shouldBeTypeOf<WaitForUndoEffect>()
                    runBlockingTest {
                        val executedUndo = waitForUndoEffect.execute()
                        executedUndo shouldBe TimeEntriesLogAction.CommitDeletion(listOf(1L, 2L, 3L, 4L))
                    }
                }

                "should just put the swiped TEs to pending deletion in state in case there's nothing pending deletion" {
                    val timeEntries = (1L..10L).map { createTimeEntry(it, "testing") }
                    val timeEntriesToSwipe = timeEntries.take(4)

                    val initialState = createInitialState(timeEntries)
                    var state = initialState
                    val settableValue = state.toSettableValue { state = it }
                    val action =
                        TimeEntriesLogAction.TimeEntryGroupSwiped(timeEntriesToSwipe.map { it.id }, SwipeDirection.Left)

                    val effectActions = reducer.reduce(settableValue, action)
                    val waitForUndoEffect = effectActions[0]

                    effectActions.size shouldBe 1
                    state.entriesPendingDeletion shouldBe setOf(1L, 2L, 3L, 4L)
                    waitForUndoEffect.shouldBeTypeOf<WaitForUndoEffect>()
                    runBlockingTest {
                        val executedUndo = waitForUndoEffect.execute()
                        executedUndo shouldBe TimeEntriesLogAction.CommitDeletion(listOf(1L, 2L, 3L, 4L))
                    }
                }
            }
        }
    }
}