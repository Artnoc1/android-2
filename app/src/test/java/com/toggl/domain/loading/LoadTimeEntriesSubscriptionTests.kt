package com.toggl.domain.loading

import com.toggl.architecture.Failure
import com.toggl.architecture.Loadable
import com.toggl.common.CoroutineTest
import com.toggl.domain.AppState
import com.toggl.domain.extensions.createTimeEntry
import com.toggl.domain.extensions.createUser
import com.toggl.models.domain.TimeEntry
import com.toggl.models.domain.User
import com.toggl.repository.interfaces.TimeEntryRepository
import io.kotlintest.shouldBe
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
@DisplayName("The LoadTimeEntriesSubscription")
class LoadTimeEntriesSubscriptionTests : CoroutineTest() {
    private val repository = mockk<TimeEntryRepository>()
    private val subscription = LoadTimeEntriesSubscription(repository, dispatcherProvider)

    private val uninitializedUser = Loadable.Uninitialized
    private val loadingUser = Loadable.Loading
    private val errorUser = Loadable.Error<User>(Failure(IllegalAccessError(), ""))
    private val loadedUser = Loadable.Loaded(createUser(1))

    private val te1 = createTimeEntry(1)
    private val te2 = createTimeEntry(2)

    @Test
    fun `emits empty time entry list when user is not loaded`() = runBlockingTest {
        subscription.testSubscribe(
            inputTimeEntriesFlow = flowOf(listOf(te1, te2)),
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
    fun `emits time entries only after user is fully loaded`() = runBlockingTest {
        subscription.testSubscribe(
            inputTimeEntriesFlow = flowOf(listOf(te1, te2)),
            inputStateFlow = flowOf(
                AppState(user = uninitializedUser),
                AppState(user = errorUser),
                AppState(user = loadingUser),
                AppState(user = loadedUser)
            ),
            expectedOutput = listOf(
                listOf(),
                listOf(te1, te2)
            )
        )
    }

    @Test
    fun `emits empty time entry list right after logout`() = runBlockingTest {
        subscription.testSubscribe(
            inputTimeEntriesFlow = flowOf(listOf(te1, te2)),
            inputStateFlow = flowOf(
                AppState(user = loadedUser),
                AppState(user = loadingUser),
                AppState(user = uninitializedUser),
                AppState(user = errorUser)
            ),
            expectedOutput = listOf(
                listOf(te1, te2),
                listOf()
            )
        )
    }

    @Test
    fun `re-emits time entries after repeated login`() = runBlockingTest {
        subscription.testSubscribe(
            inputTimeEntriesFlow = flowOf(listOf(te1, te2)),
            inputStateFlow = flowOf(
                AppState(user = loadedUser),
                AppState(user = loadingUser),
                AppState(user = loadedUser),
                AppState(user = uninitializedUser)
            ),
            expectedOutput = listOf(
                listOf(te1, te2),
                listOf(),
                listOf(te1, te2),
                listOf()
            )
        )
    }

    @Test
    fun `always emits the latest time entry list`() = runBlockingTest {
        subscription.testSubscribe(
            inputTimeEntriesFlow = flowOf(
                listOf(te1),
                listOf(te2),
                listOf(te1, te2)
            ),
            inputStateFlow = flowOf(
                AppState(user = loadedUser)
            ),
            expectedOutput = listOf(
                listOf(te1),
                listOf(te2),
                listOf(te1, te2)
            )
        )
    }

    private suspend fun LoadTimeEntriesSubscription.testSubscribe(
        inputTimeEntriesFlow: Flow<List<TimeEntry>>,
        inputStateFlow: Flow<AppState>,
        expectedOutput: List<List<TimeEntry>>
    ) {
        every { repository.loadTimeEntries() } returns inputTimeEntriesFlow
        val outputActionFlow = this.subscribe(inputStateFlow)
        val outputActions = outputActionFlow.toList(mutableListOf())
        val outTimeEntries = outputActions
            .map { it.action as LoadingAction.TimeEntriesLoaded }
            .map { it.timeEntries }
        outTimeEntries shouldBe expectedOutput
    }
}