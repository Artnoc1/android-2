package com.toggl.calendar.calendarday.domain

import com.toggl.calendar.common.CoroutineTest
import com.toggl.calendar.common.createCalendarDayReducer
import com.toggl.calendar.common.createCalendarEvent
import com.toggl.calendar.common.testReduceNoEffects
import com.toggl.calendar.common.testReduceState
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@ExperimentalCoroutinesApi
@DisplayName("The CalendarEventsFetched action")
internal class CalendarEventsFetchedActionTests : CoroutineTest() {

    private val initialState = createInitialState()
    private val reducer = createCalendarDayReducer(dispatcherProvider = dispatcherProvider)

    private val calendarEvents = listOf(
        createCalendarEvent("1"),
        createCalendarEvent("2"),
        createCalendarEvent("3")
    )

    @Test
    fun `Should set calendar events`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            CalendarDayAction.CalendarEventsFetched(calendarEvents)
        ) { state ->
            state shouldBe initialState.copy(
                events = mapOf(
                    "1" to calendarEvents[0],
                    "2" to calendarEvents[1],
                    "3" to calendarEvents[2]
                )
            )
        }
    }

    @Test
    fun `Shouldn't return any effects`() = runBlockingTest {
        reducer.testReduceNoEffects(
            initialState,
            CalendarDayAction.CalendarEventsFetched(calendarEvents)
        )
    }
}