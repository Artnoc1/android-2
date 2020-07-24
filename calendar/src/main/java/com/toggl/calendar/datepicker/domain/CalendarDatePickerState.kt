package com.toggl.calendar.datepicker.domain

import com.toggl.calendar.common.domain.CalendarState
import java.time.OffsetDateTime

data class CalendarDatePickerState(
    val selectedDate: OffsetDateTime
) {
    companion object {
        fun fromCalendarState(calendarState: CalendarState) =
            CalendarDatePickerState(
                selectedDate = calendarState.localState.selectedDate
            )

        fun toCalendarState(calendarState: CalendarState, datePickerState: CalendarDatePickerState) =
            calendarState.copy(
                localState = calendarState.localState.copy(
                    selectedDate = datePickerState.selectedDate
                )
            )
    }
}