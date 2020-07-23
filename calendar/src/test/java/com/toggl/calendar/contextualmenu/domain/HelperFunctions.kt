package com.toggl.calendar.contextualmenu.domain

import com.toggl.calendar.common.createCalendarEvent
import com.toggl.calendar.common.validUser
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.services.calendar.Calendar
import com.toggl.models.domain.Client
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.models.domain.Project
import com.toggl.models.domain.TimeEntry

fun createInitialState(
    timeEntries: List<TimeEntry> = emptyList(),
    projects: List<Project> = emptyList(),
    clients: List<Client> = emptyList(),
    calendars: List<Calendar> = emptyList(),
    editableTimeEntry: EditableTimeEntry? = null,
    selectedItem: SelectedCalendarItem =
        if (editableTimeEntry != null) {
            SelectedCalendarItem.SelectedTimeEntry(editableTimeEntry)
        } else {
            SelectedCalendarItem.SelectedCalendarEvent(calendarEvent = createCalendarEvent())
        }
) = ContextualMenuState(
    validUser,
    timeEntries.associateBy { it.id },
    selectedItem = selectedItem,
    backStack = listOf(Route.ContextualMenu(selectedItem)),
    projects = projects.associateBy { it.id },
    clients = clients.associateBy { it.id },
    calendars = calendars
)