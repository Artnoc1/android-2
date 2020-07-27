package com.toggl.timer.log.domain

import com.toggl.timer.common.FreeCoroutineSpec
import com.toggl.timer.common.createTimeEntry
import com.toggl.timer.common.toMutableValue
import com.toggl.common.feature.timeentry.exceptions.TimeEntryDoesNotExistException
import io.kotest.properties.assertAll
import io.kotest.matchers.shouldBe
import io.kotest.shouldThrow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Duration

@ExperimentalCoroutinesApi
class TimeEntryGroupTappedActionTests : FreeCoroutineSpec() {
    init {
        val reducer = TimeEntriesLogReducer()
        val testTimeEntries = listOf(createTimeEntry(1, "test"), createTimeEntry(2, "test"))

        "The TimeEntryGroupTapped action" - {
            "should thrown when there are no time entries" - {
                "with the matching id" {
                    val initialState = createInitialState(testTimeEntries)
                    var state = initialState
                    val mutableValue = state.toMutableValue { state = it }
                    shouldThrow<TimeEntryDoesNotExistException> {
                        reducer.reduce(
                            mutableValue,
                            TimeEntriesLogAction.TimeEntryGroupTapped(listOf(3))
                        )
                    }
                }

                "at all" {
                    val initialState = createInitialState()
                    assertAll(fn = { id: Long ->
                        var state = initialState
                        val mutableValue = state.toMutableValue { state = it }
                        shouldThrow<TimeEntryDoesNotExistException> {
                            reducer.reduce(
                                mutableValue,
                                TimeEntriesLogAction.TimeEntryGroupTapped(listOf(id))
                            )
                        }
                    })
                }
            }

            "set the editing time entry property when the time entry exists" {
                val initialState = createInitialState(testTimeEntries)

                var state = initialState
                val mutableValue = state.toMutableValue { state = it }
                val idsToEdit = listOf(1L, 2L)
                reducer.reduce(mutableValue, TimeEntriesLogAction.TimeEntryGroupTapped(idsToEdit))
                state.editableTimeEntry!!.ids shouldBe idsToEdit
                state.editableTimeEntry!!.duration shouldBe Duration.ofMinutes(4)
            }
        }
    }
}
