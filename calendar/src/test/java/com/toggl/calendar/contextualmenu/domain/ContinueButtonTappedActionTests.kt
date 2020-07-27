package com.toggl.calendar.contextualmenu.domain

import com.toggl.calendar.common.createCalendarEvent
import com.toggl.calendar.common.createTimeEntry
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.calendar.common.shouldEmitTimeEntryAction
import com.toggl.calendar.common.testReduceEffects
import com.toggl.calendar.common.testReduceException
import com.toggl.calendar.exception.SelectedItemShouldBeATimeEntryException
import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.common.feature.timeentry.exceptions.TimeEntryShouldBePersistedException
import com.toggl.common.feature.timeentry.exceptions.TimeEntryShouldNotBeRunningException
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
@DisplayName("The ContinueButtonTapped action")
internal class ContinueButtonTappedActionTests {

    private val timeService = mockk<TimeService> { every { now() } returns OffsetDateTime.now() }
    private val reducer = ContextualMenuReducer(timeService)

    @Test
    fun `throws if executed on a calendar item`() = runBlockingTest {
        val calendar = createCalendarEvent()
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedCalendarEvent(calendar))

        reducer.testReduceException(
            initialState = initialState,
            action = ContextualMenuAction.ContinueButtonTapped,
            exception = SelectedItemShouldBeATimeEntryException::class.java
        )
    }

    @Test
    fun `throws if executed on a non persisted time entry`() = runBlockingTest {
        val timeEntry = EditableTimeEntry.empty(1)
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedTimeEntry(timeEntry))

        reducer.testReduceException(
            initialState = initialState,
            action = ContextualMenuAction.ContinueButtonTapped,
            exception = TimeEntryShouldBePersistedException::class.java
        )
    }

    @Test
    fun `throws if executed on a running time entry`() = runBlockingTest {
        val timeEntry = createTimeEntry(1, duration = null, startTime = OffsetDateTime.now())
        val editableTimeEntry = EditableTimeEntry.fromSingle(timeEntry)
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedTimeEntry(editableTimeEntry))

        reducer.testReduceException(
            initialState = initialState,
            action = ContextualMenuAction.ContinueButtonTapped,
            exception = TimeEntryShouldNotBeRunningException::class.java
        )
    }

    @Test
    fun `returns an effect to continue the time entry`() = runBlockingTest {
        val timeEntry = createTimeEntry(1, startTime = OffsetDateTime.now().minusMinutes(3), duration = Duration.ofMinutes(3))
        val editableTimeEntry = EditableTimeEntry.fromSingle(timeEntry)
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedTimeEntry(editableTimeEntry))

        reducer.testReduceEffects(
            initialState = initialState,
            action = ContextualMenuAction.ContinueButtonTapped
        ) { effects -> effects.first().shouldEmitTimeEntryAction<ContextualMenuAction.TimeEntryHandling, TimeEntryAction.ContinueTimeEntry> {
            it.id shouldBe timeEntry.id
        } }
    }

    @Test
    fun `returns a close effect`() = runBlockingTest {
        val timeEntry = createTimeEntry(1, startTime = OffsetDateTime.now().minusMinutes(3), duration = Duration.ofMinutes(3))
        val editableTimeEntry = EditableTimeEntry.fromSingle(timeEntry)
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedTimeEntry(editableTimeEntry))

        reducer.testReduceEffects(initialState, ContextualMenuAction.DialogDismissed) {
            it.last().execute().shouldBeInstanceOf<ContextualMenuAction.Close>()
        }
    }
}