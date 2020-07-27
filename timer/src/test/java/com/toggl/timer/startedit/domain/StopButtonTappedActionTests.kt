package com.toggl.timer.startedit.domain

import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.timer.common.testReduce
import com.toggl.timer.common.testReduceNoEffects
import com.toggl.timer.common.testReduceState
import com.toggl.timer.startedit.domain.StopButtonTappedActionTests.TheoryHolder.Companion.now
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@DisplayName("The StopButtonTapped action")
class StopButtonTappedActionTests {
    val initialState = createInitialState()
    val timeService = mockk<TimeService>()
    val reducer = createReducer(timeService = timeService)

    init {
        every { timeService.now() } returns now
    }

    private fun createEditableTimeEntry(startTime: OffsetDateTime?, duration: Duration?) =
        EditableTimeEntry(
            workspaceId = 1,
            startTime = startTime,
            duration = duration
        )

    @Nested
    @DisplayName("For time entries that were started less than 999 hours ago")
    inner class ValidRunningTimeEntries : TheoryHolder {

        @ParameterizedTest
        @MethodSource("runningTimeEntriesTestData")
        fun `Sets the editableTimeEntry's duration to the duration between the start time and 'now'`(testData: StartTimeAnDurationTestData) =
            runBlockingTest {
                val initialTimeEntry = createEditableTimeEntry(testData.startTime, null)
                val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

                reducer.testReduceState(
                    initialState,
                    action = StartEditAction.StopButtonTapped
                ) {
                    it shouldBe initialState.copy(
                        editableTimeEntry = initialTimeEntry.copy(
                            startTime = testData.startTime,
                            duration = testData.expectedDuration
                        )
                    )
                }
            }

        @ParameterizedTest
        @MethodSource("runningTimeEntriesTestData")
        fun `Doesn't produce any effects`(testData: StartTimeAnDurationTestData) = runBlockingTest {
            val initialTimeEntry = createEditableTimeEntry(testData.startTime, null)
            val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

            reducer.testReduceNoEffects(
                initialState,
                action = StartEditAction.StopButtonTapped
            )
        }
    }

    @Nested
    @DisplayName("For new time entries")
    inner class NewTimeEntries : TheoryHolder {

        @Test
        fun `Sets the editableTimeEntry's start time to 'now' and the duration to Zero`() =
            runBlockingTest {
                val initialTimeEntry = createEditableTimeEntry(null, null)
                val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

                reducer.testReduceState(
                    initialState,
                    action = StartEditAction.StopButtonTapped
                ) {
                    it shouldBe initialState.copy(
                        editableTimeEntry = initialTimeEntry.copy(
                            startTime = now,
                            duration = Duration.ZERO
                        )
                    )
                }
            }

        @Test
        fun `Doesn't produce any effects`() = runBlockingTest {
            val initialTimeEntry = createEditableTimeEntry(null, null)
            val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

            reducer.testReduceNoEffects(
                initialState,
                action = StartEditAction.StopButtonTapped
            )
        }
    }

    @Test
    fun `Does nothing if editableTimeEntry's start time is more than 999 hours in the past`() = runBlockingTest {
        val forgottenTimeEntryStartTime = now.minusHours(999).minusSeconds(1)
        val initialTimeEntry = createEditableTimeEntry(forgottenTimeEntryStartTime, null)
        val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

        reducer.testReduce(
            initialState,
            action = StartEditAction.StopButtonTapped
        ) { state, effects ->
            state shouldBe initialState
            effects.shouldBeEmpty()
        }
    }

    @Test
    fun `Does nothing if editableTimeEntry's start time is running in the future`() = runBlockingTest {
        val timeEntryInTheFuture = now.plusSeconds(1)
        val initialTimeEntry = createEditableTimeEntry(timeEntryInTheFuture, null)
        val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

        reducer.testReduce(
            initialState,
            action = StartEditAction.StopButtonTapped
        ) { state, effects ->
            state shouldBe initialState
            effects.shouldBeEmpty()
        }
    }

    @Test
    fun `Does nothing if editableTimeEntry's start time is stopped`() = runBlockingTest {
        val timeEntryInTheFuture = now.minusMinutes(30)
        val initialTimeEntry = createEditableTimeEntry(timeEntryInTheFuture, Duration.ofMinutes(30))
        val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

        reducer.testReduce(
            initialState,
            action = StartEditAction.StopButtonTapped
        ) { state, effects ->
            state shouldBe initialState
            effects.shouldBeEmpty()
        }
    }

    interface TheoryHolder {

        companion object {
            val now: OffsetDateTime = OffsetDateTime.of(2020, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)

            @JvmStatic
            fun runningTimeEntriesTestData(): Stream<StartTimeAnDurationTestData> = Stream.of(
                StartTimeAnDurationTestData(now.minusMinutes(10), Duration.ofMinutes(10)),
                StartTimeAnDurationTestData(now, Duration.ZERO),
                StartTimeAnDurationTestData(now.minusHours(999), Duration.ofHours(999))
            )
        }
    }

    data class StartTimeAnDurationTestData(val startTime: OffsetDateTime, val expectedDuration: Duration)
}
