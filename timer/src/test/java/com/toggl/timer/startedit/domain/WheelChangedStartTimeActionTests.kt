package com.toggl.timer.startedit.domain

import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.timer.common.testReduceState
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
@DisplayName("The WheelChangedStartTime action")
class WheelChangedStartTimeActionTests {
    val initialState = createInitialState()
    val timeService = mockk<TimeService>()
    val reducer = createReducer(timeService = timeService)

    private fun createEditableTimeEntry(startTime: OffsetDateTime?, duration: Duration?) =
        EditableTimeEntry(
            workspaceId = 1,
            startTime = startTime,
            duration = duration
        )

    companion object {
        val now: OffsetDateTime = OffsetDateTime.of(2020, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)

        @JvmStatic
        fun validEntriesForRunningOrNewTimeEntries(): Stream<RunningTimeEntryTestData> = Stream.of(
            RunningTimeEntryTestData(now),
            RunningTimeEntryTestData(now.minusHours(999)),
            RunningTimeEntryTestData(now.minusHours(1))
        )

        @JvmStatic
        fun validEntriesAndExpectedDurationsForStoppedTimeEntries(): Stream<StoppedTimeEntryTestData> = Stream.of(
            StoppedTimeEntryTestData(now, Duration.ZERO, now, Duration.ZERO),
            StoppedTimeEntryTestData(now, Duration.ZERO, now.minusMinutes(30), Duration.ofMinutes(30)),
            StoppedTimeEntryTestData(now, Duration.ZERO, now.minusHours(999), Duration.ofHours(999)),
            StoppedTimeEntryTestData(now, Duration.ofMinutes(30), now.minusMinutes(15), Duration.ofMinutes(45)),
            StoppedTimeEntryTestData(now, Duration.ofMinutes(10), now.plusMinutes(10), Duration.ZERO)
        )
    }

    init {
        every { timeService.now() } returns now
    }

    @ParameterizedTest
    @MethodSource("validEntriesForRunningOrNewTimeEntries")
    fun `sets the DateTime inputted to the editableTimeEntry's startTime when the time entry is running`(testData: RunningTimeEntryTestData) =
        runBlockingTest {
            val initialTimeEntry = createEditableTimeEntry(now, null)
            val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

            reducer.testReduceState(
                initialState,
                action = StartEditAction.WheelChangedStartTime(testData.inputtedStartTime)
            ) {
                it shouldBe initialState.copy(
                    editableTimeEntry = it.editableTimeEntry.copy(
                        startTime = testData.inputtedStartTime
                    )
                )
            }
        }

    @ParameterizedTest
    @MethodSource("validEntriesForRunningOrNewTimeEntries")
    fun `sets the DateTime inputted to the editableTimeEntry's startTime when the time entry is new`(testData: RunningTimeEntryTestData) =
        runBlockingTest {
            val initialTimeEntry = createEditableTimeEntry(null, null)
            val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

            reducer.testReduceState(
                initialState,
                action = StartEditAction.WheelChangedStartTime(testData.inputtedStartTime)
            ) {
                it shouldBe initialState.copy(
                    editableTimeEntry = it.editableTimeEntry.copy(
                        startTime = testData.inputtedStartTime
                    )
                )
            }
        }

    @ParameterizedTest
    @MethodSource("validEntriesAndExpectedDurationsForStoppedTimeEntries")
    fun `sets the DateTime inputted to the editableTimeEntry's startTime, adjusting the duration when the time entry is stopped`(
        testData: StoppedTimeEntryTestData
    ) = runBlockingTest {
        val initialTimeEntry = createEditableTimeEntry(testData.initialStartTime, testData.initialDuration)
        val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

        reducer.testReduceState(
            initialState,
            action = StartEditAction.WheelChangedStartTime(testData.inputtedStartTime)
        ) {
            it shouldBe initialState.copy(
                editableTimeEntry = it.editableTimeEntry.copy(
                    startTime = testData.inputtedStartTime,
                    duration = testData.expectedDuration
                )
            )
        }
    }

    @Test
    fun `does not change anything when the inputted DateTime is past now for a running time entry`() = runBlockingTest {
        val initialStartTime = now - Duration.ofHours(1)
        val initialTimeEntry = createEditableTimeEntry(initialStartTime, null)
        val startTimeInputted = now.plusSeconds(1)
        val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

        reducer.testReduceState(
            initialState,
            action = StartEditAction.WheelChangedStartTime(startTimeInputted)
        ) {
            it shouldBe initialState
        }
    }

    @Test
    fun `does not change anything when the inputted DateTime would result in a time entry with a duration longer than 999 hours`() =
        runBlockingTest {
            val initialStartTime = now
            val initialDuration = Duration.ofMinutes(30)
            val initialTimeEntry = createEditableTimeEntry(initialStartTime, initialDuration)
            val startTimeInputted = now.minusHours(998).minusMinutes(31)
            val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

            reducer.testReduceState(
                initialState,
                action = StartEditAction.WheelChangedStartTime(startTimeInputted)
            ) {
                it shouldBe initialState
            }
        }

    @Test
    fun `does not change anything when the inputted DateTime would result in a time entry with a negative duration`() =
        runBlockingTest {
            val initialStartTime = now
            val initialDuration = Duration.ofMinutes(10)
            val initialTimeEntry = createEditableTimeEntry(initialStartTime, initialDuration)
            val startTimeInputted = now.plusMinutes(11)
            val initialState = initialState.copy(editableTimeEntry = initialTimeEntry)

            reducer.testReduceState(
                initialState,
                action = StartEditAction.WheelChangedStartTime(startTimeInputted)
            ) {
                it shouldBe initialState
            }
        }

    data class RunningTimeEntryTestData(
        val inputtedStartTime: OffsetDateTime
    )

    data class StoppedTimeEntryTestData(
        val initialStartTime: OffsetDateTime,
        val initialDuration: Duration,
        val inputtedStartTime: OffsetDateTime,
        val expectedDuration: Duration
    )
}
