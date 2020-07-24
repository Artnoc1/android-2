package com.toggl.calendar.datepicker.domain

import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.core.Selector
import com.toggl.calendar.common.domain.CalendarConstants.numberOfDaysToShow
import com.toggl.calendar.datepicker.ui.DatePickerViewModel
import com.toggl.calendar.datepicker.ui.VisibleDate
import com.toggl.calendar.datepicker.ui.Week
import com.toggl.calendar.datepicker.ui.end
import com.toggl.calendar.datepicker.ui.start
import com.toggl.common.extensions.beginningOfWeek
import com.toggl.common.extensions.toList
import com.toggl.common.services.time.TimeService
import com.toggl.repository.interfaces.SettingsRepository
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class DatePickerSelector @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val timeService: TimeService,
    private val dispatcherProvider: DispatcherProvider
) : Selector<CalendarDatePickerState, DatePickerViewModel> {
    private val daysInAWeek = 7
    private val dateLabelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd")

    override suspend fun select(state: CalendarDatePickerState): DatePickerViewModel = withContext(dispatcherProvider.io) {
        val today = timeService.now()
        val userPreferences = settingsRepository.loadUserPreferences()
        val userFirstDayOfTheWeek = userPreferences.firstDayOfTheWeek
        val userFirstDayOfTheWeekValue = userFirstDayOfTheWeek.value - 1
        val weekLabels = (0 until daysInAWeek)
            .map { (DayOfWeek.of((it + userFirstDayOfTheWeekValue) % daysInAWeek + 1)) }

        val firstAvailableDate = today.minusDays(numberOfDaysToShow - 1)
        val firstShownDate = firstAvailableDate.beginningOfWeek(userFirstDayOfTheWeek)
        val lastShownDate = today.beginningOfWeek(userFirstDayOfTheWeek).plusDays(6)

        val visibleDates = (firstShownDate..lastShownDate).toList()
        val availableDates = firstAvailableDate..today
        val selectedDate = state.selectedDate

        val allDates = visibleDates.map {
            VisibleDate(
                dateLabelFormatter.format(it),
                it in availableDates,
                it.dayOfYear == today.dayOfYear,
                it
            )
        }
        val weeks = if (allDates.isEmpty()) {
            emptyList()
        } else {
            (allDates.indices step daysInAWeek)
                .map { Week(allDates.subList(it, it + daysInAWeek)) }
        }

        val selectedWeek = weeks.indexOfFirst { week -> selectedDate in week.start..week.end }
        DatePickerViewModel(
            weekLabels,
            weeks,
            selectedWeek,
            selectedDate
        )
    }
}
