package com.toggl.calendar.calendarday.domain

import com.toggl.calendar.common.CoroutineTest
import com.toggl.calendar.common.createCalendarDayReducer
import com.toggl.calendar.common.createTimeEntry
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.calendar.common.testReduceNoEffects
import com.toggl.calendar.common.testReduceState
import com.toggl.common.Constants
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.push
import com.toggl.common.feature.navigation.setRouteParam
import com.toggl.models.domain.EditableTimeEntry
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@ExperimentalCoroutinesApi
@DisplayName("The EmptyPositionLongPressed action")
class EmptyPositionLongPressedActionTests : CoroutineTest() {
    private val initialState = createInitialState()
    private val reducer = createCalendarDayReducer(dispatcherProvider = dispatcherProvider)

    @Test
    fun `should set selectedItem with the start time`() = runBlockingTest {
        val expectedStartTime: OffsetDateTime = mockk()
        reducer.testReduceState(
            initialState,
            CalendarDayAction.EmptyPositionLongPressed(expectedStartTime)
        ) { state ->
            state shouldBe state.copy(
                backStack = initialState.backStack.push(
                    Route.ContextualMenu(
                        SelectedCalendarItem.SelectedTimeEntry(
                            EditableTimeEntry.empty(1).copy(
                                startTime = expectedStartTime,
                                duration = Constants.TimeEntry.defaultTimeEntryDuration
                            )
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `should set selectedItem with the start time even if the item is already set`() = runBlockingTest {
        val expectedStartTime: OffsetDateTime = mockk()
        val timeEntry = EditableTimeEntry.fromSingle(createTimeEntry(1))
        val initialRoute = Route.ContextualMenu(SelectedCalendarItem.SelectedTimeEntry(timeEntry))
        val initialState = initialState.copy(backStack = listOf(initialRoute))
        reducer.testReduceState(
            initialState,
            CalendarDayAction.EmptyPositionLongPressed(expectedStartTime)
        ) { state ->
            state shouldBe state.copy(
                backStack = initialState.backStack.setRouteParam {
                    Route.ContextualMenu(
                        SelectedCalendarItem.SelectedTimeEntry(
                            EditableTimeEntry.empty(1).copy(
                                startTime = expectedStartTime,
                                duration = Constants.TimeEntry.defaultTimeEntryDuration
                            )
                        )
                    )
                }
            )
        }
    }

    @Test
    fun `shouldn't return any effect`() = runBlockingTest {
        reducer.testReduceNoEffects(initialState, CalendarDayAction.EmptyPositionLongPressed(mockk()))
    }
}