package com.toggl.calendar.calendarday.domain

import com.toggl.calendar.common.CoroutineTest
import com.toggl.calendar.common.createCalendarDayReducer
import com.toggl.calendar.common.createTimeEntry
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.calendar.common.testReduceException
import com.toggl.calendar.common.testReduceNoEffects
import com.toggl.calendar.common.testReduceState
import com.toggl.calendar.exception.SelectedItemShouldBeATimeEntryException
import com.toggl.calendar.exception.SelectedItemShouldNotBeNullException
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.setRouteParam
import com.toggl.common.feature.timeentry.exceptions.TimeEntryShouldNotBeNewException
import com.toggl.common.feature.timeentry.exceptions.TimeEntryShouldNotBeRunningException
import com.toggl.models.domain.EditableTimeEntry
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@ExperimentalCoroutinesApi
@DisplayName("The TimeEntryDragged action")
class TimeEntryDraggedActionTests : CoroutineTest() {

    private val reducer = createCalendarDayReducer(dispatcherProvider = dispatcherProvider)

    private val startTime = OffsetDateTime.of(2005, 5, 5, 5, 5, 0, 0, ZoneOffset.UTC)

    private val validEditableTimeEntry = EditableTimeEntry.fromSingle(createTimeEntry(
        1,
        "test",
        startTime,
        Duration.ofMinutes(90)
    ))

    @Test
    fun `throws if executed on a calendar item`() = runBlockingTest {
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedCalendarEvent(mockk()))
        reducer.testReduceException(
            initialState,
            CalendarDayAction.TimeEntryDragged(OffsetDateTime.MAX),
            SelectedItemShouldBeATimeEntryException::class.java
        )
    }

    @Test
    fun `throws if executed on a null selected item`() = runBlockingTest {
        val initialState = createInitialState(selectedItem = null)
        reducer.testReduceException(
            initialState,
            CalendarDayAction.TimeEntryDragged(OffsetDateTime.MAX),
            SelectedItemShouldNotBeNullException::class.java
        )
    }

    @Test
    fun `throws if executed on a new time entry`() = runBlockingTest {
        val timeEntry = EditableTimeEntry.empty(1)
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedTimeEntry(timeEntry))
        reducer.testReduceException(
            initialState,
            CalendarDayAction.TimeEntryDragged(OffsetDateTime.MAX),
            TimeEntryShouldNotBeNewException::class.java
        )
    }

    @Test
    fun `throws if executed on a running time entry`() = runBlockingTest {
        val timeEntry = EditableTimeEntry.empty(1).copy(startTime = OffsetDateTime.MIN)
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedTimeEntry(timeEntry))
        reducer.testReduceException(
            initialState,
            CalendarDayAction.TimeEntryDragged(OffsetDateTime.MAX),
            TimeEntryShouldNotBeRunningException::class.java
        )
    }

    @Test
    fun `shouldn't return any effects`() = runBlockingTest {
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedTimeEntry(validEditableTimeEntry))
        val newStart = startTime.minusHours(1)
        reducer.testReduceNoEffects(initialState, CalendarDayAction.TimeEntryDragged(newStart))
    }

    @Test
    fun `should set the new start time without changing anything else`() = runBlockingTest {
        val selectedItem = SelectedCalendarItem.SelectedTimeEntry(validEditableTimeEntry)
        val initialState = createInitialState(selectedItem = selectedItem)
        val newStart = startTime.minusHours(1)
        reducer.testReduceState(initialState, CalendarDayAction.TimeEntryDragged(newStart)) { state ->
            state shouldBe initialState.copy(
                backStack = state.backStack.setRouteParam {
                    Route.ContextualMenu(
                        SelectedCalendarItem.SelectedTimeEntry(validEditableTimeEntry.copy(startTime = newStart))
                    )
                }
            )
        }
    }
}
