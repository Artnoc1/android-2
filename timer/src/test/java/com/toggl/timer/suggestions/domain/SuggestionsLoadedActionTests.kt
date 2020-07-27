package com.toggl.timer.suggestions.domain

import com.toggl.common.feature.services.calendar.CalendarEvent
import com.toggl.common.services.time.TimeService
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.testReduceNoEffects
import com.toggl.timer.common.testReduceState
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime

@kotlinx.coroutines.ExperimentalCoroutinesApi
@DisplayName("The SuggestionsLoaded action")
internal class SuggestionsLoadedActionTests : CoroutineTest() {

    private val timeService = mockk<TimeService>()
    private val suggestionsProvider = mockk<SuggestionProvider>()
    private val reducer = SuggestionsReducer(timeService, suggestionsProvider)

    @Test
    fun `should set new suggestions`() = runBlockingTest {
        val initialState = createInitialState()
        val loadedSuggestions = listOf(
            Suggestion.Calendar(
                CalendarEvent("id", OffsetDateTime.MAX, Duration.ofHours(1), "", null, ""),
                1
            )
        )
        reducer.testReduceState(
            initialState,
            SuggestionsAction.SuggestionsLoaded(loadedSuggestions)
        ) { state -> state shouldBe initialState.copy(suggestions = loadedSuggestions) }
    }

    @Test
    fun `shouldn't return any effects`() = runBlockingTest {
        reducer.testReduceNoEffects(createInitialState(), SuggestionsAction.SuggestionsLoaded(emptyList()))
    }
}
