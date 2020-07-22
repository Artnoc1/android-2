package com.toggl.domain.loading

import com.toggl.architecture.DispatcherProvider
import com.toggl.repository.interfaces.TimeEntryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@ExperimentalCoroutinesApi
class LoadTimeEntriesSubscription(
    private val timeEntryRepository: TimeEntryRepository,
    dispatcherProvider: DispatcherProvider
) : BaseLoadingSubscription(dispatcherProvider) {
    override fun subscribe(isUserLoggedIn: Boolean): Flow<LoadingAction> {
        val timeEntries = if (isUserLoggedIn) timeEntryRepository.loadTimeEntries()
        else flowOf(emptyList())
        return timeEntries.map { LoadingAction.TimeEntriesLoaded(it) }
    }
}