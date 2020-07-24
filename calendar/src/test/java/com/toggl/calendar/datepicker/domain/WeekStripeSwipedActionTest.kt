package com.toggl.calendar.datepicker.domain

import com.toggl.calendar.common.CoroutineTest
import com.toggl.calendar.common.domain.CalendarConstants.numberOfDaysToShow
import com.toggl.calendar.common.testReduceNoEffects
import com.toggl.calendar.common.testReduceState
import com.toggl.calendar.common.toMutableValue
import com.toggl.common.services.time.TimeService
import com.toggl.models.common.SwipeDirection
import io.kotlintest.matchers.numerics.shouldBeLessThanOrEqual
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Month
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@ExperimentalCoroutinesApi
@DisplayName("The Swiped action")
internal class WeekStripeSwipedActionTest : CoroutineTest() {

    private val fixedDate = OffsetDateTime.of(2020, Month.JULY.value, 14, 0, 0, 0, 0, ZoneOffset.UTC) // Tuesday

    private val initialState = CalendarDatePickerState(
        selectedDate = fixedDate
    )

    private val timeService: TimeService = mockk()

    private val reducer = createReducer(
        timeService = timeService
    )

    init {
        every { timeService.now() }.returns(fixedDate)
    }

    @Test
    fun `Should change the selected date to "today" when swiping into the future`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            CalendarDatePickerAction.WeekStripeSwiped(SwipeDirection.Right)
        ) { state ->
            state.selectedDate.toLocalDate() shouldBe initialState.selectedDate.toLocalDate()
        }
    }

    @Test
    fun `Should change the selected date to the last available date when swiping past the past limit`() = runBlockingTest {
        val testInitialState = initialState.copy(selectedDate = fixedDate.minusDays(numberOfDaysToShow - 1))
        reducer.testReduceState(
            testInitialState,
            CalendarDatePickerAction.WeekStripeSwiped(SwipeDirection.Left)
        ) { state ->
            state.selectedDate.toLocalDate() shouldBe testInitialState.selectedDate.toLocalDate()
        }
    }

    @Test
    fun `Changes the selected date by one week`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            CalendarDatePickerAction.WeekStripeSwiped(SwipeDirection.Left)
        ) { state ->
            state.selectedDate shouldNotBe initialState.selectedDate
            state.selectedDate.dayOfWeek shouldBe initialState.selectedDate.dayOfWeek
            Duration.between(state.selectedDate, initialState.selectedDate).toDays() shouldBe 7L
        }
    }

    @Test
    fun `Swiping back and forth resets the state`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            CalendarDatePickerAction.WeekStripeSwiped(SwipeDirection.Left)
        ) { intermediateState ->
            intermediateState shouldNotBe initialState
            reducer.testReduceState(
                intermediateState,
                CalendarDatePickerAction.WeekStripeSwiped(SwipeDirection.Right)
            ) { state ->
                state shouldBe initialState
            }
        }
    }

    @Test
    fun `Shouldn't allow swiping more than two weeks into the past`() = runBlockingTest {
        var state = initialState
        val mutableState = state.toMutableValue { state = it }
        val action = CalendarDatePickerAction.WeekStripeSwiped(SwipeDirection.Left)

        repeat(5) {
            reducer.reduce(mutableState, action)
        }

        Duration.between(initialState.selectedDate, state.selectedDate).toDays() shouldBeLessThanOrEqual 14
    }

    @Test
    fun `Shouldn't return any effects for swiping left`() = runBlockingTest {
        reducer.testReduceNoEffects(
            initialState,
            CalendarDatePickerAction.WeekStripeSwiped(SwipeDirection.Left)
        )
    }

    @Test
    fun `Shouldn't return any effects for swiping right`() = runBlockingTest {
        reducer.testReduceNoEffects(
            initialState,
            CalendarDatePickerAction.WeekStripeSwiped(SwipeDirection.Right)
        )
    }
}