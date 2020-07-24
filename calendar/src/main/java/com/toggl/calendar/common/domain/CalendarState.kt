package com.toggl.calendar.common.domain

import com.toggl.architecture.Loadable
import com.toggl.common.feature.navigation.BackStack
import com.toggl.common.feature.navigation.BackStackAwareState
import com.toggl.common.feature.navigation.pop
import com.toggl.common.feature.services.calendar.Calendar
import com.toggl.common.feature.services.calendar.CalendarEvent
import com.toggl.models.domain.Client
import com.toggl.models.domain.Project
import com.toggl.models.domain.TimeEntry
import com.toggl.models.domain.User
import java.time.OffsetDateTime

data class CalendarState(
    val user: Loadable<User>,
    val timeEntries: Map<Long, TimeEntry>,
    val projects: Map<Long, Project>,
    val clients: Map<Long, Client>,
    val backStack: BackStack,
    val calendarEvents: Map<String, CalendarEvent>,
    val localState: LocalState
) : BackStackAwareState<CalendarState> {
    data class LocalState internal constructor(
        internal val selectedDate: OffsetDateTime,
        internal val calendarEvents: Map<String, CalendarEvent>,
        internal val calendars: List<Calendar>,
        internal val availableDates: List<OffsetDateTime>,
        internal val visibleDates: List<OffsetDateTime>
    ) {
        constructor() : this(OffsetDateTime.now(), mapOf(), listOf(), listOf(), listOf())
    }

    override fun popBackStack(): CalendarState =
        copy(backStack = backStack.pop())

    companion object
}