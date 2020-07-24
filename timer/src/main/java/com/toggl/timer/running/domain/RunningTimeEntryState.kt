package com.toggl.timer.running.domain

import com.toggl.architecture.Loadable
import com.toggl.common.feature.navigation.BackStack
import com.toggl.models.domain.TimeEntry
import com.toggl.models.domain.User
import com.toggl.timer.common.domain.TimerState

data class RunningTimeEntryState(
    val user: User,
    val backStack: BackStack,
    val timeEntries: Map<Long, TimeEntry>
) {
    companion object {
        fun fromTimerState(timerState: TimerState): RunningTimeEntryState? {
            val user = timerState.user as? Loadable.Loaded<User> ?: return null
            return RunningTimeEntryState(
                user = user.value,
                backStack = timerState.backStack,
                timeEntries = timerState.timeEntries
            )
        }

        fun toTimerState(timerState: TimerState, runningTimeEntryState: RunningTimeEntryState?) =
            runningTimeEntryState?.let {
                timerState.copy(
                    backStack = runningTimeEntryState.backStack,
                    timeEntries = runningTimeEntryState.timeEntries
                )
            } ?: timerState
    }
}