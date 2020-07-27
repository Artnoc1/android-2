package com.toggl.timer.startedit.domain

import com.toggl.common.Constants.AutoCompleteSuggestions.projectToken
import com.toggl.common.Constants.AutoCompleteSuggestions.tagToken
import com.toggl.models.domain.Tag
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.createTimeEntry
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.timer.project.domain.createProject
import com.toggl.timer.startedit.ui.chips.ChipViewModel
import io.kotest.matchers.boolean.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.types.shouldNotBeNull
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
@DisplayName("The ProjectTagChipSelector returns")
internal class ProjectTagChipSelectorTests : CoroutineTest() {

    private val tagLabel = "Add Tags"
    private val projectLabel = "Add a Project"
    private val selector = ProjectTagChipSelector(projectLabel, tagLabel)

    @Test
    fun `an add project chip when there are no projects`() = runBlockingTest {

        val timeEntry = createTimeEntry(1, projectId = null)
        val editableTimeEntry = EditableTimeEntry.fromSingle(timeEntry)

        val state = createInitialState(editableTimeEntry = editableTimeEntry)
        val chips = selector.select(state)

        chips.filterIsInstance<ChipViewModel.AddProject>().singleOrNull { it.text == "$projectToken $projectLabel" }.shouldNotBeNull()
    }

    @Test
    fun `a project chip when there is a project`() = runBlockingTest {

        val project = createProject(1)
        val timeEntry = createTimeEntry(2, projectId = project.id)
        val projects = listOf(project)
        val editableTimeEntry = EditableTimeEntry.fromSingle(timeEntry)

        val state = createInitialState(projects = projects, editableTimeEntry = editableTimeEntry)
        val chips = selector.select(state)

        chips.filterIsInstance<ChipViewModel.Project>().singleOrNull { it.project == project }.shouldNotBeNull()
    }

    @Test
    fun `no add project chip when there is a project`() = runBlockingTest {

        val project = createProject(1)
        val timeEntry = createTimeEntry(2, projectId = project.id)
        val projects = listOf(project)
        val editableTimeEntry = EditableTimeEntry.fromSingle(timeEntry)

        val state = createInitialState(projects = projects, editableTimeEntry = editableTimeEntry)
        val chips = selector.select(state)

        chips.filterIsInstance<ChipViewModel.AddProject>().shouldBeEmpty()
    }

    @Test
    fun `a tag chip for each tag in the editable time entry`() = runBlockingTest {

        val tags = (1L..10L).map { Tag(it, "Tag $it", 1) }
        val timeEntry = createTimeEntry(2, tags = tags.map { it.id })
        val editableTimeEntry = EditableTimeEntry.fromSingle(timeEntry)

        val state = createInitialState(tags = tags, editableTimeEntry = editableTimeEntry)
        val chips = selector.select(state)

        chips.filterIsInstance<ChipViewModel.Tag>().all { tags.contains(it.tag) }.shouldBeTrue()
    }

    @Test
    fun `an add tag chip when there are tags`() = runBlockingTest {

        val tags = (1L..10L).map { Tag(it, "Tag $it", 1) }
        val timeEntry = createTimeEntry(2, tags = tags.map { it.id })
        val editableTimeEntry = EditableTimeEntry.fromSingle(timeEntry)

        val state = createInitialState(tags = tags, editableTimeEntry = editableTimeEntry)
        val chips = selector.select(state)

        chips.any { it is ChipViewModel.AddTag && it.text == "$tagToken $tagLabel" }.shouldBeTrue()
    }

    @Test
    fun `an add tag chip when there are no tags`() = runBlockingTest {

        val timeEntry = createTimeEntry(2, tags = emptyList())
        val editableTimeEntry = EditableTimeEntry.fromSingle(timeEntry)

        val state = createInitialState(editableTimeEntry = editableTimeEntry)
        val chips = selector.select(state)
        chips.any { it is ChipViewModel.AddTag && it.text == "$tagToken $tagLabel" }.shouldBeTrue()
    }
}
