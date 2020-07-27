package com.toggl.timer.project.domain

import com.toggl.models.domain.EditableProject
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
@DisplayName("The ColorPicked action")
internal class ColorPickedActionTests : CoroutineTest() {
    private val reducer = createProjectReducer(dispatcherProvider = dispatcherProvider)

    @Test
    fun `sets the editableProject's color property`() = runBlockingTest {
        val editableProject = EditableProject.empty(1)
        val initialState = createInitialState(editableProject = editableProject)
        val color = "#123123"

        reducer.testReduceState(
            initialState = initialState,
            action = ProjectAction.ColorPicked(color)
        ) { state -> state.editableProject.color shouldBe color }
    }

    @Test
    fun `returns no effects`() = runBlockingTest {
        val editableProject = EditableProject.empty(1)
        val initialState = createInitialState(editableProject = editableProject)

        reducer.testReduce(
            initialState = initialState,
            action = ProjectAction.ColorPicked("#123123"),
            testCase = ::assertNoEffectsWereReturned
        )
    }
}