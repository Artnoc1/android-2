package com.toggl.calendar.datepicker.domain

import com.toggl.architecture.core.Selector
import com.toggl.calendar.calendarday.domain.CalendarDayState
import com.toggl.calendar.datepicker.ui.CalendarDayHeaderViewModel
import com.toggl.common.extensions.maybePlus
import com.toggl.common.feature.extensions.formatForDisplaying
import com.toggl.common.feature.timeentry.extensions.runningTimeEntryOrNull
import com.toggl.common.feature.timeentry.extensions.totalDuration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class DayHeaderSelector @Inject constructor() : Selector<CalendarDayState, CalendarDayHeaderViewModel> {

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM dd")

    override suspend fun select(state: CalendarDayState): CalendarDayHeaderViewModel {
        val selectedDate = state.selectedDate.toLocalDate()
        fun isOnDate(startTime: OffsetDateTime, endTime: OffsetDateTime?) =
            startTime.toLocalDate() == selectedDate && (endTime == null || endTime.toLocalDate() == selectedDate)

        val timeEntriesOnDay = state.timeEntries
            .filterValues { isOnDate(it.startTime, it.startTime.maybePlus(it.duration)) }
            .values

        val runningTimeEntry = timeEntriesOnDay.runningTimeEntryOrNull()
        val dayLabel = formatter.format(selectedDate)
        val totalDuration = timeEntriesOnDay.totalDuration()

        if (runningTimeEntry == null) {
            return CalendarDayHeaderViewModel.StoppedDay(dayLabel, totalDuration.formatForDisplaying())
        }

        return CalendarDayHeaderViewModel.DayWithRunningTimeEntry(
            dayLabel,
            totalDuration,
            runningTimeEntry.startTime
        )
    }
}