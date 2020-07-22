package com.toggl.repository.interfaces

import com.toggl.models.domain.TimeEntry
import com.toggl.repository.dto.CreateTimeEntryDTO
import com.toggl.repository.dto.StartTimeEntryDTO
import kotlinx.coroutines.flow.Flow

data class StartTimeEntryResult(
    val startedTimeEntry: TimeEntry,
    val stoppedTimeEntry: TimeEntry?
)

interface TimeEntryRepository {
    fun loadTimeEntries(): Flow<List<TimeEntry>>
    suspend fun timeEntriesCount(): Int
    suspend fun startTimeEntry(startTimeEntryDTO: StartTimeEntryDTO): StartTimeEntryResult
    suspend fun createTimeEntry(createTimeEntryDTO: CreateTimeEntryDTO): TimeEntry
    suspend fun stopRunningTimeEntry(): TimeEntry?
    suspend fun editTimeEntry(timeEntry: TimeEntry): TimeEntry
    suspend fun deleteTimeEntry(timeEntry: TimeEntry): TimeEntry
}
