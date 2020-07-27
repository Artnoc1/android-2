package com.toggl.timer.suggestions.domain

import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.common.services.time.TimeService
import com.toggl.repository.dto.StartTimeEntryDTO
import com.toggl.timer.common.createCalendarEvent
import com.toggl.timer.common.createTimeEntry
import com.toggl.timer.common.shouldEmitTimeEntryAction
import com.toggl.timer.common.testReduceEffects
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@kotlinx.coroutines.ExperimentalCoroutinesApi
@DisplayName("The SuggestionTapped action")
internal class SuggestionTappedTests {

    private val timeService = mockk<TimeService> { every { now() } returns OffsetDateTime.now() }
    private val timeEntry = createTimeEntry(1, "Some description")
    private val initialState = createInitialState(timeEntries = listOf(timeEntry))
    private val suggestionProvider = mockk<SuggestionProvider>()
    private val reducer = SuggestionsReducer(timeService, suggestionProvider)

    @Test
    fun `returns an effect to continue a time entry when receiving most used suggestions`() = runBlockingTest {
        val suggestion = Suggestion.MostUsed(timeEntry)

        reducer.testReduceEffects(initialState, SuggestionsAction.SuggestionTapped(suggestion)) { effects ->
            effects.single().shouldEmitTimeEntryAction<SuggestionsAction.TimeEntryHandling, TimeEntryAction.ContinueTimeEntry> {
                it.id shouldBe timeEntry.id
            }
        }
    }

    @Test
    fun `returns an effect to start a time entry when receiving calendar suggestions`() = runBlockingTest {
        val event = createCalendarEvent()
        val suggestion = Suggestion.Calendar(event, 1)

        reducer.testReduceEffects(initialState, SuggestionsAction.SuggestionTapped(suggestion)) { effects ->
            effects.single().shouldEmitTimeEntryAction<SuggestionsAction.TimeEntryHandling, TimeEntryAction.StartTimeEntry> {
                it.startTimeEntryDTO shouldBe StartTimeEntryDTO(
                    description = event.description,
                    startTime = event.startTime,
                    billable = false,
                    workspaceId = suggestion.workspaceId,
                    projectId = null,
                    taskId = null,
                    tagIds = emptyList()
                )
            }
        }
    }
}
