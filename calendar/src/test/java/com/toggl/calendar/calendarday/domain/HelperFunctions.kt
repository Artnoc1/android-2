package com.toggl.calendar.calendarday.domain

import com.toggl.calendar.common.validUser
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.backStackOf
import com.toggl.common.feature.services.calendar.Calendar
import com.toggl.common.feature.services.calendar.CalendarEvent
import com.toggl.models.domain.Project
import com.toggl.models.domain.TimeEntry
import java.time.OffsetDateTime

fun createInitialState(
    timeEntries: List<TimeEntry> = listOf(),
    calendarEvents: List<CalendarEvent> = listOf(),
    projects: List<Project> = listOf(),
    selectedItem: SelectedCalendarItem? = null,
    date: OffsetDateTime = OffsetDateTime.now(),
    calendars: List<Calendar> = listOf()
) = CalendarDayState(
    validUser,
    timeEntries.associateBy { it.id },
    projects.associateBy { it.id },
    if (selectedItem == null) emptyList() else backStackOf(Route.ContextualMenu(selectedItem)),
    calendarEvents.associateBy { it.id },
    date,
    calendars
)