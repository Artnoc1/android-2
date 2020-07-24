package com.toggl.calendar.datepicker.domain

import com.toggl.calendar.common.CoroutineTest
import com.toggl.calendar.common.testReduceNoEffects
import com.toggl.calendar.common.testReduceState
import io.kotlintest.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@ExperimentalCoroutinesApi
@DisplayName("The DaySelected action")
class DaySelectedActionTests : CoroutineTest() {

    val reducer = createReducer()

    @Test
    fun `Updates the selected day`() = runBlockingTest {
        val state = createInitialState()

        val dayToSelect = OffsetDateTime.MAX

        reducer.testReduceState(state, CalendarDatePickerAction.DaySelected(dayToSelect)) {
            it.selectedDate shouldBe dayToSelect
        }
    }

    @Test
    fun `Produces no effects`() = runBlockingTest {
        val state = createInitialState()

        val dayToSelect = OffsetDateTime.MAX

        reducer.testReduceNoEffects(state, CalendarDatePickerAction.DaySelected(dayToSelect))
    }
}