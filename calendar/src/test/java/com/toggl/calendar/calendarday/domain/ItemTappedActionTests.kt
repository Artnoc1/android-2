package com.toggl.calendar.calendarday.domain

import com.toggl.calendar.common.CoroutineTest
import com.toggl.calendar.common.createCalendarDayReducer
import com.toggl.calendar.common.createTimeEntry
import com.toggl.calendar.common.domain.CalendarItem
import com.toggl.calendar.common.testReduceNoEffects
import com.toggl.calendar.common.testReduceState
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.backStackOf
import com.toggl.common.feature.navigation.push
import com.toggl.common.feature.navigation.setRouteParam
import com.toggl.common.feature.services.calendar.CalendarEvent
import com.toggl.models.domain.EditableTimeEntry
import io.kotlintest.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@ExperimentalCoroutinesApi
@DisplayName("The ItemTappedActionTests action")
internal class ItemTappedActionTests : CoroutineTest() {

    private val initialState = createInitialState()
    private val reducer = createCalendarDayReducer(dispatcherProvider = dispatcherProvider)

    private val timeEntry = createTimeEntry(1)
    private val calendarEvent = CalendarEvent("1", OffsetDateTime.now(), Duration.ofSeconds(10), "", "", "")

    private val timeEntryItemToBeSelected = CalendarItem.TimeEntry(timeEntry)
    private val calendarEventItemToBeSelected = CalendarItem.CalendarEvent(calendarEvent)

    @Test
    fun `should set selectedItem correctly when timeEntry is tapped and there already is another item selected`() = runBlockingTest {
        val initialState = initialState.copy(backStack = backStackOf(Route.ContextualMenu(SelectedCalendarItem.SelectedCalendarEvent(calendarEvent))))
        reducer.testReduceState(
            initialState,
            CalendarDayAction.ItemTapped(timeEntryItemToBeSelected)
        ) { state ->
            state shouldBe initialState.copy(backStack = initialState.backStack.setRouteParam {
                Route.ContextualMenu(SelectedCalendarItem.SelectedTimeEntry(EditableTimeEntry.fromSingle(timeEntry)))
            })
        }
    }

    @Test
    fun `should set selectedItem correctly when timeEntry is tapped`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            CalendarDayAction.ItemTapped(timeEntryItemToBeSelected)
        ) { state ->
            state shouldBe initialState.copy(backStack = initialState.backStack.push(
                Route.ContextualMenu(SelectedCalendarItem.SelectedTimeEntry(EditableTimeEntry.fromSingle(timeEntry)))
            ))
        }
    }

    @Test
    fun `should set selectedItem correctly when calendarEvent is tapped`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            CalendarDayAction.ItemTapped(calendarEventItemToBeSelected)
        ) { state ->
            state shouldBe initialState.copy(backStack = initialState.backStack.push(
                Route.ContextualMenu(SelectedCalendarItem.SelectedCalendarEvent(calendarEvent))
            ))
        }
    }

    @Test
    fun `shouldn't return any effect`() = runBlockingTest {
        reducer.testReduceNoEffects(initialState, CalendarDayAction.ItemTapped(timeEntryItemToBeSelected))
        reducer.testReduceNoEffects(initialState, CalendarDayAction.ItemTapped(calendarEventItemToBeSelected))
    }
}