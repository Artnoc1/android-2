package com.toggl.timer.log.domain

import com.toggl.timer.common.FreeCoroutineSpec
import com.toggl.timer.common.createTimeEntry
import com.toggl.timer.common.toMutableValue
import com.toggl.common.feature.timeentry.exceptions.TimeEntryDoesNotExistException
import io.kotest.properties.assertAll
import io.kotest.matchers.shouldBe
import io.kotest.shouldThrow
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class TimeEntryTappedActionTests : FreeCoroutineSpec() {
    init {
        val reducer = TimeEntriesLogReducer()
        val testTe = createTimeEntry(1, "test")

        "The TimeEntryTapped action" - {
            "should thrown when there are no time entries" - {
                "with the matching id" {
                    val initialState = createInitialState(listOf(testTe))
                    var state = initialState
                    val mutableValue = state.toMutableValue { state = it }
                    shouldThrow<TimeEntryDoesNotExistException> {
                        reducer.reduce(mutableValue, TimeEntriesLogAction.TimeEntryTapped(2))
                    }
                }

                "at all" {
                    val initialState = createInitialState()
                    assertAll(fn = { id: Long ->
                        var state = initialState
                        val mutableValue = state.toMutableValue { state = it }
                        shouldThrow<TimeEntryDoesNotExistException> {
                            reducer.reduce(mutableValue, TimeEntriesLogAction.TimeEntryTapped(id))
                        }
                    })
                }
            }

            "set the editing time entry property when the time entry exists" {
                val initialState = createInitialState(listOf(testTe))

                var state = initialState
                val mutableValue = state.toMutableValue { state = it }
                reducer.reduce(mutableValue, TimeEntriesLogAction.TimeEntryTapped(1))
                state.editableTimeEntry!!.ids.single() shouldBe testTe.id
            }
        }
    }
}
