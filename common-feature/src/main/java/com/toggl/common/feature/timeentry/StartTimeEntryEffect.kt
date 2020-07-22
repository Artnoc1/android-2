package com.toggl.common.feature.timeentry

import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.core.Effect
import com.toggl.repository.dto.StartTimeEntryDTO
import com.toggl.repository.interfaces.TimeEntryRepository
import kotlinx.coroutines.withContext

class StartTimeEntryEffect(
    private val startTimeEntryDTO: StartTimeEntryDTO,
    private val repository: TimeEntryRepository,
    private val dispatcherProvider: DispatcherProvider
) : Effect<TimeEntryAction> {
    override suspend fun execute(): TimeEntryAction? = withContext(dispatcherProvider.io) {
        repository
            .startTimeEntry(startTimeEntryDTO)
            .let { TimeEntryAction.TimeEntriesUpdated }
    }
}
