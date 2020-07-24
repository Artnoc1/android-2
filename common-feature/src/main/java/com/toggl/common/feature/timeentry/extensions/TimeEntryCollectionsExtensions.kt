package com.toggl.common.feature.timeentry.extensions

import com.toggl.models.domain.TimeEntry
import java.time.Duration

fun Map<Long, TimeEntry>.replaceTimeEntryWithId(id: Long, timeEntryToReplace: TimeEntry): Map<Long, TimeEntry> =
    toMutableMap()
        .also { it[id] = timeEntryToReplace }
        .toMap()

fun Map<Long, TimeEntry>.runningTimeEntryOrNull() =
    this.values.runningTimeEntryOrNull()

fun Collection<TimeEntry>.runningTimeEntryOrNull() =
    firstOrNull { it.duration == null }

fun Collection<TimeEntry>.totalDuration(): Duration =
    mapNotNull { it.duration }
        .fold(Duration.ZERO) { acc, duration -> acc + duration }
