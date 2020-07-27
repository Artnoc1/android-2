package com.toggl.timer.suggestions.domain

import com.toggl.common.Constants
import com.toggl.common.feature.services.calendar.CalendarEvent
import com.toggl.common.services.time.TimeService
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.createCalendarEvent
import io.kotest.matchers.boolean.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.should
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@DisplayName("The CalendarSuggestionProvider")
class CalendarSuggestionProviderTest : CoroutineTest() {

    private val timeService = mockk<TimeService> { every { now() } returns now }
    private val provider = CalendarSuggestionProvider(
        timeService,
        dispatcherProvider,
        Constants.Suggestions.maxNumberOfCalendarSuggestions
    )

    @Test
    fun `gets events within 1 hour before or after now`() = runBlockingTest {
        val calendarSuggestionsProvider = CalendarSuggestionProvider(
            timeService,
            dispatcherProvider,
            300
        )
        val span = Duration.ofHours(1)
        val expectedStartOfRange = now - span
        val expectedEndOfRange = now + span
        val calendarEvents = (-61..61).map {
            CalendarEvent(
                it.toString(),
                now + Duration.ofMinutes(it.toLong()),
                Duration.ofMinutes(30),
                "Cool Event",
                "",
                ""
            )
        }
        val initialState = createInitialState(calendarEvents = calendarEvents.associateBy { it.id })

        val suggestions = calendarSuggestionsProvider.getSuggestions(initialState)
        suggestions.shouldHaveSize(121) // out of bounds (123 - 2 == 121)
        suggestions.forEach { suggestion ->
            suggestion.should { it is Suggestion.Calendar && it.calendarEvent.startTime > expectedStartOfRange && it.calendarEvent.startTime < expectedEndOfRange }
        }
    }

    @Test
    fun `does not include calendar events without a description`() = runBlockingTest {

        val calendarEvents = listOf(createCalendarEvent(description = "", startTime = now))
        val initialState = createInitialState(calendarEvents = calendarEvents.associateBy { it.id })

        val suggestions = provider.getSuggestions(initialState)

        suggestions.shouldBeEmpty()
    }

    @Test
    fun `creates the suggestions using the default workspace id of the current user`() = runBlockingTest {
        val user = createUser()
        val calendarEvents =
            listOf("This is valid", "This is also valid").map { createCalendarEvent(description = it, startTime = now) }
        val initialState = createInitialState(user = user, calendarEvents = calendarEvents.associateBy { it.id })

        val suggestions = provider.getSuggestions(initialState)

        suggestions.all { it is Suggestion.Calendar && it.workspaceId == user.defaultWorkspaceId }.shouldBeTrue()
    }

    @Test
    fun `returns at most N calendar suggestions`() = runBlockingTest {
        val maxSuggestionNumber = Constants.Suggestions.maxNumberOfCalendarSuggestions
        val user = createUser()
        val calendarEvents =
            listOf("This is valid", "This is also valid").map { createCalendarEvent(description = it, startTime = now) }
        val initialState = createInitialState(user = user, calendarEvents = calendarEvents.associateBy { it.id })

        val suggestions = provider.getSuggestions(initialState)

        suggestions shouldHaveSize maxSuggestionNumber
    }

    @ParameterizedTest
    @MethodSource("calendarEvents")
    fun `sorts by the distance between the event start and now`(testData: CalendarTestData) = runBlockingTest {
        val suggestionsState = createInitialState(calendarEvents = testData.events.associateBy { it.id })
        val suggestion = provider.getSuggestions(suggestionsState).filterIsInstance<Suggestion.Calendar>().single()

        suggestion.calendarEvent shouldBe testData.closestEvent
    }

    data class CalendarTestData(val events: List<CalendarEvent>, val closestEvent: CalendarEvent)

    companion object {
        private val now = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

        @JvmStatic
        fun calendarEvents(): Stream<CalendarTestData> = Stream.of(
            CalendarTestData(
                listOf(
                    createCalendarEvent(id = "a", description = "A", startTime = now.plusMinutes(1)),
                    createCalendarEvent(id = "b", description = "B", startTime = now.plusMinutes(2)),
                    createCalendarEvent(id = "c", description = "C", startTime = now.plusMinutes(3))
                ), createCalendarEvent(id = "a", description = "A", startTime = now.plusMinutes(1))
            ),
            CalendarTestData(
                listOf(
                    createCalendarEvent(id = "a", description = "A", startTime = now.plusMinutes(3)),
                    createCalendarEvent(id = "b", description = "B", startTime = now.minusMinutes(1)),
                    createCalendarEvent(id = "c", description = "C", startTime = now.plusMinutes(2))
                ), createCalendarEvent(id = "b", description = "B", startTime = now.minusMinutes(1))
            )
        )
    }
}