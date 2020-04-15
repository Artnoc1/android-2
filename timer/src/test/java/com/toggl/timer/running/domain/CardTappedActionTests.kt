package com.toggl.timer.running.domain

import com.toggl.architecture.DispatcherProvider
import com.toggl.models.domain.Workspace
import com.toggl.repository.interfaces.TimeEntryRepository
import com.toggl.timer.common.assertNoEffectsWereReturned
import com.toggl.timer.common.createTimeEntry
import com.toggl.timer.common.domain.EditableTimeEntry
import com.toggl.timer.common.testReduce
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.threeten.bp.Duration

@ExperimentalCoroutinesApi
@DisplayName("The CardTapped action")
class CardTappedActionTests {
    private val testDispatcher = TestCoroutineDispatcher()
    private val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, Dispatchers.Main)
    private val repository = mockk<TimeEntryRepository>()
    private val workspace = mockk<Workspace>()
    private val reducer = RunningTimeEntryReducer(repository, dispatcherProvider)
    private val editableTimeEntry = EditableTimeEntry.fromSingle(createTimeEntry(1, description = "Test"))

    @Test
    fun `should init editableTimeEntry with an empty id list when no TE is running`() = runBlockingTest {
        val initialState = createInitialState(editableTimeEntry = editableTimeEntry)
        coEvery { workspace.id } returns 1

        reducer.testReduce(
            initialState = initialState,
            action = RunningTimeEntryAction.CardTapped
        ) { state, _ ->
            state.editableTimeEntry.shouldNotBeNull()
            state.editableTimeEntry!!.ids shouldBe emptyList()
        }
    }

    @Test
    fun `should init editableTimeEntry with the currently running time entry when there is one`() = runBlockingTest {
        val initialState = createInitialState(
            editableTimeEntry = editableTimeEntry,
            timeEntries = mapOf(
                1L to createTimeEntry(1, description = "Test", duration = Duration.ofHours(1)),
                2L to createTimeEntry(2, description = "Running", duration = null)
            )
        )
        coEvery { workspace.id } returns 1

        reducer.testReduce(
            initialState = initialState,
            action = RunningTimeEntryAction.CardTapped
        ) { state, _ ->
            state.editableTimeEntry.shouldNotBeNull()
            state.editableTimeEntry!!.description shouldBe "Running"
        }
    }

    @Test
    fun `shouldn't emit any effect effect`() = runBlockingTest {
        val initialState = createInitialState(editableTimeEntry = editableTimeEntry)
        coEvery { workspace.id } returns 1

        reducer.testReduce(
            initialState = initialState,
            action = RunningTimeEntryAction.CardTapped,
            testCase = ::assertNoEffectsWereReturned
        )
    }

    @BeforeEach
    fun beforeTest() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun afterTest() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
}