package com.toggl.timer.project.domain

import com.toggl.common.feature.navigation.BackStack
import com.toggl.common.feature.navigation.BackStackAwareState
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.getRouteParam
import com.toggl.common.feature.navigation.pop
import com.toggl.common.feature.navigation.setRouteParam
import com.toggl.models.common.AutocompleteSuggestion
import com.toggl.models.domain.Client
import com.toggl.models.domain.EditableProject
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.models.domain.Project
import com.toggl.models.domain.Workspace
import com.toggl.models.validation.HSVColor
import com.toggl.timer.common.domain.TimerState

data class ProjectState(
    val editableProject: EditableProject,
    val editableTimeEntry: EditableTimeEntry,
    val projects: Map<Long, Project>,
    val workspaces: Map<Long, Workspace>,
    val clients: Map<Long, Client>,
    val cursorPosition: Int,
    val customColor: HSVColor,
    val autocompleteQuery: ProjectAutocompleteQuery,
    val autocompleteSuggestions: List<AutocompleteSuggestion.ProjectSuggestions>,
    val backStack: BackStack
) : BackStackAwareState<ProjectState> {
    companion object {

        fun fromTimerState(timerState: TimerState): ProjectState? {
            val editableTimeEntry = timerState.backStack.getRouteParam<EditableTimeEntry>() ?: return null
            val editableProject = timerState.backStack.getRouteParam<EditableProject>() ?: return null

            return ProjectState(
                backStack = timerState.backStack,
                editableProject = editableProject,
                editableTimeEntry = editableTimeEntry,
                projects = timerState.projects,
                workspaces = timerState.workspaces,
                clients = timerState.clients,
                customColor = timerState.localState.customColor,
                cursorPosition = timerState.localState.cursorPosition,
                autocompleteQuery = timerState.localState.projectAutocompleteQuery,
                autocompleteSuggestions = timerState.localState.projectAutoCompleteSuggestions
            )
        }

        fun toTimerState(timerState: TimerState, projectState: ProjectState?) =
            projectState?.let {
                timerState.copy(
                    projects = projectState.projects,
                    clients = projectState.clients,
                    localState = timerState.localState.copy(
                        customColor = projectState.customColor,
                        projectAutocompleteQuery = projectState.autocompleteQuery,
                        projectAutoCompleteSuggestions = projectState.autocompleteSuggestions
                    ),
                    backStack = projectState.backStack
                        .setRouteParam { Route.StartEdit(projectState.editableTimeEntry) }
                        .setRouteParam { Route.Project(projectState.editableProject) }
                )
            } ?: timerState
    }

    override fun popBackStack() =
        copy(backStack = backStack.pop())
}