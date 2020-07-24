package com.toggl.timer.running.domain

import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.common.feature.timeentry.TimeEntryActionHolder

sealed class RunningTimeEntryAction {
    object StartButtonTapped : RunningTimeEntryAction()
    object StopButtonTapped : RunningTimeEntryAction()
    object CardTapped : RunningTimeEntryAction()
    data class TimeEntryHandling(override val timeEntryAction: TimeEntryAction) : RunningTimeEntryAction(), TimeEntryActionHolder

    companion object
}

fun RunningTimeEntryAction.formatForDebug() =
    when (this) {
        RunningTimeEntryAction.StartButtonTapped -> "Start time entry button tapped"
        RunningTimeEntryAction.StopButtonTapped -> "Stop time entry button tapped"
        RunningTimeEntryAction.CardTapped -> "Running time entry card tapped"
        is RunningTimeEntryAction.TimeEntryHandling -> "Time entry action $timeEntryAction"
    }