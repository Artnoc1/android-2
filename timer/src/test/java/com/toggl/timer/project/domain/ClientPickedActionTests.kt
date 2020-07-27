package com.toggl.timer.project.domain

import com.toggl.models.domain.EditableProject
import com.toggl.models.domain.Client
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
@DisplayName("The ClientPicked action")
internal class ClientPickedActionTests : CoroutineTest() {
    private val reducer = createProjectReducer(dispatcherProvider = dispatcherProvider)

    @Test
    fun `sets the editableProject's clientId property when the client isn't null`() = runBlockingTest {
        val editableProject = EditableProject.empty(1)
        val initialState = createInitialState(editableProject = editableProject)
        val client = Client(10, "The New Client", 1)

        reducer.testReduceState(
            initialState = initialState,
            action = ProjectAction.ClientPicked(client)
        ) { state -> state.editableProject.clientId shouldBe client.id }
    }

    @Test
    fun `sets the editableProject's clientId property to null if the client is null`() = runBlockingTest {
        val editableProject = EditableProject.empty(1).copy(clientId = 1)
        val initialState = createInitialState(editableProject = editableProject)

        reducer.testReduceState(
            initialState = initialState,
            action = ProjectAction.ClientPicked(null)
        ) { state -> state.editableProject.clientId shouldBe null }
    }

    @Test
    fun `returns no effects`() = runBlockingTest {
        val editableProject = EditableProject.empty(1)
        val initialState = createInitialState(editableProject = editableProject)
        val client = Client(10, "The New Client", 1)

        reducer.testReduce(
            initialState = initialState,
            action = ProjectAction.ClientPicked(client),
            testCase = ::assertNoEffectsWereReturned
        )
    }
}