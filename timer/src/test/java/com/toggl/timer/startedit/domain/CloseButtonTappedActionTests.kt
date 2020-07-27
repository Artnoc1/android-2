package com.toggl.timer.startedit.domain

import com.toggl.models.domain.EditableTimeEntry
import com.toggl.models.domain.Workspace
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.createTimeEntry
import com.toggl.timer.common.testReduceEffects
import io.kotest.matchers.types.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The CloseButtonTapped action")
class CloseButtonTappedActionTests : CoroutineTest() {
    private val workspace = mockk<Workspace> { every { id } returns 1 }
    private val editableTimeEntry = EditableTimeEntry.fromSingle(createTimeEntry(1, description = "Test"))
    private val state = createInitialState(workspaces = listOf(workspace), editableTimeEntry = editableTimeEntry)
    private val reducer = createReducer(dispatcherProvider = dispatcherProvider)

    @Test
    fun `shouldn't return any effect`() = runBlockingTest {
        reducer.testReduceEffects(
            initialState = state,
            action = StartEditAction.CloseButtonTapped
        ) { effects ->
            val closeAction = effects.single().execute() as? StartEditAction.Close
            closeAction.shouldNotBeNull()
        }
    }
}