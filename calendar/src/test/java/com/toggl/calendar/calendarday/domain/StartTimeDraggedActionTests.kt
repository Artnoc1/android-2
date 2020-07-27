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
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.EditableTimeEntry
import io.kotest.matchers.shouldBe
import io.mockk.every
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
@DisplayName("The StartTimeDragged action")
class StartTimeDraggedActionTests : CoroutineTest() {
    private val timeService: TimeService = mockk()
    private val now = OffsetDateTime.of(2005, 5, 5, 11, 5, 0, 0, ZoneOffset.UTC)
    private val startTime = OffsetDateTime.of(2005, 5, 5, 5, 5, 0, 0, ZoneOffset.UTC)
    private val duration = Duration.ofHours(5)
    private val endTime = startTime.plus(duration)

    private val reducer = createCalendarDayReducer(dispatcherProvider = dispatcherProvider, timeService = timeService)

    init {
        every { timeService.now() }.returns(now)
    }

    private val validEditableTimeEntry = EditableTimeEntry.fromSingle(
        createTimeEntry(
            1,
            "test",
            startTime,
            duration
        )
    )

    private val validEditableRunningTimeEntry = EditableTimeEntry.fromSingle(
        createTimeEntry(
            1,
            "running test",
            startTime,
            null
        )
    )

    @Test
    fun `throws if executed on a calendar item`() = runBlockingTest {
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedCalendarEvent(mockk()))
        reducer.testReduceException(
            initialState,
            CalendarDayAction.StartTimeDragged(OffsetDateTime.MAX),
            SelectedItemShouldBeATimeEntryException::class.java
        )
    }

    @Test
    fun `throws if executed on a null selected item`() = runBlockingTest {
        val initialState = createInitialState(selectedItem = null)
        reducer.testReduceException(
            initialState,
            CalendarDayAction.StartTimeDragged(OffsetDateTime.MAX),
            SelectedItemShouldNotBeNullException::class.java
        )
    }

    @Test
    fun `throws if executed on a new time entry`() = runBlockingTest {
        val timeEntry = EditableTimeEntry.empty(1)
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedTimeEntry(timeEntry))
        reducer.testReduceException(
            initialState,
            CalendarDayAction.StartTimeDragged(OffsetDateTime.MAX),
            TimeEntryShouldNotBeNewException::class.java
        )
    }

    @Test
    fun `shouldn't change the state if new start time is after original end time`() = runBlockingTest {
        val selectedItem = SelectedCalendarItem.SelectedTimeEntry(validEditableTimeEntry)
        val initialState = createInitialState(selectedItem = selectedItem)
        val newStart = endTime.plusHours(1)
        reducer.testReduceState(
            initialState,
            CalendarDayAction.StartTimeDragged(newStart)
        ) { state ->
            state shouldBe initialState
        }
    }

    @Test
    fun `shouldn't change the state if new start time is after now when the time entry is running`() = runBlockingTest {
        val selectedItem = SelectedCalendarItem.SelectedTimeEntry(validEditableRunningTimeEntry)
        val initialState = createInitialState(selectedItem = selectedItem)
        val newStart = now.plusHours(1)
        reducer.testReduceState(
            initialState,
            CalendarDayAction.StartTimeDragged(newStart)
        ) { state ->
            state shouldBe initialState
        }
    }

    @Test
    fun `shouldn't return any effects`() = runBlockingTest {
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedTimeEntry(validEditableTimeEntry))
        val newStart = startTime.minusHours(1)
        reducer.testReduceNoEffects(initialState, CalendarDayAction.StartTimeDragged(newStart))
    }

    @Test
    fun `should set the new start time and increase duration when moved back for stopped time entries`() = runBlockingTest {
        val selectedItem = SelectedCalendarItem.SelectedTimeEntry(validEditableTimeEntry)
        val initialState = createInitialState(selectedItem = selectedItem)
        val newStart = startTime.minusHours(1)
        val expectedNewDuration = duration.plusHours(1)
        reducer.testReduceState(
            initialState,
            CalendarDayAction.StartTimeDragged(newStart)
        ) { state ->
            state shouldBe initialState.copy(
                backStack = state.backStack.setRouteParam {
                    Route.ContextualMenu(
                        SelectedCalendarItem.SelectedTimeEntry(
                            validEditableTimeEntry.copy(
                                startTime = newStart,
                                duration = expectedNewDuration
                            )
                        )
                    )
                }
            )
        }
    }

    @Test
    fun `should set the new start time and decrease duration when moved forward  for stopped time entries`() =
        runBlockingTest {
            val selectedItem = SelectedCalendarItem.SelectedTimeEntry(validEditableTimeEntry)
            val initialState = createInitialState(selectedItem = selectedItem)
            val newStart = startTime.plusHours(1)
            val expectedNewDuration = duration.minusHours(1)
            reducer.testReduceState(
                initialState,
                CalendarDayAction.StartTimeDragged(newStart)
            ) { state ->
                state shouldBe initialState.copy(
                    backStack = state.backStack.setRouteParam {
                        Route.ContextualMenu(
                            SelectedCalendarItem.SelectedTimeEntry(
                                validEditableTimeEntry.copy(
                                    startTime = newStart,
                                    duration = expectedNewDuration
                                )
                            )
                        )
                    }
                )
            }
        }

    @Test
    fun `should set the new start time and not set the duration when moved back for running time entries`() = runBlockingTest {
        val selectedItem = SelectedCalendarItem.SelectedTimeEntry(validEditableRunningTimeEntry)
        val initialState = createInitialState(selectedItem = selectedItem)
        val newStart = startTime.minusHours(1)
        reducer.testReduceState(
            initialState,
            CalendarDayAction.StartTimeDragged(newStart)
        ) { state ->
            state shouldBe initialState.copy(
                backStack = state.backStack.setRouteParam {
                    Route.ContextualMenu(
                        SelectedCalendarItem.SelectedTimeEntry(
                            validEditableRunningTimeEntry.copy(
                                startTime = newStart,
                                duration = null
                            )
                        )
                    )
                }
            )
        }
    }

    @Test
    fun `should set the new start time and not set the duration when moved forward  for running time entries`() =
        runBlockingTest {
            val selectedItem = SelectedCalendarItem.SelectedTimeEntry(validEditableRunningTimeEntry)
            val initialState = createInitialState(selectedItem = selectedItem)
            val newStart = startTime.plusHours(1)
            reducer.testReduceState(
                initialState,
                CalendarDayAction.StartTimeDragged(newStart)
            ) { state ->
                state shouldBe initialState.copy(
                    backStack = state.backStack.setRouteParam {
                        Route.ContextualMenu(
                            SelectedCalendarItem.SelectedTimeEntry(
                                validEditableRunningTimeEntry.copy(
                                    startTime = newStart,
                                    duration = null
                                )
                            )
                        )
                    }
                )
            }
        }
}
