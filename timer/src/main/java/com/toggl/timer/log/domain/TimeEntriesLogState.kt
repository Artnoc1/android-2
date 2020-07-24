package com.toggl.timer.log.domain

import com.toggl.common.feature.navigation.BackStack
import com.toggl.models.domain.Client
import com.toggl.models.domain.Project
import com.toggl.models.domain.TimeEntry
import com.toggl.timer.common.domain.TimerState

data class TimeEntriesLogState(
    val timeEntries: Map<Long, TimeEntry>,
    val projects: Map<Long, Project>,
    val clients: Map<Long, Client>,
    val backStack: BackStack,
    val expandedGroupIds: Set<Long>,
    val entriesPendingDeletion: Set<Long>,
    val shouldGroup: Boolean = true
) {
    companion object {
        fun fromTimerState(timerState: TimerState) =
            TimeEntriesLogState(
                timerState.timeEntries,
                timerState.projects,
                timerState.clients,
                timerState.backStack,
                timerState.localState.expandedGroupIds,
                timerState.localState.entriesPendingDeletion
            )

        fun toTimerState(timerState: TimerState, timeEntriesLogState: TimeEntriesLogState) =
            timerState.copy(
                timeEntries = timeEntriesLogState.timeEntries,
                backStack = timeEntriesLogState.backStack,
                localState = timerState.localState.copy(
                    expandedGroupIds = timeEntriesLogState.expandedGroupIds,
                    entriesPendingDeletion = timeEntriesLogState.entriesPendingDeletion
                )
            )
    }
}
