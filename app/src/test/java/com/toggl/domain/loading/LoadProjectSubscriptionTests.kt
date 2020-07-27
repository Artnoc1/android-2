package com.toggl.domain.loading

import com.toggl.architecture.Failure
import com.toggl.architecture.Loadable
import com.toggl.common.CoroutineTest
import com.toggl.domain.AppState
import com.toggl.domain.extensions.createProject
import com.toggl.domain.extensions.createUser
import com.toggl.models.domain.Project
import com.toggl.models.domain.User
import com.toggl.repository.interfaces.ProjectRepository
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The LoadProjectSubscription")
class LoadProjectSubscriptionTests : CoroutineTest() {
    private val repository = mockk<ProjectRepository>()
    private val subscription = LoadProjectsSubscriptions(repository, dispatcherProvider)

    private val uninitializedUser = Loadable.Uninitialized
    private val loadingUser = Loadable.Loading
    private val errorUser = Loadable.Error<User>(Failure(IllegalAccessError(), ""))
    private val loadedUser = Loadable.Loaded(createUser(1))

    private val p1 = createProject(1)
    private val p2 = createProject(2)

    @Test
    fun `emits empty project list when user is not loaded`() = runBlockingTest {
        subscription.testSubscribe(
            inputProjectsFlow = flowOf(listOf(p1, p2)),
            inputStateFlow = flowOf(
                AppState(user = uninitializedUser),
                AppState(user = loadingUser),
                AppState(user = errorUser)
            ),
            expectedOutput = listOf(
                emptyList()
            )
        )
    }

    @Test
    fun `emits projects only after user is fully loaded`() = runBlockingTest {
        subscription.testSubscribe(
            inputProjectsFlow = flowOf(listOf(p1, p2)),
            inputStateFlow = flowOf(
                AppState(user = uninitializedUser),
                AppState(user = errorUser),
                AppState(user = loadingUser),
                AppState(user = loadedUser)
            ),
            expectedOutput = listOf(
                listOf(),
                listOf(p1, p2)
            )
        )
    }

    @Test
    fun `emits empty projects list right after logout`() = runBlockingTest {
        subscription.testSubscribe(
            inputProjectsFlow = flowOf(listOf(p1, p2)),
            inputStateFlow = flowOf(
                AppState(user = loadedUser),
                AppState(user = loadingUser),
                AppState(user = uninitializedUser),
                AppState(user = errorUser)
            ),
            expectedOutput = listOf(
                listOf(p1, p2),
                listOf()
            )
        )
    }

    @Test
    fun `re-emits projects after repeated login`() = runBlockingTest {
        subscription.testSubscribe(
            inputProjectsFlow = flowOf(listOf(p1, p2)),
            inputStateFlow = flowOf(
                AppState(user = loadedUser),
                AppState(user = loadingUser),
                AppState(user = loadedUser),
                AppState(user = uninitializedUser)
            ),
            expectedOutput = listOf(
                listOf(p1, p2),
                listOf(),
                listOf(p1, p2),
                listOf()
            )
        )
    }

    @Test
    fun `always emits the latest project list`() = runBlockingTest {
        subscription.testSubscribe(
            inputProjectsFlow = flowOf(
                listOf(p1),
                listOf(p2),
                listOf(p1, p2)
            ),
            inputStateFlow = flowOf(
                AppState(user = loadedUser)
            ),
            expectedOutput = listOf(
                listOf(p1),
                listOf(p2),
                listOf(p1, p2)
            )
        )
    }

    private suspend fun LoadProjectsSubscriptions.testSubscribe(
        inputProjectsFlow: Flow<List<Project>>,
        inputStateFlow: Flow<AppState>,
        expectedOutput: List<List<Project>>
    ) {
        every { repository.loadProjects() } returns inputProjectsFlow
        val outputActionFlow = this.subscribe(inputStateFlow)
        val outputActions = outputActionFlow.toList(mutableListOf())
        val outTimeEntries = outputActions
            .map { it.action as LoadingAction.ProjectsLoaded }
            .map { it.projects }
        outTimeEntries shouldBe expectedOutput
    }
}