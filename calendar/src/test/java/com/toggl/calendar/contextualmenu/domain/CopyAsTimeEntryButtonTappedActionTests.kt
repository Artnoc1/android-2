package com.toggl.calendar.contextualmenu.domain

import com.toggl.calendar.common.createCalendarEvent
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.calendar.common.testReduceEffects
import com.toggl.calendar.common.testReduceException
import com.toggl.calendar.exception.SelectedItemShouldBeACalendarEventException
import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.EditableTimeEntry
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@ExperimentalCoroutinesApi
@DisplayName("The CopyAsTimeEntryButtonTapped action")
internal class CopyAsTimeEntryButtonTappedActionTests {

    private val timeService = mockk<TimeService> { every { now() } returns OffsetDateTime.now() }
    private val reducer = ContextualMenuReducer(timeService)

    @Test
    fun `throws if executed on a time entry item`() = runBlockingTest {
        val timeEntry = EditableTimeEntry.empty(1)
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedTimeEntry(timeEntry))

        reducer.testReduceException(
            initialState = initialState,
            action = ContextualMenuAction.CopyAsTimeEntryButtonTapped,
            exception = SelectedItemShouldBeACalendarEventException::class.java
        )
    }

    @Test
    fun `returns an effect to create a time entry with the event details`() = runBlockingTest {
        val event = createCalendarEvent(description = "test", duration = Duration.ofMinutes(3), startTime = OffsetDateTime.now())
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedCalendarEvent(event))

        reducer.testReduceEffects(
            initialState = initialState,
            action = ContextualMenuAction.CopyAsTimeEntryButtonTapped
        ) { effects ->
            val action = effects.first().execute() as ContextualMenuAction.TimeEntryHandling
            action.timeEntryAction.shouldBeInstanceOf<TimeEntryAction.CreateTimeEntry>()

            val startAction = action.timeEntryAction as TimeEntryAction.CreateTimeEntry
            startAction.createTimeEntryDTO.description shouldBe event.description
            startAction.createTimeEntryDTO.startTime shouldBe event.startTime
            startAction.createTimeEntryDTO.duration shouldBe event.duration
        }
    }

    @Test
    fun `returns a close effect`() = runBlockingTest {
        val event = createCalendarEvent(description = "test", duration = Duration.ofMinutes(3), startTime = OffsetDateTime.now())
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedCalendarEvent(event))

        reducer.testReduceEffects(initialState, ContextualMenuAction.DialogDismissed) {
            it.last().execute().shouldBeInstanceOf<ContextualMenuAction.Close>()
        }
    }
}