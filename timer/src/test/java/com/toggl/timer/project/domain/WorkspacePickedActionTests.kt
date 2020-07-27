package com.toggl.timer.project.domain

import com.toggl.models.domain.EditableProject
import com.toggl.models.domain.Workspace
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.assertNoEffectsWereReturned
import com.toggl.timer.common.testReduce
import com.toggl.timer.common.testReduceState
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The WorkspacePicked action")
internal class WorkspacePickedActionTests : CoroutineTest() {
    private val reducer = createProjectReducer(dispatcherProvider = dispatcherProvider)

    @Test
    fun `sets the editableProject's workspaceId property`() = runBlockingTest {
        val editableProject = EditableProject.empty(1)
        val initialState = createInitialState(editableProject = editableProject)
        val workspace = Workspace(10, "The New Workspace", emptyList())

        reducer.testReduceState(
            initialState = initialState,
            action = ProjectAction.WorkspacePicked(workspace)
        ) { state -> state.editableProject.workspaceId shouldBe workspace.id }
    }

    @Test
    fun `returns no effects`() = runBlockingTest {
        val editableProject = EditableProject.empty(1)
        val initialState = createInitialState(editableProject = editableProject)
        val workspace = Workspace(10, "The New Workspace", emptyList())

        reducer.testReduce(
            initialState = initialState,
            action = ProjectAction.WorkspacePicked(workspace),
            testCase = ::assertNoEffectsWereReturned
        )
    }
}