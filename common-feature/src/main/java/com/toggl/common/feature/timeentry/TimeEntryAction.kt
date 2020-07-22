package com.toggl.common.feature.timeentry

import com.toggl.models.domain.TimeEntry
import com.toggl.repository.dto.CreateTimeEntryDTO
import com.toggl.repository.dto.StartTimeEntryDTO

sealed class TimeEntryAction {
    data class DeleteTimeEntry(val id: Long) : TimeEntryAction()
    data class ContinueTimeEntry(val id: Long) : TimeEntryAction()
    object StopRunningTimeEntry : TimeEntryAction()
    data class CreateTimeEntry(val createTimeEntryDTO: CreateTimeEntryDTO) : TimeEntryAction()
    data class StartTimeEntry(val startTimeEntryDTO: StartTimeEntryDTO) : TimeEntryAction()
    data class EditTimeEntry(val timeEntry: TimeEntry) : TimeEntryAction()

    object TimeEntriesUpdated : TimeEntryAction()

    companion object {
        fun <Action> fromTimeEntryActionHolder(action: Action): TimeEntryAction? =
            (action as? TimeEntryActionHolder)?.timeEntryAction
    }
}

fun TimeEntryAction.formatForDebug() =
    when (this) {
        is TimeEntryAction.DeleteTimeEntry -> "Delete time entry $id"
        is TimeEntryAction.ContinueTimeEntry -> "Continue time entry $id"
        is TimeEntryAction.StopRunningTimeEntry -> "Stop running time entry"
        is TimeEntryAction.CreateTimeEntry -> "Create time entry $createTimeEntryDTO"
        is TimeEntryAction.StartTimeEntry -> "Start time entry $startTimeEntryDTO"
        is TimeEntryAction.EditTimeEntry -> "Edit time entry $timeEntry"
        TimeEntryAction.TimeEntriesUpdated -> "Time Entries updated"
    }

interface TimeEntryActionHolder {
    val timeEntryAction: TimeEntryAction
}
