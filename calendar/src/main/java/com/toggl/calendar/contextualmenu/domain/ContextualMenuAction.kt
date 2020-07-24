package com.toggl.calendar.contextualmenu.domain

import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.common.feature.timeentry.TimeEntryActionHolder

sealed class ContextualMenuAction {
    object CloseButtonTapped : ContextualMenuAction()
    object DiscardButtonTapped : ContextualMenuAction()
    object DialogDismissed : ContextualMenuAction()
    object SaveButtonTapped : ContextualMenuAction()
    object EditButtonTapped : ContextualMenuAction()
    object DeleteButtonTapped : ContextualMenuAction()
    object ContinueButtonTapped : ContextualMenuAction()
    object StopButtonTapped : ContextualMenuAction()
    object StartFromEventButtonTapped : ContextualMenuAction()
    object CopyAsTimeEntryButtonTapped : ContextualMenuAction()
    object Close : ContextualMenuAction()

    data class TimeEntryHandling(override val timeEntryAction: TimeEntryAction) : ContextualMenuAction(), TimeEntryActionHolder

    companion object
}

fun ContextualMenuAction.formatForDebug() =
    when (this) {
        ContextualMenuAction.CloseButtonTapped -> "Contextual menu close button pressed"
        ContextualMenuAction.DiscardButtonTapped -> "Contextual menu discarded"
        ContextualMenuAction.DialogDismissed -> "Contextual menu dialog dismissed"
        ContextualMenuAction.SaveButtonTapped -> "Save button tapped"
        ContextualMenuAction.EditButtonTapped -> "Edit button tapped"
        ContextualMenuAction.DeleteButtonTapped -> "Delete button tapped"
        ContextualMenuAction.ContinueButtonTapped -> "Continue button tapped"
        ContextualMenuAction.StopButtonTapped -> "Stop button tapped"
        ContextualMenuAction.StartFromEventButtonTapped -> "Start button tapped"
        ContextualMenuAction.CopyAsTimeEntryButtonTapped -> "Copy button tapped"
        is ContextualMenuAction.TimeEntryHandling -> "Time entry action $timeEntryAction"
        ContextualMenuAction.Close -> "Contextual menu closed"
    }