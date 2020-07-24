package com.toggl.calendar.datepicker.domain

import com.toggl.models.common.SwipeDirection
import java.time.OffsetDateTime

sealed class CalendarDatePickerAction {
    data class DaySelected(val day: OffsetDateTime) : CalendarDatePickerAction()
    data class DaySwiped(val direction: SwipeDirection) : CalendarDatePickerAction()
    data class WeekStripeSwiped(val direction: SwipeDirection) : CalendarDatePickerAction()
}

fun CalendarDatePickerAction.formatForDebug() =
    when (this) {
        is CalendarDatePickerAction.DaySelected -> "Calendar date picker day selected: $day"
        is CalendarDatePickerAction.DaySwiped -> "Calendar date picker day swiped: $direction"
        is CalendarDatePickerAction.WeekStripeSwiped -> "Week stripe swiped $direction"
    }
