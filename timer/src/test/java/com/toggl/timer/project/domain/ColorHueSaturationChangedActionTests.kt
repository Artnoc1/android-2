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
@DisplayName("The ColorHueSaturationChanged action")
internal class ColorHueSaturationChangedActionTests : CoroutineTest() {
    private val reducer = createProjectReducer(dispatcherProvider = dispatcherProvider)

    init {
        mockkStatic("com.toggl.common.feature.extensions.ColorExtensionsKt")
        every {
            HSVColor(0.1f, 0.2f, 1f).toHex()
        } returns "#123123"
    }

    @Test
    fun `sets the customColor's hue and saturation property`() = runBlockingTest {
        val editableProject = EditableProject.empty(1)
        val customColor = HSVColor(1f, 1f, 1f)
        val initialState = createInitialState(
            editableProject = editableProject,
            customColor = customColor
        )

        reducer.testReduceState(
            initialState = initialState,
            action = ProjectAction.ColorHueSaturationChanged(0.1f, 0.2f)
        ) { state -> state.customColor shouldBe customColor.copy(hue = 0.1f, saturation = 0.2f) }
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
            ProjectAction.ColorHueSaturationChanged(0.1f, 0.2f)
        ) { effects ->
            effects.shouldBeSingleton()
            val effectAction = effects.first().execute()
            effectAction.shouldBeTypeOf<ProjectAction.ColorPicked> { colorPickedAction ->
                colorPickedAction.color shouldBe "#123123"
            }
        }
    }
}