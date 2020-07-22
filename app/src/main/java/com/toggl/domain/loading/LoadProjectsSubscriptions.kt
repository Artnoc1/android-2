package com.toggl.domain.loading

import com.toggl.architecture.DispatcherProvider
import com.toggl.repository.interfaces.ProjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@ExperimentalCoroutinesApi
class LoadProjectsSubscriptions(
    private val projectRepository: ProjectRepository,
    dispatcherProvider: DispatcherProvider
) : BaseLoadingSubscription(dispatcherProvider) {
    override fun subscribe(isUserLoggedIn: Boolean): Flow<LoadingAction> {
        val projects = if (isUserLoggedIn) projectRepository.loadProjects()
        else flowOf(emptyList())
        return projects.map { LoadingAction.ProjectsLoaded(it) }
    }
}