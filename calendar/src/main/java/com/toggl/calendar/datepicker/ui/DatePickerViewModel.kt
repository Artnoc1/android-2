package com.toggl.calendar.datepicker.ui

import com.toggl.common.extensions.toBeginningOfTheDay
import com.toggl.common.extensions.toEndOfTheDay
import java.time.DayOfWeek
import java.time.OffsetDateTime

data class DatePickerViewModel(
    val weekHeaderLabels: List<DayOfWeek>,
    val weeks: List<Week>,
    val selectedWeek: Int,
    val selectedDay: OffsetDateTime
)

data class Week(
    val dates: List<VisibleDate>
)

data class VisibleDate(
    val dateLabel: String,
    val isSelectable: Boolean,
    val isToday: Boolean,
    val date: OffsetDateTime
)

val Week.start: OffsetDateTime
    get() = this.dates.first().date.toBeginningOfTheDay()

val Week.end: OffsetDateTime
    get() = this.dates.last().date.toEndOfTheDay()