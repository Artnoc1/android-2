package com.toggl.calendar.datepicker.domain

import com.toggl.calendar.common.CoroutineTest
import com.toggl.calendar.common.testReduceNoEffects
import com.toggl.calendar.common.testReduceState
import com.toggl.common.services.time.TimeService
import com.toggl.models.common.SwipeDirection
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@ExperimentalCoroutinesApi
@DisplayName("The DaySwiped action")
class DaySwipedActionTests : CoroutineTest() {
    val timeService: TimeService = mockk()

    val reducer = createReducer(
        timeService = timeService
    )

    init {
        every { timeService.now() }.returns(OffsetDateTime.parse("2020-02-20T20:20:20+00:00"))
    }

    @Test
    fun `When swiping left, updates the selected date to yesterday`() = runBlockingTest {
        val state = createInitialState()
        val initialDate = state.selectedDate

        reducer.testReduceState(state, CalendarDatePickerAction.DaySwiped(SwipeDirection.Left)) {
            it.selectedDate shouldBe initialDate.minusDays(1)
        }
    }

    @Test
    fun `When swiping right, updates the selected date to tomorrow`() = runBlockingTest {
        val state = createInitialState()
        val initialDate = state.selectedDate
        every { timeService.now() }.returns(initialDate.plusDays(1))

        reducer.testReduceState(state, CalendarDatePickerAction.DaySwiped(SwipeDirection.Right)) {
            it.selectedDate shouldBe initialDate.plusDays(1)
        }
    }

    @Test
    fun `Can swipe left to the first available day`() = runBlockingTest {
        val today = OffsetDateTime.parse("2020-02-20T20:20:20+00:00")
        every { timeService.now() }.returns(today)
        val state = createInitialState(
            selectedDate = today.minusDays(12)
        )

        reducer.testReduceState(state, CalendarDatePickerAction.DaySwiped(SwipeDirection.Left)) {
            it.selectedDate.dayOfMonth shouldBe 7
        }
    }

    @Test
    fun `Can swipe right to today`() = runBlockingTest {
        val today = OffsetDateTime.parse("2020-02-20T20:20:20+00:00")
        every { timeService.now() }.returns(today)
        val state = createInitialState(
            selectedDate = today.minusDays(1)
        )

        reducer.testReduceState(state, CalendarDatePickerAction.DaySwiped(SwipeDirection.Right)) {
            it.selectedDate.dayOfMonth shouldBe 20
        }
    }

    @Test
    fun `When the swiping left when the selected date is two weeks in the past, does nothing`() = runBlockingTest {
        val state = createInitialState()
        val initialDate = state.selectedDate
        every { timeService.now() }.returns(state.selectedDate.plusWeeks(2))

        reducer.testReduceState(state, CalendarDatePickerAction.DaySwiped(SwipeDirection.Left)) {
            it.selectedDate shouldBe initialDate
        }
    }

    @Test
    fun `When the swiping right when the selected date is today, does nothing`() = runBlockingTest {
        val state = createInitialState()
        val initialDate = state.selectedDate
        every { timeService.now() }.returns(state.selectedDate)

        reducer.testReduceState(state, CalendarDatePickerAction.DaySwiped(SwipeDirection.Right)) {
            it.selectedDate shouldBe initialDate
        }
    }

    @Test
    fun `Produces no effects`() = runBlockingTest {
        val state = createInitialState()

        reducer.testReduceNoEffects(state, CalendarDatePickerAction.DaySwiped(SwipeDirection.Left))
    }
}