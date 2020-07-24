package com.toggl.timer.log.domain

import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.common.feature.timeentry.TimeEntryActionHolder
import com.toggl.models.common.SwipeDirection

sealed class TimeEntriesLogAction {
    data class ContinueButtonTapped(val id: Long) : TimeEntriesLogAction()
    data class TimeEntryTapped(val id: Long) : TimeEntriesLogAction()
    data class TimeEntrySwiped(val id: Long, val direction: SwipeDirection) : TimeEntriesLogAction()
    data class TimeEntryGroupTapped(val ids: List<Long>) : TimeEntriesLogAction()
    data class TimeEntryGroupSwiped(val ids: List<Long>, val direction: SwipeDirection) : TimeEntriesLogAction()
    data class ToggleTimeEntryGroupTapped(val groupId: Long) : TimeEntriesLogAction()
    data class CommitDeletion(val ids: List<Long>) : TimeEntriesLogAction()
    object UndoButtonTapped : TimeEntriesLogAction()
    object OpenSettingsButtonTapped : TimeEntriesLogAction()

    data class TimeEntryHandling(override val timeEntryAction: TimeEntryAction) : TimeEntriesLogAction(), TimeEntryActionHolder
    companion object
}

fun TimeEntriesLogAction.formatForDebug() =
    when (this) {
        is TimeEntriesLogAction.ContinueButtonTapped -> "Continue time entry button tapped for id $id"
        is TimeEntriesLogAction.TimeEntryTapped -> "Tapped time entry with id $id"
        is TimeEntriesLogAction.TimeEntrySwiped -> "Time entry with id $id swiped to the $direction "
        is TimeEntriesLogAction.TimeEntryGroupTapped -> "Tapped group containing time entries $ids"
        is TimeEntriesLogAction.TimeEntryGroupSwiped -> "Group containing time entries $ids swiped to the $direction"
        is TimeEntriesLogAction.ToggleTimeEntryGroupTapped -> "Time entry group with id $groupId toggled"
        is TimeEntriesLogAction.CommitDeletion -> "Committed deletion of $ids"
        is TimeEntriesLogAction.UndoButtonTapped -> "Undo button tapped"
        is TimeEntriesLogAction.TimeEntryHandling -> "Time entry action $timeEntryAction"
        TimeEntriesLogAction.OpenSettingsButtonTapped -> "Open settings button tapped"
    }
