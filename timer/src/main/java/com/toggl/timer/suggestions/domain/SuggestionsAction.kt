package com.toggl.timer.suggestions.domain

import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.common.feature.timeentry.TimeEntryActionHolder

sealed class SuggestionsAction {
    data class TimeEntryHandling(override val timeEntryAction: TimeEntryAction) : SuggestionsAction(), TimeEntryActionHolder

    companion object
}

fun SuggestionsAction.formatForDebug() =
    when (this) {
        is SuggestionsAction.TimeEntryHandling -> "Time entry action $timeEntryAction"
    }