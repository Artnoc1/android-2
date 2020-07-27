package com.toggl.timer.startedit.domain

import com.toggl.common.Constants.AutoCompleteSuggestions.projectToken
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.timer.common.testReduceEffects
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
@DisplayName("The AddProjectChipTapped action")
internal class AddProjectChipTappedTests {
    val initialState = createInitialState()
    val reducer = createReducer()

    @Test
    fun `should return an effect to set description to the project token when the description is empty`() = runBlockingTest {
        reducer.testReduceEffects(initialState, StartEditAction.AddProjectChipTapped) { effects ->
            val action = effects.single().execute() as StartEditAction.DescriptionEntered
            action.description shouldBe projectToken.toString()
            action.cursorPosition shouldBe 1
        }
    }

    @Test
    fun `should return an effect to append the project token to description`() = runBlockingTest {
        val editableWithDescription =
            EditableTimeEntry(workspaceId = 1, description = "asdf")
        val state = initialState.copy(editableTimeEntry = editableWithDescription)
        reducer.testReduceEffects(state, StartEditAction.AddProjectChipTapped) { effects ->
            val action = effects.single().execute() as StartEditAction.DescriptionEntered
            action.description shouldBe editableWithDescription.description + " $projectToken"
            action.cursorPosition shouldBe editableWithDescription.description.length + 2
        }
    }
}
