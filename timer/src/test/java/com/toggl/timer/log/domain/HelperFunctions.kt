package com.toggl.timer.log.domain

import com.toggl.common.feature.navigation.getRouteParam
import com.toggl.models.domain.Client
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.models.domain.Project
import com.toggl.models.domain.TimeEntry

fun createInitialState(
    timeEntries: List<TimeEntry> = listOf(),
    projects: List<Project> = listOf(),
    clients: List<Client> = listOf(),
    expandedGroupIds: Set<Long> = setOf(),
    entriesPendingDeletion: Set<Long> = setOf()
) =
    TimeEntriesLogState(
        timeEntries = timeEntries.associateBy { it.id },
        projects = projects.associateBy { it.id },
        clients = clients.associateBy { it.id },
        backStack = emptyList(),
        expandedGroupIds = expandedGroupIds,
        entriesPendingDeletion = entriesPendingDeletion
    )

val TimeEntriesLogState.editableTimeEntry: EditableTimeEntry?
    get() = backStack.getRouteParam<EditableTimeEntry>()