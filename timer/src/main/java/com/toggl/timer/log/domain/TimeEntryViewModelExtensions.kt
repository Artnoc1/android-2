package com.toggl.timer.log.domain

import com.toggl.common.feature.domain.ProjectViewModel
import com.toggl.common.feature.timeentry.extensions.totalDuration
import com.toggl.models.domain.Client
import com.toggl.models.domain.Project
import com.toggl.models.domain.TimeEntry

fun TimeEntry.toFlatTimeEntryViewModel(projects: Map<Long, Project>, clients: Map<Long, Client>) =
    FlatTimeEntryViewModel(
        id = id,
        description = description,
        startTime = startTime,
        duration = duration
            ?: throw IllegalStateException("Running time entries are not supported"),
        project = projects.getProjectViewModelFor(this, clients),
        billable = billable,
        hasTags = tagIds.isNotEmpty()
    )

fun List<TimeEntry>.toTimeEntryGroupViewModel(
    groupId: Long,
    isExpanded: Boolean,
    projects: Map<Long, Project>,
    clients: Map<Long, Client>
) =
    TimeEntryGroupViewModel(
        groupId = groupId,
        timeEntryIds = map(TimeEntry::id),
        isExpanded = isExpanded,
        description = first().description,
        duration = totalDuration(),
        project = projects.getProjectViewModelFor(this.first(), clients),
        billable = first().billable,
        hasTags = first().tagIds.isNotEmpty()
    )

fun Project.toProjectViewModel(clients: Map<Long, Client>) =
    ProjectViewModel(id, name, color, clients[clientId]?.name)

fun Map<Long, Project>.getProjectViewModelFor(timeEntry: TimeEntry, clients: Map<Long, Client>): ProjectViewModel? {
    val projectId = timeEntry.projectId
    return if (projectId == null) null
    else this[projectId]?.toProjectViewModel(clients)
}