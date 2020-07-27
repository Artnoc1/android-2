package com.toggl.domain.reducers

import com.toggl.architecture.Loadable
import com.toggl.common.CoroutineTest
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.backStackOf
import com.toggl.domain.extensions.createClient
import com.toggl.domain.extensions.createProject
import com.toggl.domain.extensions.createTag
import com.toggl.domain.extensions.createTask
import com.toggl.domain.extensions.createTimeEntry
import com.toggl.domain.extensions.toMutableValue
import com.toggl.domain.loading.LoadClientsEffect
import com.toggl.domain.loading.LoadTagsEffect
import com.toggl.domain.loading.LoadTasksEffect
import com.toggl.domain.loading.LoadUserPreferencesEffect
import com.toggl.domain.loading.LoadWorkspacesEffect
import com.toggl.domain.loading.LoadingAction
import com.toggl.domain.loading.LoadingReducer
import com.toggl.domain.loading.LoadingState
import com.toggl.domain.loading.TryLoadingUserEffect
import com.toggl.models.domain.DateFormat
import com.toggl.models.domain.DurationFormat
import com.toggl.models.domain.SmartAlertsOption
import com.toggl.models.domain.User
import com.toggl.models.domain.UserPreferences
import com.toggl.models.domain.Workspace
import com.toggl.models.domain.WorkspaceFeature
import com.toggl.models.validation.ApiToken
import com.toggl.models.validation.Email
import com.toggl.repository.interfaces.ClientRepository
import com.toggl.repository.interfaces.SettingsRepository
import com.toggl.repository.interfaces.TagRepository
import com.toggl.repository.interfaces.TaskRepository
import com.toggl.repository.interfaces.UserRepository
import com.toggl.repository.interfaces.WorkspaceRepository
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.time.DayOfWeek

@ExperimentalCoroutinesApi
@DisplayName("The Loading Reducer")
class LoadingReducerTests : CoroutineTest() {
    private val clientRepository = mockk<ClientRepository>()
    private val workspaceRepository = mockk<WorkspaceRepository>()
    private val tagRepository = mockk<TagRepository>()
    private val userRepository = mockk<UserRepository>()
    private val taskRepository = mockk<TaskRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val reducer = LoadingReducer(
        clientRepository,
        workspaceRepository,
        tagRepository,
        taskRepository,
        userRepository,
        settingsRepository,
        dispatcherProvider
    )

    private val emptyState = LoadingState(
        Loadable.Uninitialized, listOf(), listOf(), listOf(), listOf(), listOf(), listOf(),
        UserPreferences(
            manualModeEnabled = true,
            twentyFourHourClockEnabled = false,
            selectedWorkspaceId = 1,
            dateFormat = DateFormat.DDMMYYYY_dash,
            durationFormat = DurationFormat.Decimal,
            firstDayOfTheWeek = DayOfWeek.MONDAY,
            groupSimilarTimeEntriesEnabled = true,
            cellSwipeActionsEnabled = true,
            smartAlertsOption = SmartAlertsOption.Disabled,
            calendarIntegrationEnabled = true,
            calendarIds = emptyList()
        ), listOf()
    )

    @Nested
    @DisplayName("when receiving a Start Loading action")
    inner class StartLoading {
        fun `sets the user to loading`() {
            var initialState = emptyState
            val mutableValue = initialState.toMutableValue { initialState = it }
            reducer.reduce(mutableValue, LoadingAction.StartLoading)

            initialState shouldBe emptyState.copy(
                backStack = backStackOf(Route.Timer)
            )
        }

        fun `returns an effect to load the user`() {
            var initialState = emptyState
            val mutableValue = initialState.toMutableValue { initialState = it }
            val effects = reducer.reduce(mutableValue, LoadingAction.StartLoading)

            effects.single().shouldBeInstanceOf<TryLoadingUserEffect>()
        }
    }

    @Nested
    @DisplayName("when receiving an user loaded action")
    inner class UserLoadedAction {

        @Nested
        @DisplayName("with a non null user")
        inner class ValidUser {

            fun `sets the current route to the timer page`() {
                var initialState = emptyState
                val mutableValue = initialState.toMutableValue { initialState = it }
                reducer.reduce(mutableValue, LoadingAction.UserLoaded(User(
                    id = 0,
                    apiToken = ApiToken.from("12345678901234567890123456789012") as ApiToken.Valid,
                    defaultWorkspaceId = 1,
                    email = Email.from("validemail@toggl.com") as Email.Valid,
                    name = "name"
                )))

                initialState shouldBe emptyState.copy(
                    backStack = backStackOf(Route.Timer)
                )
            }

            fun `returns a list of effects that load entities`() {
                var initialState = emptyState
                val mutableValue = initialState.toMutableValue { initialState = it }
                val effects = reducer.reduce(mutableValue, LoadingAction.UserLoaded(User(
                    id = 0,
                    apiToken = ApiToken.from("12345678901234567890123456789012") as ApiToken.Valid,
                    defaultWorkspaceId = 1,
                    email = Email.from("validemail@toggl.com") as Email.Valid,
                    name = "name"
                )))

                effects.map { it.javaClass.kotlin } shouldContainExactlyInAnyOrder listOf(
                    LoadWorkspacesEffect::class,
                    LoadClientsEffect::class,
                    LoadTagsEffect::class,
                    LoadTasksEffect::class,
                    LoadUserPreferencesEffect::class
                )
            }
        }

        @Nested
        @DisplayName("and there is no api token")
        inner class InvalidUser {

            fun `sets the current route to the onboarding page`() {
                var initialState = emptyState
                val mutableValue = initialState.toMutableValue { initialState = it }
                reducer.reduce(mutableValue, LoadingAction.UserLoaded(null))

                initialState shouldBe emptyState.copy(
                    backStack = backStackOf(Route.Login),
                    user = Loadable.Uninitialized
                )
            }

            fun `returns no effects`() {
                var initialState = emptyState
                val mutableValue = initialState.toMutableValue { initialState = it }
                val effects = reducer.reduce(mutableValue, LoadingAction.UserLoaded(null))
                effects.shouldBeEmpty()
            }
        }
    }

    @Nested
    @DisplayName("when receiving a Time Entries Loaded action")
    inner class TimeEntriesLoadedAction {

        fun `updates the state to add the loaded time entries`() {
            val entries = listOf(createTimeEntry(1), createTimeEntry(2), createTimeEntry(3))
            var initialState = emptyState
            val mutableValue = initialState.toMutableValue { initialState = it }
            reducer.reduce(mutableValue, LoadingAction.TimeEntriesLoaded(entries))

            initialState shouldBe emptyState.copy(timeEntries = entries)
        }
    }

    @Nested
    @DisplayName("when receiving a Workspaces Loaded action")
    inner class WorkspacesLoadedAction {

        fun `updates the state to add the loaded workspaces`() {
            val workspaces = listOf(
                Workspace(1, "1", listOf()),
                Workspace(2, "2", listOf(WorkspaceFeature.Pro)),
                Workspace(3, "3", listOf())
            )
            var initialState = emptyState
            val mutableValue = initialState.toMutableValue { initialState = it }
            reducer.reduce(mutableValue, LoadingAction.WorkspacesLoaded(workspaces))

            initialState shouldBe emptyState.copy(workspaces = workspaces)
        }
    }

    @Nested
    @DisplayName("when receiving a Projects Loaded action")
    inner class ProjectsLoadedAction {

        fun `updates the state to add the loaded projects`() {
            val projects = (1L..10L).map { createProject(it) }
            var initialState = emptyState
            val mutableValue = initialState.toMutableValue { initialState = it }
            reducer.reduce(mutableValue, LoadingAction.ProjectsLoaded(projects))

            initialState shouldBe emptyState.copy(projects = projects)
        }
    }

    @Nested
    @DisplayName("when receiving a Clients Loaded action")
    inner class ClientsLoadedAction {

        fun `updates the state to add the loaded clients`() {
            val clients = (1L..10L).map { createClient(it) }
            var initialState = emptyState
            val mutableValue = initialState.toMutableValue { initialState = it }
            reducer.reduce(mutableValue, LoadingAction.ClientsLoaded(clients))

            initialState shouldBe emptyState.copy(clients = clients)
        }
    }

    @Nested
    @DisplayName("when receiving a Tags Loaded action")
    inner class TagsLoadedAction {

        fun `updates the state to add the loaded tags`() {
            val tags = (1L..10L).map { createTag(it) }
            var initialState = emptyState
            val mutableValue = initialState.toMutableValue { initialState = it }
            reducer.reduce(mutableValue, LoadingAction.TagsLoaded(tags))

            initialState shouldBe emptyState.copy(tags = tags)
        }
    }

    @Nested
    @DisplayName("when receiving a Tasks Loaded action")
    inner class TasksLoadedAction {

        fun `updates the state to add the loaded tasks`() {
            val tasks = (1L..10L).map { createTask(it) }
            var initialState = emptyState
            val mutableValue = initialState.toMutableValue { initialState = it }
            reducer.reduce(mutableValue, LoadingAction.TasksLoaded(tasks))

            initialState shouldBe emptyState.copy(tasks = tasks)
        }
    }
}