package com.toggl.models.domain

import java.time.Duration
import java.time.OffsetDateTime

data class EditableTimeEntry(
    val ids: List<Long> = listOf(),
    val workspaceId: Long,
    val description: String = "",
    val startTime: OffsetDateTime? = null,
    val duration: Duration? = null,
    val billable: Boolean = false,
    val projectId: Long? = null,
    val taskId: Long? = null,
    val tagIds: List<Long> = listOf()
) {
    companion object {
        fun empty(workspaceId: Long) = EditableTimeEntry(workspaceId = workspaceId)

        fun stopped(workspaceId: Long, startTime: OffsetDateTime, duration: Duration) =
            EditableTimeEntry(
                workspaceId = workspaceId,
                startTime = startTime,
                duration = duration
            )

        fun fromSingle(timeEntry: TimeEntry) =
            EditableTimeEntry(
                ids = listOf(timeEntry.id),
                workspaceId = timeEntry.workspaceId,
                description = timeEntry.description,
                startTime = timeEntry.startTime,
                duration = timeEntry.duration,
                billable = timeEntry.billable,
                projectId = timeEntry.projectId,
                tagIds = timeEntry.tagIds,
                taskId = timeEntry.taskId
            )

        fun fromGroup(timeEntries: Collection<TimeEntry>): EditableTimeEntry {
            val sample = timeEntries.first()
            return EditableTimeEntry(
                ids = timeEntries.map { it.id },
                workspaceId = sample.workspaceId,
                description = sample.description,
                duration = timeEntries.fold(Duration.ZERO) { totalTime, timeEntry -> totalTime + timeEntry.duration },
                billable = sample.billable,
                projectId = sample.projectId,
                tagIds = sample.tagIds,
                taskId = sample.taskId
            )
        }
    }
}
