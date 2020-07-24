package com.toggl.timer.log.domain

import com.toggl.architecture.core.Selector
import com.toggl.common.feature.timeentry.extensions.totalDuration
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.TimeEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val timeEntriesLogHeaderTimeFormat = "eee, dd MMM"

class TimeEntriesLogSelector(
    private val todayString: String,
    private val yesterdayString: String,
    private val timeService: TimeService
) : Selector<TimeEntriesLogState, List<TimeEntryViewModel>> {
    override suspend fun select(state: TimeEntriesLogState): List<TimeEntryViewModel> {
        val timeEntries = state.timeEntries
        val projects = state.projects
        val clients = state.clients
        val shouldGroup = true
        val expandedGroupIds = state.expandedGroupIds
        val entriesPendingDeletion = state.entriesPendingDeletion
        val today = timeService.now().toLocalDate()
        val yesterday = today.minusDays(1)

        fun List<TimeEntry>.mapToGroups(): List<List<TimeEntry>> =
            this.groupBy(TimeEntry::similarityHashCode)
                .map { (_, timeEntries) -> timeEntries }

        suspend fun SequenceScope<TimeEntryViewModel>.yieldDayHeader(
            groupDate: LocalDate,
            timeEntries: List<TimeEntry>
        ) {
            yield(
                DayHeaderViewModel(
                    dayTitle = when (groupDate) {
                        today -> todayString
                        yesterday -> yesterdayString
                        else -> groupDate.format(
                            DateTimeFormatter.ofPattern(
                                timeEntriesLogHeaderTimeFormat
                            )
                        )
                    },
                    totalDuration = timeEntries.totalDuration()
                )
            )
        }

        suspend fun SequenceScope<TimeEntryViewModel>.yieldFlatTimeEntry(timeEntry: TimeEntry) =
            yield(timeEntry.toFlatTimeEntryViewModel(projects, clients))

        suspend fun SequenceScope<TimeEntryViewModel>.yieldTimeEntryGroup(isExpanded: Boolean, timeEntries: List<TimeEntry>) =
            yield(
                timeEntries.toTimeEntryGroupViewModel(
                    timeEntries.first().similarityHashCode(),
                    isExpanded,
                    projects,
                    clients
                )
            )

        return timeEntries.values
            .filter { it.duration != null && !it.isDeleted && !entriesPendingDeletion.contains(it.id) }
            .sortedByDescending { it.startTime }
            .groupBy { timeEntry -> timeEntry.startTime.toLocalDate() }
            .flatMap { (groupDate, timeEntries) ->
                sequence<TimeEntryViewModel> {
                    yieldDayHeader(groupDate, timeEntries)

                    if (shouldGroup) {
                        val timeEntryGroups = timeEntries.mapToGroups()
                        for (timeEntryGroup in timeEntryGroups) {
                            if (timeEntryGroup.size == 1) {
                                yieldFlatTimeEntry(timeEntryGroup.first())
                            } else {
                                val groupId = timeEntryGroup.first().similarityHashCode()
                                if (expandedGroupIds.contains(groupId)) {
                                    yieldTimeEntryGroup(true, timeEntryGroup)
                                    for (timeEntry in timeEntryGroup) {
                                        yieldFlatTimeEntry(timeEntry)
                                    }
                                } else {
                                    yieldTimeEntryGroup(false, timeEntryGroup)
                                }
                            }
                        }
                    } else {
                        for (timeEntry in timeEntries) {
                            yieldFlatTimeEntry(timeEntry)
                        }
                    }
                }.toList()
            }
    }
}

fun TimeEntry.similarityHashCode(): Long {
    var result = description.hashCode()
    result = 31 * result + billable.hashCode()
    result = 31 * result + startTime.dayOfYear.hashCode()
    result = 31 * result + (projectId?.hashCode() ?: 0)
    result = 31 * result + (taskId?.hashCode() ?: 0)
    result = 31 * result + tagIds.hashCode()
    return result.toLong()
}