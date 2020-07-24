package com.toggl.calendar.contextualmenu.domain

import com.toggl.architecture.Loadable
import com.toggl.calendar.common.domain.CalendarState
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.common.feature.navigation.BackStack
import com.toggl.common.feature.navigation.BackStackAwareState
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.getRouteParam
import com.toggl.common.feature.navigation.pop
import com.toggl.common.feature.navigation.setRouteParam
import com.toggl.common.feature.services.calendar.Calendar
import com.toggl.models.domain.Client
import com.toggl.models.domain.Project
import com.toggl.models.domain.TimeEntry
import com.toggl.models.domain.User

data class ContextualMenuState(
    val user: User,
    val timeEntries: Map<Long, TimeEntry>,
    val projects: Map<Long, Project>,
    val clients: Map<Long, Client>,
    val calendars: List<Calendar>,
    val backStack: BackStack,
    val selectedItem: SelectedCalendarItem
) : BackStackAwareState<ContextualMenuState> {
    companion object {
        fun fromCalendarState(calendarState: CalendarState): ContextualMenuState? {
            val user = calendarState.user as? Loadable.Loaded<User> ?: return null
            val selectedItem = calendarState.backStack.getRouteParam<SelectedCalendarItem>() ?: return null

            return ContextualMenuState(
                user.value,
                calendarState.timeEntries,
                calendarState.projects,
                calendarState.clients,
                calendarState.localState.calendars,
                calendarState.backStack,
                selectedItem
            )
        }

        fun toCalendarState(calendarState: CalendarState, contextualMenuState: ContextualMenuState?) =
            contextualMenuState?.let {
                calendarState.copy(
                    timeEntries = contextualMenuState.timeEntries,
                    projects = contextualMenuState.projects,
                    clients = contextualMenuState.clients,
                    backStack = contextualMenuState.backStack.setRouteParam {
                        Route.ContextualMenu((contextualMenuState.selectedItem))
                    }
                )
            } ?: calendarState
    }

    override fun popBackStack(): ContextualMenuState =
        copy(backStack = backStack.pop())
}