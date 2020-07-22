package com.toggl.common.feature.timeentry

import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.core.Effect
import com.toggl.models.domain.TimeEntry
import com.toggl.repository.interfaces.TimeEntryRepository
import kotlinx.coroutines.withContext

class EditTimeEntryEffect(
    private val timeEntry: TimeEntry,
    private val repository: TimeEntryRepository,
    private val dispatcherProvider: DispatcherProvider
) : Effect<TimeEntryAction> {
    override suspend fun execute(): TimeEntryAction? = withContext(dispatcherProvider.io) {
        repository
            .editTimeEntry(timeEntry)
            .let { TimeEntryAction.TimeEntriesUpdated }
    }
}
