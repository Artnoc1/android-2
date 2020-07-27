package com.toggl.timer.startedit.domain

import com.toggl.models.domain.Workspace
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.createTimeEntry
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.timer.common.testReduce
import com.toggl.timer.common.testReduceState
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The TimeEntryDescriptionChanged action")
internal class DescriptionEnteredActionTests : CoroutineTest() {
    private val workspace = mockk<Workspace> { every { id } returns 1 }
    private val editableTimeEntry = EditableTimeEntry.fromSingle(createTimeEntry(1, description = ""))
    private val state = createInitialState(workspaces = listOf(workspace), editableTimeEntry = editableTimeEntry)
    private val reducer = createReducer(dispatcherProvider = dispatcherProvider)

    @Test
    fun `should change EditableTimeEntry's description`() = runBlockingTest {
        reducer.testReduceState(
            initialState = state,
            action = StartEditAction.DescriptionEntered("new description", 5)
        ) { state -> state.editableTimeEntry.description shouldBe "new description" }
    }

    @Test
    fun `should change the state's cursorPosition`() = runBlockingTest {
        reducer.testReduce(
            initialState = state,
            action = StartEditAction.DescriptionEntered("new description", 5)
        ) { state, _ -> state.cursorPosition shouldBe 5 }
    }

    @Test
    fun `returns the effect that updates the autocomplete suggestions`() = runBlockingTest {
        reducer.testReduce(
            initialState = state,
            action = StartEditAction.DescriptionEntered("new description", 5)
        ) { _, effects -> effects.single()::class shouldBe UpdateAutocompleteSuggestionsEffect::class }
    }
}
