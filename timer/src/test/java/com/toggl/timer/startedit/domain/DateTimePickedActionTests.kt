package com.toggl.timer.startedit.domain

import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.repository.Repository
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.assertNoEffectsWereReturned
import com.toggl.timer.common.testReduce
import com.toggl.timer.common.testReduceEffects
import com.toggl.timer.common.testReduceException
import com.toggl.timer.common.testReduceState
import com.toggl.timer.exceptions.EditableTimeEntryDoesNotHaveADurationSetException
import com.toggl.timer.exceptions.EditableTimeEntryDoesNotHaveAStartTimeSetException
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@DisplayName("The DateTimePicked action")
internal class DateTimePickedActionTests : CoroutineTest() {
    val repository = mockk<Repository>()
    val timeService = mockk<TimeService>()
    val reducer = StartEditReducer(repository, timeService, dispatcherProvider)

    interface TheoryHolder {
        companion object {
            val oneHour = Duration.ofHours(1)
            val midnight = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            val oneAM = midnight + oneHour
            val midday = OffsetDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)
            val onePM = midday + oneHour

            @JvmStatic
            fun temporalInconsistenciesTheory(): Stream<DateTimePickedTemporalInconsistencyTestData> = Stream.of(
                DateTimePickedTemporalInconsistencyTestData(
                    pickMode = DateTimePickMode.StartTime,
                    editableTimeEntryStartTime = null,
                    editableTimeEntryDuration = null,
                    now = midday,
                    newDateTime = midday.minusHours(999).minusSeconds(1),
                    exceptedTemporalInconsistency = TemporalInconsistency.DurationTooLong
                ),
                DateTimePickedTemporalInconsistencyTestData(
                    pickMode = DateTimePickMode.StartDate,
                    editableTimeEntryStartTime = null,
                    editableTimeEntryDuration = null,
                    now = midday,
                    newDateTime = midday.minusHours(999).minusSeconds(1),
                    exceptedTemporalInconsistency = TemporalInconsistency.DurationTooLong
                ),

                DateTimePickedTemporalInconsistencyTestData(
                    pickMode = DateTimePickMode.StartTime,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = null,
                    now = midday,
                    newDateTime = midday.minusHours(999).minusSeconds(1),
                    exceptedTemporalInconsistency = TemporalInconsistency.DurationTooLong
                ),
                DateTimePickedTemporalInconsistencyTestData(
                    pickMode = DateTimePickMode.StartDate,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = null,
                    now = midday,
                    newDateTime = midday.minusHours(999).minusSeconds(1),
                    exceptedTemporalInconsistency = TemporalInconsistency.DurationTooLong
                ),

                DateTimePickedTemporalInconsistencyTestData(
                    pickMode = DateTimePickMode.StartTime,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = oneHour,
                    now = midday,
                    newDateTime = onePM.plusMinutes(1),
                    exceptedTemporalInconsistency = TemporalInconsistency.StartTimeAfterStopTime
                ),
                DateTimePickedTemporalInconsistencyTestData(
                    pickMode = DateTimePickMode.StartDate,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = oneHour,
                    now = midday,
                    newDateTime = onePM.minusHours(999).minusSeconds(1),
                    exceptedTemporalInconsistency = TemporalInconsistency.DurationTooLong
                ),

                DateTimePickedTemporalInconsistencyTestData(
                    pickMode = DateTimePickMode.EndTime,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = oneHour,
                    now = midday,
                    newDateTime = oneAM,
                    exceptedTemporalInconsistency = TemporalInconsistency.StopTimeBeforeStartTime
                ),
                DateTimePickedTemporalInconsistencyTestData(
                    pickMode = DateTimePickMode.EndDate,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = oneHour,
                    now = midday,
                    newDateTime = oneAM,
                    exceptedTemporalInconsistency = TemporalInconsistency.StopTimeBeforeStartTime
                ),

                DateTimePickedTemporalInconsistencyTestData(
                    pickMode = DateTimePickMode.EndTime,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = oneHour,
                    now = midday,
                    newDateTime = midday.plusHours(999).plusMinutes(1),
                    exceptedTemporalInconsistency = TemporalInconsistency.DurationTooLong
                ),
                DateTimePickedTemporalInconsistencyTestData(
                    pickMode = DateTimePickMode.EndDate,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = oneHour,
                    now = midday,
                    newDateTime = midday.plusHours(999).plusMinutes(1),
                    exceptedTemporalInconsistency = TemporalInconsistency.DurationTooLong
                )
            )

            @JvmStatic
            fun happyPathsTheory(): Stream<ValidDateTimePickedTestData> = Stream.of(
                ValidDateTimePickedTestData(
                    pickMode = DateTimePickMode.StartTime,
                    editableTimeEntryStartTime = null,
                    editableTimeEntryDuration = null,
                    now = midday,
                    pickedTime = oneAM,
                    expectedStartTime = oneAM,
                    expectedDuration = null
                ),
                ValidDateTimePickedTestData(
                    pickMode = DateTimePickMode.StartDate,
                    editableTimeEntryStartTime = null,
                    editableTimeEntryDuration = null,
                    now = midday,
                    pickedTime = oneAM,
                    expectedStartTime = oneAM,
                    expectedDuration = null
                ),

                ValidDateTimePickedTestData(
                    pickMode = DateTimePickMode.StartTime,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = null,
                    now = midday,
                    pickedTime = oneAM,
                    expectedStartTime = oneAM,
                    expectedDuration = null
                ),
                ValidDateTimePickedTestData(
                    pickMode = DateTimePickMode.StartDate,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = null,
                    now = midday,
                    pickedTime = oneAM,
                    expectedStartTime = oneAM,
                    expectedDuration = null
                ),

                ValidDateTimePickedTestData(
                    pickMode = DateTimePickMode.StartTime,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = oneHour,
                    now = midday,
                    pickedTime = midnight,
                    expectedStartTime = midnight,
                    expectedDuration = Duration.ofHours(13)
                ),
                ValidDateTimePickedTestData(
                    pickMode = DateTimePickMode.StartDate,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = oneHour,
                    now = midday,
                    pickedTime = midnight,
                    expectedStartTime = midnight,
                    expectedDuration = Duration.ofHours(13)
                ),

                ValidDateTimePickedTestData(
                    pickMode = DateTimePickMode.EndTime,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = oneHour,
                    now = midday,
                    pickedTime = midday.plusMinutes(30),
                    expectedStartTime = midday,
                    expectedDuration = Duration.ofMinutes(30)
                ),
                ValidDateTimePickedTestData(
                    pickMode = DateTimePickMode.EndDate,
                    editableTimeEntryStartTime = midday,
                    editableTimeEntryDuration = oneHour,
                    now = midday,
                    pickedTime = midday.plusHours(5),
                    expectedStartTime = midday,
                    expectedDuration = Duration.ofHours(5)
                )
            )
        }
    }

    @ParameterizedTest
    @EnumSource(DateTimePickMode::class, names = ["EndTime", "EndDate"])
    @DisplayName("EditableTimeEntryDoesNotHaveAStartTimeSetException is thrown when trying pick and EndDateTime and there's no start time set")
    fun anExceptionIsThrownWhenTryingToPickADateTimeWhenTheresNoStartTime(pickMode: DateTimePickMode) {
        val initialEditableTimeEntry = EditableTimeEntry.empty(1)
            .copy(
                startTime = null,
                duration = null
            )
        every { timeService.now() }.returns(OffsetDateTime.now())
        val initialState = createInitialState().copy(
            dateTimePickMode = pickMode,
            editableTimeEntry = initialEditableTimeEntry
        )

        reducer.testReduceException(
            initialState,
            StartEditAction.DateTimePicked(mockk()),
            EditableTimeEntryDoesNotHaveAStartTimeSetException::class.java
        )
    }

    @ParameterizedTest
    @EnumSource(DateTimePickMode::class, names = ["EndTime", "EndDate"])
    @DisplayName("EditableTimeEntryDoesNotHaveADurationSetException.kt is thrown when trying pick and EndDateTime and there's no duration set")
    fun anExceptionIsThrownWhenTryingToPickADateTimeWhenTheresNoDurationSet(pickMode: DateTimePickMode) {
        val initialEditableTimeEntry = EditableTimeEntry.empty(1)
            .copy(
                startTime = OffsetDateTime.now(),
                duration = null
            )
        every { timeService.now() }.returns(OffsetDateTime.now())
        val initialState = createInitialState().copy(
            dateTimePickMode = pickMode,
            editableTimeEntry = initialEditableTimeEntry
        )

        reducer.testReduceException(
            initialState,
            StartEditAction.DateTimePicked(mockk()),
            EditableTimeEntryDoesNotHaveADurationSetException::class.java
        )
    }

    @DisplayName("Nothing happens when the pick mode is None")
    @Nested
    inner class None : TheoryHolder {
        @ParameterizedTest
        @MethodSource("temporalInconsistenciesTheory")
        fun test(testData: DateTimePickedTemporalInconsistencyTestData) = runBlockingTest {
            val (_, startTime, duration, now, pickedTime, _) = testData
            val initialEditableTimeEntry = EditableTimeEntry.empty(1)
                .copy(
                    startTime = startTime,
                    duration = duration
                )
            every { timeService.now() }.returns(now)
            val initialState = createInitialState().copy(
                dateTimePickMode = DateTimePickMode.None,
                editableTimeEntry = initialEditableTimeEntry
            )
            val pickAction = StartEditAction.DateTimePicked(pickedTime)

            reducer.testReduce(
                initialState,
                pickAction
            ) { state, effect ->
                state shouldBe initialState
                effect.shouldBeEmpty()
            }
        }
    }

    @Nested
    @DisplayName("When temporal inconsistencies are detected")
    inner class TemporalInconsistenciesTests : TheoryHolder {

        lateinit var initialState: StartEditState
        lateinit var pickAction: StartEditAction.DateTimePicked
        lateinit var expectedInconsistency: TemporalInconsistency

        fun setup(testData: DateTimePickedTemporalInconsistencyTestData) {
            val (pickMode, startTime, duration, now, pickedTime, inconsistency) = testData
            val initialEditableTimeEntry = EditableTimeEntry.empty(1)
                .copy(
                    startTime = startTime,
                    duration = duration
                )
            every { timeService.now() }.returns(now)
            initialState = createInitialState().copy(
                dateTimePickMode = pickMode,
                editableTimeEntry = initialEditableTimeEntry
            )
            pickAction = StartEditAction.DateTimePicked(pickedTime)
            expectedInconsistency = inconsistency
        }

        @ParameterizedTest
        @MethodSource("temporalInconsistenciesTheory")
        @DisplayName("the temporal inconsistency is set to the state")
        fun temporalInconsistenciesAreDetected(testData: DateTimePickedTemporalInconsistencyTestData) = runBlockingTest {
            setup(testData)
            reducer.testReduceState(
                initialState,
                pickAction
            ) {
                it.temporalInconsistency shouldBe expectedInconsistency
            }
        }

        @ParameterizedTest
        @MethodSource("temporalInconsistenciesTheory")
        @DisplayName("the dateTimePickMode is set to None")
        fun dateTimePickModeIsReset(testData: DateTimePickedTemporalInconsistencyTestData) = runBlockingTest {
            setup(testData)
            reducer.testReduceState(
                initialState,
                pickAction
            ) {
                it.dateTimePickMode shouldBe DateTimePickMode.None
            }
        }

        @ParameterizedTest
        @MethodSource("temporalInconsistenciesTheory")
        @DisplayName("an effect to Pick the date time again is produced")
        fun reopenPickerEffectIsProduced(testData: DateTimePickedTemporalInconsistencyTestData) = runBlockingTest {
            setup(testData)
            reducer.testReduceEffects(
                initialState,
                pickAction
            ) {
                it.shouldBeSingleton()
                (it.first() as ReopenPickerEffect).dateTimePickMode shouldBe initialState.dateTimePickMode
            }
        }
    }

    @Nested
    @DisplayName("When no temporal inconsistencies are detected")
    inner class HappyPaths : TheoryHolder {
        lateinit var initialState: StartEditState
        lateinit var pickAction: StartEditAction.DateTimePicked
        lateinit var expectedStartTime: OffsetDateTime
        var expectedDuration: Duration? = null

        fun setup(testData: ValidDateTimePickedTestData) {
            val (pickMode, startTime, duration, now, pickedTime, start, end) = testData
            val initialEditableTimeEntry = EditableTimeEntry.empty(1)
                .copy(
                    startTime = startTime,
                    duration = duration
                )
            every { timeService.now() }.returns(now)
            initialState = createInitialState().copy(
                dateTimePickMode = pickMode,
                editableTimeEntry = initialEditableTimeEntry
            )
            pickAction = StartEditAction.DateTimePicked(pickedTime)
            expectedStartTime = start
            expectedDuration = end
        }

        @ParameterizedTest
        @MethodSource("happyPathsTheory")
        @DisplayName("the target is set")
        fun targetIsSet(testData: ValidDateTimePickedTestData) = runBlockingTest {
            setup(testData)
            reducer.testReduceState(
                initialState,
                pickAction
            ) {
                it.editableTimeEntry.startTime shouldBe expectedStartTime
                it.editableTimeEntry.duration shouldBe expectedDuration
            }
        }

        @ParameterizedTest
        @MethodSource("happyPathsTheory")
        @DisplayName("the dateTimePickMode is set to None")
        fun dateTimePickModeIsReset(testData: ValidDateTimePickedTestData) = runBlockingTest {
            setup(testData)
            reducer.testReduceState(
                initialState,
                pickAction
            ) {
                it.dateTimePickMode shouldBe DateTimePickMode.None
            }
        }

        @ParameterizedTest
        @MethodSource("happyPathsTheory")
        @DisplayName("no effect is produced")
        fun noEffectIsProduced(testData: ValidDateTimePickedTestData) = runBlockingTest {
            setup(testData)
            reducer.testReduce(
                initialState,
                pickAction,
                ::assertNoEffectsWereReturned
            )
        }
    }

    data class DateTimePickedTemporalInconsistencyTestData(
        val pickMode: DateTimePickMode,
        val editableTimeEntryStartTime: OffsetDateTime?,
        val editableTimeEntryDuration: Duration?,
        val now: OffsetDateTime,
        val newDateTime: OffsetDateTime,
        val exceptedTemporalInconsistency: TemporalInconsistency
    ) {
        override fun toString(): String {
            return "pickMode: $pickMode, TE(start: $editableTimeEntryStartTime, duration: $editableTimeEntryDuration), now: $now, newDateTime: $newDateTime, inconsistency: $exceptedTemporalInconsistency "
        }
    }

    data class ValidDateTimePickedTestData(
        val pickMode: DateTimePickMode,
        val editableTimeEntryStartTime: OffsetDateTime?,
        val editableTimeEntryDuration: Duration?,
        val now: OffsetDateTime,
        val pickedTime: OffsetDateTime,
        val expectedStartTime: OffsetDateTime,
        val expectedDuration: Duration?
    ) {
        override fun toString(): String {
            return "pickMode: $pickMode, TE(start: $editableTimeEntryStartTime, duration: $editableTimeEntryDuration), now: $now, pickedTime: $pickedTime, expectedStart: $expectedStartTime, expectedDuration: $expectedDuration"
        }
    }
}