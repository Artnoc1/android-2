package com.toggl.timer.startedit.domain

import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.models.domain.TimeEntry
import com.toggl.models.domain.Workspace
import com.toggl.repository.Repository
import com.toggl.repository.interfaces.StartTimeEntryResult
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.createTimeEntry
import com.toggl.timer.common.shouldEmitTimeEntryAction
import com.toggl.timer.common.testReduceEffects
import com.toggl.timer.common.toMutableValue
import io.kotest.DisplayName
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime

@ExperimentalCoroutinesApi
@DisplayName("The DoneButtonTapped action")
class DoneButtonTappedActionTests : CoroutineTest() {
    private val repository = mockk<Repository>()
    private val startTimeEntryResult = mockk<StartTimeEntryResult>()
    private val editedTimeEntry = createTimeEntry(1, description = "Test", billable = true, tags = listOf(1, 2), projectId = 10)
    private val timeEntry = createTimeEntry(1, "old description", billable = false)
    private val timeEntry2 = createTimeEntry(2, "old description", billable = true)
    private val workspace = mockk<Workspace> { every { id } returns 1 }
    private val timeService = mockk<TimeService> { every { now() } returns OffsetDateTime.MAX }
    private val editableTimeEntry = EditableTimeEntry.fromSingle(editedTimeEntry)
    private val reducer = createReducer(repository = repository, timeService = timeService, dispatcherProvider = dispatcherProvider)
    private val state = createInitialState(
        workspaces = listOf(workspace),
        timeEntries = listOf(timeEntry, timeEntry2),
        editableTimeEntry = editableTimeEntry
    )

    init {
        coEvery { workspace.id } returns 1
        every { startTimeEntryResult.startedTimeEntry } returns mockk()
        every { startTimeEntryResult.stoppedTimeEntry } returns mockk()
    }

    @Test
    fun `should start the TE if the editable has no ids and no duration`() = runBlockingTest {
        val initialState = state.copy(editableTimeEntry = editableTimeEntry.copy(ids = listOf(), duration = null))

        reducer.testReduceEffects(
            initialState,
            StartEditAction.DoneButtonTapped
        ) { effects ->
            val actions = effects.map { it.execute() }.filterIsInstance(StartEditAction.TimeEntryHandling::class.java)
            actions.shouldBeSingleton()
            effects.first().shouldEmitTimeEntryAction<StartEditAction.TimeEntryHandling, TimeEntryAction.StartTimeEntry>()
        }
    }

    @Test
    fun `should create a TE if the editable has no ids and a duration set`() = runBlockingTest {
        val initialState = state.copy(editableTimeEntry = editableTimeEntry.copy(ids = listOf(), duration = Duration.ofMinutes(2)))

        reducer.testReduceEffects(
            initialState,
            StartEditAction.DoneButtonTapped
        ) { effects ->
            val actions = effects.map { it.execute() }.filterIsInstance(StartEditAction.TimeEntryHandling::class.java)
            actions.shouldBeSingleton()
            effects.first().shouldEmitTimeEntryAction<StartEditAction.TimeEntryHandling, TimeEntryAction.CreateTimeEntry>()
        }
    }

    @Test
    fun `should update the TE if the editable has one id`() = runBlockingTest {
        reducer.testReduceEffects(
            state.copy(),
            StartEditAction.DoneButtonTapped
        ) { effects ->
            val actions = effects.map { it.execute() }.filterIsInstance(StartEditAction.TimeEntryHandling::class.java)
            actions.shouldBeSingleton()
            effects.first().shouldEmitTimeEntryAction<StartEditAction.TimeEntryHandling, TimeEntryAction.EditTimeEntry> {
                it.timeEntry.shouldBe(
                    TimeEntry(
                        id = editableTimeEntry.ids.single(),
                        description = editableTimeEntry.description,
                        billable = editableTimeEntry.billable,
                        workspaceId = editableTimeEntry.workspaceId,
                        projectId = editableTimeEntry.projectId,
                        taskId = editableTimeEntry.taskId,
                        tagIds = editableTimeEntry.tagIds,
                        startTime = editableTimeEntry.startTime!!,
                        duration = editableTimeEntry.duration,
                        isDeleted = false
                    )
                )
            }
        }
    }

    @Test
    fun `should not update the duration or start time when editing a group`() = runBlockingTest {
        val initialState = state.copy(editableTimeEntry = editableTimeEntry.copy(ids = listOf(1L, 2L)))
        reducer.testReduceEffects(
            initialState,
            StartEditAction.DoneButtonTapped
        ) { effects ->
                val actions = effects.map { it.execute() }.filterIsInstance(StartEditAction.TimeEntryHandling::class.java)
                actions shouldHaveSize 2
                actions.forEach { action -> action.timeEntryAction.shouldBeTypeOf<TimeEntryAction.EditTimeEntry> {
                    it.timeEntry.shouldBe(
                        TimeEntry(
                            id = it.timeEntry.id,
                            description = editableTimeEntry.description,
                            billable = editableTimeEntry.billable,
                            workspaceId = editableTimeEntry.workspaceId,
                            projectId = editableTimeEntry.projectId,
                            taskId = editableTimeEntry.taskId,
                            tagIds = editableTimeEntry.tagIds,
                            startTime = state.timeEntries.getValue(it.timeEntry.id).startTime,
                            duration = state.timeEntries.getValue(it.timeEntry.id).duration,
                            isDeleted = false
                        )
                    )
                }
            }
        }
    }

    @Test
    fun `should update each TE in a group if the editable has several ids`() = runBlockingTest {
        val initialState = state.copy(editableTimeEntry = editableTimeEntry.copy(ids = listOf(1L, 2L)))
        reducer.testReduceEffects(
            initialState,
            StartEditAction.DoneButtonTapped
        ) { effects ->
            val actions = effects.map { it.execute() }.filterIsInstance(StartEditAction.TimeEntryHandling::class.java)
            actions shouldHaveSize 2
            actions.forEach { it.timeEntryAction.shouldBeTypeOf<TimeEntryAction.EditTimeEntry>() }
        }
    }

    @Test
    fun `should update no TEs if they're not in state`() = runBlockingTest {
        var initialState = state.copy(editableTimeEntry = editableTimeEntry.copy(ids = listOf(3L, 4L)))
        val mutableValue = initialState.toMutableValue { initialState = it }

        val result = reducer.reduce(mutableValue, StartEditAction.DoneButtonTapped)
        val actions = result.map { it.execute() }.filterIsInstance(StartEditAction.TimeEntryHandling::class.java)

        actions.size shouldBe 0
        coVerify(exactly = 0) {
            repository.editTimeEntry(any())
        }
    }
}