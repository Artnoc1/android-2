package com.toggl.timer.common.domain

import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.core.Effect
import com.toggl.models.domain.TimeEntry
import com.toggl.repository.interfaces.TimeEntryRepository
import kotlinx.coroutines.withContext

class StopTimeEntryEffect<Action>(
    private val repository: TimeEntryRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val mapFn: (TimeEntry) -> Action
) : Effect<Action> {
    override suspend fun execute(): Action? = withContext(dispatcherProvider.io) {
        repository.stopRunningTimeEntry()
            ?.run(mapFn)
    }
}
