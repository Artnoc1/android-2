package com.toggl.timer.common.domain

import com.toggl.architecture.Loadable
import com.toggl.common.Constants
import com.toggl.common.feature.navigation.BackStack
import com.toggl.common.feature.navigation.BackStackAwareState
import com.toggl.common.feature.navigation.pop
import com.toggl.common.feature.services.calendar.CalendarEvent
import com.toggl.models.common.AutocompleteSuggestion
import com.toggl.models.domain.Client
import com.toggl.models.domain.Project
import com.toggl.models.domain.Tag
import com.toggl.models.domain.Task
import com.toggl.models.domain.TimeEntry
import com.toggl.models.domain.User
import com.toggl.models.domain.Workspace
import com.toggl.models.validation.HSVColor
import com.toggl.timer.project.domain.ProjectAutocompleteQuery
import com.toggl.timer.startedit.domain.DateTimePickMode
import com.toggl.timer.startedit.domain.TemporalInconsistency
import com.toggl.timer.suggestions.domain.Suggestion

data class TimerState(
    val user: Loadable<User>,
    val tags: Map<Long, Tag>,
    val tasks: Map<Long, Task>,
    val clients: Map<Long, Client>,
    val projects: Map<Long, Project>,
    val workspaces: Map<Long, Workspace>,
    val timeEntries: Map<Long, TimeEntry>,
    val backStack: BackStack,
    val calendarEvents: Map<String, CalendarEvent>,
    val localState: LocalState
) : BackStackAwareState<TimerState> {
    data class LocalState internal constructor(
        internal val expandedGroupIds: Set<Long>,
        internal val entriesPendingDeletion: Set<Long>,
        internal val autocompleteSuggestions: List<AutocompleteSuggestion.StartEditSuggestions>,
        internal val dateTimePickMode: DateTimePickMode,
        internal val temporalInconsistency: TemporalInconsistency,
        internal val cursorPosition: Int,
        internal val customColor: HSVColor,
        internal val maxNumberOfSuggestions: Int,
        internal val suggestions: List<Suggestion>,
        internal val projectAutocompleteQuery: ProjectAutocompleteQuery,
        internal val projectAutoCompleteSuggestions: List<AutocompleteSuggestion.ProjectSuggestions>
    ) {
        constructor() : this(
            expandedGroupIds = setOf(),
            entriesPendingDeletion = setOf(),
            autocompleteSuggestions = emptyList(),
            dateTimePickMode = DateTimePickMode.None,
            temporalInconsistency = TemporalInconsistency.None,
            cursorPosition = 0,
            customColor = HSVColor.defaultCustomColor,
            maxNumberOfSuggestions = Constants.Suggestions.maxNumberOfSuggestions,
            suggestions = emptyList(),
            projectAutocompleteQuery = ProjectAutocompleteQuery.None,
            projectAutoCompleteSuggestions = emptyList()
        )
    }

    override fun popBackStack(): TimerState =
        copy(backStack = backStack.pop())

    companion object
}