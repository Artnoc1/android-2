package com.toggl.timer.log.domain

import com.toggl.architecture.extensions.noEffect
import com.toggl.repository.interfaces.TimeEntryRepository
import com.toggl.timer.common.FreeCoroutineSpec
import com.toggl.timer.common.createTimeEntry
import com.toggl.timer.common.domain.DeleteTimeEntryEffect
import com.toggl.timer.common.toMutableValue
import io.kotlintest.matchers.types.shouldBeTypeOf
import io.kotlintest.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class CommitDeletionActionTests : FreeCoroutineSpec() {
    init {
        val repository = mockk<TimeEntryRepository>()
        val reducer = TimeEntriesLogReducer(repository, dispatcherProvider)

        "The CommitDeletion action" - {
            "should do nothing if" - {
                "there are no ids pending deletion" {
                    val initialState = createInitialState(entriesPendingDeletion = setOf())
                    var state = initialState
                    val mutableValue = state.toMutableValue { state = it }

                    val effect = reducer.reduce(
                        mutableValue,
                        TimeEntriesLogAction.CommitDeletion(listOf())
                    )

                    state shouldBe initialState
                    effect shouldBe noEffect()
                }

                "the ids pending deletion in action don't match those in state" {
                    val initialState = createInitialState(entriesPendingDeletion = setOf(1, 2, 3))
                    var state = initialState
                    val mutableValue = state.toMutableValue { state = it }

                    val effect = reducer.reduce(
                        mutableValue,
                        TimeEntriesLogAction.CommitDeletion(listOf(4, 5, 1337))
                    )

                    state shouldBe initialState
                    effect shouldBe noEffect()
                }

                "the ids pending deletion in action are a subset of those in state" {
                    val initialState = createInitialState(entriesPendingDeletion = setOf(1, 2, 3))
                    var state = initialState
                    val mutableValue = state.toMutableValue { state = it }

                    val effect = reducer.reduce(
                        mutableValue,
                        TimeEntriesLogAction.CommitDeletion(listOf(1, 3))
                    )

                    state shouldBe initialState
                    effect shouldBe noEffect()
                }

                "the ids pending deletion in action are a superset of those in state" {
                    val initialState = createInitialState(entriesPendingDeletion = setOf(1, 2, 3))
                    var state = initialState
                    val mutableValue = state.toMutableValue { state = it }

                    val effect = reducer.reduce(
                        mutableValue,
                        TimeEntriesLogAction.CommitDeletion(listOf(1, 2, 3, 1337))
                    )

                    state shouldBe initialState
                    effect shouldBe noEffect()
                }
            }

            "should delete nothing but clear the pending list if the time entries are not in state" - {
                val initialState = createInitialState(entriesPendingDeletion = setOf(1, 2, 3))
                var state = initialState
                val mutableValue = state.toMutableValue { state = it }

                val effect = reducer.reduce(
                    mutableValue,
                    TimeEntriesLogAction.CommitDeletion(listOf(1, 2, 3))
                )

                state shouldBe initialState.copy(entriesPendingDeletion = setOf())
                effect shouldBe listOf()
            }

            "should delete time entries" - {
                val te1 = createTimeEntry(1)
                val te2 = createTimeEntry(2)
                val te3 = createTimeEntry(3)
                val initialState = createInitialState(
                    timeEntries = listOf(te1, te2, te3),
                    entriesPendingDeletion = setOf(1, 2, 3)
                )
                var state = initialState
                val mutableValue = state.toMutableValue { state = it }
                coEvery { repository.deleteTimeEntry(any()) } returns mockk()
                val effect = reducer.reduce(
                    mutableValue,
                    TimeEntriesLogAction.CommitDeletion(listOf(1, 2, 3))
                )

                "clear the pending list and" - {
                    state.entriesPendingDeletion shouldBe setOf()
                }

                "optimistically update the time entries list that were deleted" - {
                    state.timeEntries shouldBe listOf(
                        te1.copy(isDeleted = true),
                        te2.copy(isDeleted = true),
                        te3.copy(isDeleted = true)
                    ).associateBy { it.id }
                }

                effect.forEach {
                    it.shouldBeTypeOf<DeleteTimeEntryEffect<TimeEntriesLogAction.TimeEntryDeleted>>()
                    it.execute()
                }

                // the following doesn't test the effect, it tests the right arguments were passed to the constructor
                coVerify {
                    repository.deleteTimeEntry(te1)
                    repository.deleteTimeEntry(te2)
                    repository.deleteTimeEntry(te3)
                }
            }
        }
    }
}