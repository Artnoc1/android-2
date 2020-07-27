package com.toggl.timer.project.domain

import com.toggl.common.feature.extensions.toHex
import com.toggl.models.domain.EditableProject
import com.toggl.models.validation.HSVColor
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.testReduceEffects
import com.toggl.timer.common.testReduceState
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The ColorValueChanged action")
internal class ColorValueChangedActionTests : CoroutineTest() {
    private val reducer = createProjectReducer(dispatcherProvider = dispatcherProvider)

    init {
        mockkStatic("com.toggl.common.feature.extensions.ColorExtensionsKt")
        every {
            HSVColor(1f, 1f, 0.3f).toHex()
        } returns "#123123"
    }

    @Test
    fun `sets the customColor's value property`() = runBlockingTest {
        val editableProject = EditableProject.empty(1)
        val customColor = HSVColor(1f, 1f, 1f)
        val initialState = createInitialState(
            editableProject = editableProject,
            customColor = customColor
        )

        reducer.testReduceState(
            initialState = initialState,
            action = ProjectAction.ColorValueChanged(0.3f)
        ) { state -> state.customColor shouldBe customColor.copy(value = 0.3f) }
    }

    @Test
    fun `should return color picked effect`() = runBlockingTest {
        val editableProject = EditableProject.empty(1)
        val customColor = HSVColor(1f, 1f, 1f)
        val initialState = createInitialState(
            editableProject = editableProject,
            customColor = customColor
        )

        reducer.testReduceEffects(
            initialState,
            ProjectAction.ColorValueChanged(0.3f)
        ) { effects ->
            effects.shouldBeSingleton()
            val effectAction = effects.first().execute()
            effectAction.shouldBeTypeOf<ProjectAction.ColorPicked> { colorPickedAction ->
                colorPickedAction.color shouldBe "#123123"
            }
        }
    }
}
