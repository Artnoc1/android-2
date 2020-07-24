package com.toggl.timer.suggestions.domain

import com.toggl.architecture.Loadable
import com.toggl.common.feature.services.calendar.CalendarEvent
import com.toggl.models.domain.Client
import com.toggl.models.domain.Project
import com.toggl.models.domain.TimeEntry
import com.toggl.models.domain.User
import com.toggl.timer.common.domain.TimerState

data class SuggestionsState(
    val suggestions: List<Suggestion>,
    val user: User,
    val projects: Map<Long, Project>,
    val clients: Map<Long, Client>,
    val timeEntries: Map<Long, TimeEntry>,
    val calendarEvents: Map<String, CalendarEvent>,
    val maxNumberOfSuggestions: Int
) {
    companion object {
        fun fromTimerState(timerState: TimerState): SuggestionsState? {

            val user = timerState.user as? Loadable.Loaded<User> ?: return null

            return SuggestionsState(
                user = user.value,
                projects = timerState.projects,
                clients = timerState.clients,
                timeEntries = timerState.timeEntries,
                maxNumberOfSuggestions = timerState.localState.maxNumberOfSuggestions,
                calendarEvents = timerState.calendarEvents,
                suggestions = timerState.localState.suggestions
            )
        }

        fun toTimerState(timerState: TimerState, suggestionsState: SuggestionsState?) =
            suggestionsState?.let {
                timerState.copy(
                    timeEntries = suggestionsState.timeEntries,
                    projects = suggestionsState.projects,
                    clients = suggestionsState.clients,
                    calendarEvents = suggestionsState.calendarEvents
                )
            } ?: timerState
    }
}