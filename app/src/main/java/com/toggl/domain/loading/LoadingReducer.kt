package com.toggl.domain.loading

import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.Loadable
import com.toggl.architecture.core.Effect
import com.toggl.architecture.core.MutableValue
import com.toggl.architecture.core.Reducer
import com.toggl.architecture.extensions.effect
import com.toggl.architecture.extensions.effects
import com.toggl.common.feature.extensions.mutateWithoutEffects
import com.toggl.common.feature.extensions.returnEffect
import com.toggl.common.feature.navigation.Route
import com.toggl.repository.interfaces.ClientRepository
import com.toggl.repository.interfaces.SettingsRepository
import com.toggl.repository.interfaces.TagRepository
import com.toggl.repository.interfaces.TaskRepository
import com.toggl.repository.interfaces.WorkspaceRepository
import com.toggl.repository.interfaces.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadingReducer @Inject constructor(
    private val clientRepository: ClientRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val tagsRepository: TagRepository,
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository,
    private val dispatcherProvider: DispatcherProvider
) : Reducer<LoadingState, LoadingAction> {

    override fun reduce(
        state: MutableValue<LoadingState>,
        action: LoadingAction
    ): List<Effect<LoadingAction>> =
        when (action) {
            LoadingAction.StartLoading -> state.mutate {
                copy(user = Loadable.Loading)
            } returnEffect effect(TryLoadingUserEffect(userRepository, dispatcherProvider))
            is LoadingAction.UserLoaded ->
                if (action.user == null) state.mutateWithoutEffects { copy(user = Loadable.Uninitialized, backStack = listOf(Route.Login)) }
                else state.mutate { copy(user = Loadable.Loaded(action.user), backStack = listOf(Route.Timer)) } returnEffect effects(
                    LoadWorkspacesEffect(workspaceRepository, dispatcherProvider),
                    LoadClientsEffect(clientRepository, dispatcherProvider),
                    LoadTagsEffect(tagsRepository, dispatcherProvider),
                    LoadTasksEffect(taskRepository, dispatcherProvider),
                    LoadUserPreferencesEffect(settingsRepository, dispatcherProvider)
                )
            is LoadingAction.TimeEntriesLoaded -> state.mutateWithoutEffects { copy(timeEntries = action.timeEntries) }
            is LoadingAction.WorkspacesLoaded -> state.mutateWithoutEffects { copy(workspaces = action.workspaces) }
            is LoadingAction.ProjectsLoaded -> state.mutateWithoutEffects { copy(projects = action.projects) }
            is LoadingAction.ClientsLoaded -> state.mutateWithoutEffects { copy(clients = action.clients) }
            is LoadingAction.TagsLoaded -> state.mutateWithoutEffects { copy(tags = action.tags) }
            is LoadingAction.TasksLoaded -> state.mutateWithoutEffects { copy(tasks = action.tasks) }
            is LoadingAction.UserPreferencesLoaded -> state.mutateWithoutEffects { copy(userPreferences = action.userPreferences) }
        }
}