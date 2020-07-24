package com.toggl.calendar.datepicker.ui

import java.time.Duration
import java.time.OffsetDateTime

sealed class CalendarDayHeaderViewModel {
    abstract val dayLabel: String

    data class StoppedDay(
        override val dayLabel: String,
        val hoursSumLabel: String
    ) : CalendarDayHeaderViewModel()

    data class DayWithRunningTimeEntry(
        override val dayLabel: String,
        val durationWithoutRunningTimeEntry: Duration,
        val runningTimeEntryStartTime: OffsetDateTime
    ) : CalendarDayHeaderViewModel()
}