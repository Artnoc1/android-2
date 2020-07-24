package com.toggl.calendar.datepicker.domain

import com.toggl.architecture.core.Effect
import com.toggl.architecture.core.MutableValue
import com.toggl.architecture.core.Reducer
import com.toggl.architecture.extensions.noEffect
import com.toggl.calendar.common.domain.CalendarConstants.numberOfDaysToShow
import com.toggl.common.Constants
import com.toggl.common.extensions.toBeginningOfTheDay
import com.toggl.common.extensions.toEndOfTheDay
import com.toggl.common.feature.extensions.mutateWithoutEffects
import com.toggl.common.services.time.TimeService
import com.toggl.models.common.SwipeDirection
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarDatePickerReducer @Inject constructor(
    private val timeService: TimeService
) : Reducer<CalendarDatePickerState, CalendarDatePickerAction> {

    override fun reduce(
        state: MutableValue<CalendarDatePickerState>,
        action: CalendarDatePickerAction
    ): List<Effect<CalendarDatePickerAction>> =
        when (action) {
            is CalendarDatePickerAction.DaySelected -> state.mutateWithoutEffects {
                copy(selectedDate = action.day)
            }
            is CalendarDatePickerAction.DaySwiped -> {
                val boundaries = calendarBoundaries()
                val currentDate = state().selectedDate
                val newDate = if (action.direction == SwipeDirection.Left) currentDate.minusDays(1) else currentDate.plusDays(1)

                if (newDate in boundaries)
                    state.mutateWithoutEffects {
                        copy(selectedDate = newDate)
                    }
                else noEffect()
            }
            is CalendarDatePickerAction.WeekStripeSwiped -> state.shiftSelectedDateByWeek(action.direction)
        }

    private fun MutableValue<CalendarDatePickerState>.shiftSelectedDateByWeek(
        direction: SwipeDirection
    ): List<Effect<CalendarDatePickerAction>> {
        return mutateWithoutEffects {
            val shiftBy = when (direction) {
                SwipeDirection.Left -> -Constants.Calendar.datePickerRowItemsCount
                SwipeDirection.Right -> Constants.Calendar.datePickerRowItemsCount
            }
            val boundaries = calendarBoundaries()

            val shiftedDate = selectedDate.plusDays(shiftBy)
            if (shiftedDate in boundaries) {
                copy(selectedDate = shiftedDate)
            } else {
                copy(
                    selectedDate = when (direction) {
                        SwipeDirection.Left -> boundaries.start
                        SwipeDirection.Right -> boundaries.endInclusive
                    }
                )
            }
        }
    }

    private fun OffsetDateTime.pastLimit() = this.minusDays(numberOfDaysToShow - 1).toBeginningOfTheDay()
    private fun calendarBoundaries(): ClosedRange<OffsetDateTime> {
        val today = timeService.now().toEndOfTheDay()
        return today.pastLimit()..today
    }
}
