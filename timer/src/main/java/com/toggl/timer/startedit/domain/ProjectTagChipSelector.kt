package com.toggl.timer.startedit.domain

import com.toggl.architecture.core.Selector
import com.toggl.common.Constants.AutoCompleteSuggestions.projectToken
import com.toggl.common.Constants.AutoCompleteSuggestions.tagToken
import com.toggl.timer.exceptions.TagDoesNotExistException
import com.toggl.timer.startedit.ui.chips.ChipViewModel
import javax.inject.Singleton

@Singleton
class ProjectTagChipSelector(
    private val addProjectLabel: String,
    private val addTagLabel: String
) : Selector<StartEditState, List<ChipViewModel>> {

    override suspend fun select(state: StartEditState): List<ChipViewModel> {

        val editableTimeEntry = state.editableTimeEntry

        return sequence {
            val projectId = editableTimeEntry.projectId
            val project = state.projects.getOrDefault(projectId, null)
            if (project != null) {
                yield(ChipViewModel.Project(project))
            } else {
                yield(ChipViewModel.AddProject("$projectToken $addProjectLabel"))
            }

            for (tagId in editableTimeEntry.tagIds) {
                val tag = state.tags[tagId] ?: throw TagDoesNotExistException()
                yield(ChipViewModel.Tag(tag))
            }

            yield(ChipViewModel.AddTag("$tagToken $addTagLabel"))
        }.toList()
    }
}