package com.toggl.timer.common.domain

import com.toggl.architecture.core.ActionWrapper
import com.toggl.timer.log.domain.TimeEntriesLogAction
import com.toggl.timer.log.domain.formatForDebug
import com.toggl.timer.project.domain.ProjectAction
import com.toggl.timer.project.domain.formatForDebug
import com.toggl.timer.running.domain.RunningTimeEntryAction
import com.toggl.timer.running.domain.formatForDebug
import com.toggl.timer.startedit.domain.StartEditAction
import com.toggl.timer.startedit.domain.formatForDebug
import com.toggl.timer.suggestions.domain.SuggestionsAction
import com.toggl.timer.suggestions.domain.formatForDebug

sealed class TimerAction {
    data class StartEditTimeEntry(override val action: StartEditAction) : TimerAction(), ActionWrapper<StartEditAction>
    data class TimeEntriesLog(override val action: TimeEntriesLogAction) : TimerAction(), ActionWrapper<TimeEntriesLogAction>
    data class RunningTimeEntry(override val action: RunningTimeEntryAction) : TimerAction(), ActionWrapper<RunningTimeEntryAction>
    data class Project(override val action: ProjectAction) : TimerAction(), ActionWrapper<ProjectAction>
    data class Suggestions(override val action: SuggestionsAction) : TimerAction(), ActionWrapper<SuggestionsAction>

    companion object
}

fun TimerAction.formatForDebug(): String =
    when (this) {
        is TimerAction.StartEditTimeEntry -> action.formatForDebug()
        is TimerAction.TimeEntriesLog -> action.formatForDebug()
        is TimerAction.RunningTimeEntry -> action.formatForDebug()
        is TimerAction.Project -> action.formatForDebug()
        is TimerAction.Suggestions -> action.formatForDebug()
    }
