package com.toggl.common.feature.timeentry

import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.core.Effect
import com.toggl.models.domain.TimeEntry
import com.toggl.repository.interfaces.TimeEntryRepository
import kotlinx.coroutines.withContext

class DeleteTimeEntryEffect(
    private val timeEntryToDelete: TimeEntry,
    private val repository: TimeEntryRepository,
    private val dispatcherProvider: DispatcherProvider
) : Effect<TimeEntryAction> {
    override suspend fun execute(): TimeEntryAction? = withContext(dispatcherProvider.io) {
        repository
            .deleteTimeEntry(timeEntryToDelete)
            .let { TimeEntryAction.TimeEntriesUpdated }
    }
}
