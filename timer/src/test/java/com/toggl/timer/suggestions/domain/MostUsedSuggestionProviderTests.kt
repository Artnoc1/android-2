package com.toggl.timer.suggestions.domain

import com.toggl.common.Constants
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.TimeEntry
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.createTimeEntry
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@DisplayName("The MostUsedSuggestionProvider")
class MostUsedSuggestionProviderTests : CoroutineTest() {

    private val timeService = mockk<TimeService> { every { now() } returns now }
    private val provider = MostUsedSuggestionProvider(timeService, dispatcherProvider, Constants.Suggestions.maxNumberOfMostUsedSuggestions)

    @Test
    fun `returns at most N time entry suggestions`() = runBlockingTest {

        val maxSuggestionNumber = Constants.Suggestions.maxNumberOfMostUsedSuggestions

        val timeEntries = (1L..10L).map { createTimeEntry(id = it, description = it.toString()) }
        val initialState = createInitialState(timeEntries = timeEntries)

        val suggestions = provider.getSuggestions(initialState)

        suggestions shouldHaveSize maxSuggestionNumber
    }

    @ParameterizedTest
    @MethodSource("filterEmptyEntries")
    fun `ignores entries without descriptions`(testData: TestData) = runBlockingTest {
        val suggestions = provider.getSuggestions(createInitialState(timeEntries = testData.entries))
            .filterIsInstance<Suggestion.MostUsed>()

        suggestions.map { it.timeEntry } shouldBe testData.pickedTimeEntries
    }

    @ParameterizedTest
    @MethodSource("timeEntriesByAmount")
    fun `sorts by the amount of entries repeated`(testData: TestData) = runBlockingTest {

        val suggestions = provider.getSuggestions(createInitialState(timeEntries = testData.entries))
            .filterIsInstance<Suggestion.MostUsed>()

        suggestions.map { it.timeEntry } shouldBe testData.pickedTimeEntries
    }

    data class TestData(val entries: List<TimeEntry>, val pickedTimeEntries: List<TimeEntry>)

    companion object {
        private val now = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

        private fun createSimilarEntries(map: Map<String, Int>): List<TimeEntry> {
            var index = 0L
            return map.flatMap { (description, amount) ->
                (0..amount).map { createTimeEntry(id = index++, description = description, startTime = now) }
            }
        }

        @JvmStatic
        fun timeEntriesByAmount(): Stream<TestData> = Stream.of(
            TestData(
                createSimilarEntries(mapOf("One" to 1, "Two" to 2, "Three" to 3, "Five" to 5)),
                listOf(createTimeEntry(9, "Five", startTime = now), createTimeEntry(5, "Three", startTime = now), createTimeEntry(2, "Two", startTime = now))
            )
        )

        @JvmStatic
        fun filterEmptyEntries(): Stream<TestData> = Stream.of(
            TestData(
                createSimilarEntries(mapOf("One" to 1, "Two" to 2, "Three" to 3, "Five" to 5, "" to 10)),
                listOf(createTimeEntry(9, "Five", startTime = now), createTimeEntry(5, "Three", startTime = now), createTimeEntry(2, "Two", startTime = now))
            )
        )
    }
}