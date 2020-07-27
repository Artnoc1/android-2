package com.toggl.timer.project.domain

import com.toggl.common.Constants
import com.toggl.models.domain.EditableProject
import com.toggl.models.domain.Project
import com.toggl.models.domain.Workspace
import com.toggl.models.domain.WorkspaceFeature
import com.toggl.timer.common.CoroutineTest
import io.kotest.matchers.boolean.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
@DisplayName("The ProjectColorSelectorTests returns")
internal class ProjectColorSelectorTests : CoroutineTest() {

    private val selector = ProjectColorSelector()

    @Test
    fun `a color view model for each default color`() = runBlockingTest {
        val state = createInitialState()
        val colors = selector.select(state)

        val defaultColors = colors.filterIsInstance<ColorViewModel.DefaultColor>()

        defaultColors shouldHaveSize Project.defaultColors.size
        defaultColors.forEach { colorViewModel ->
            Project.defaultColors.contains(colorViewModel.color).shouldBeTrue()
        }
    }

    @Test
    fun `a custom color viewmodel if the workspace is pro`() = runBlockingTest {

        val workspace = Workspace(100, "Workspace", listOf(WorkspaceFeature.Pro))
        val state = createInitialState(
            editableProject = EditableProject.empty(workspace.id),
            workspaces = listOf(workspace)
        )

        val colors = selector.select(state)

        colors.filterIsInstance<ColorViewModel.CustomColor>().singleOrNull().shouldNotBeNull()
    }

    @Test
    fun `a premium locked viewmodel if the workspace is not pro`() = runBlockingTest {

        val workspace = Workspace(100, "Workspace", listOf())
        val state = createInitialState(
            editableProject = EditableProject.empty(workspace.id),
            workspaces = listOf(workspace)
        )

        val colors = selector.select(state)

        colors.filterIsInstance<ColorViewModel.PremiumLocked>().singleOrNull().shouldNotBeNull()
    }

    @Test
    fun `only one selected view model based on the color of the editable project`() = runBlockingTest {

        val workspace = Workspace(100, "Workspace", listOf())
        val editableProject = EditableProject.empty(workspace.id)
        val state = createInitialState(
            editableProject = editableProject,
            workspaces = listOf(workspace)
        )

        val colors = selector.select(state)

        colors.filterIsInstance<ColorViewModel.DefaultColor>().single { it.selected }.color shouldBe editableProject.color
    }

    @Test
    fun `the custom color as selected if the editable project does not have a default color`() = runBlockingTest {

        val workspace = Workspace(100, "Workspace", listOf(WorkspaceFeature.Pro))
        val editableProject = EditableProject.empty(workspace.id).copy(color = Constants.DefaultCustomColor.hex)
        val state = createInitialState(
            editableProject = editableProject,
            workspaces = listOf(workspace)
        )

        val colors = selector.select(state)

        colors.filterIsInstance<ColorViewModel.CustomColor>().single().selected.shouldBeTrue()
    }
}
