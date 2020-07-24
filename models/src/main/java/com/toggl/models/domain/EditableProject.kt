package com.toggl.models.domain

data class EditableProject(
    val name: String = "",
    val color: String = Project.defaultColors.random(),
    val active: Boolean = true,
    val isPrivate: Boolean = true,
    val billable: Boolean? = null,
    val workspaceId: Long,
    val clientId: Long? = null,
    val error: ProjectError = ProjectError.None
) {
    enum class ProjectError {
        None,
        ProjectAlreadyExists
    }

    companion object {
        fun empty(workspaceId: Long) = EditableProject(workspaceId = workspaceId)
    }
}

fun EditableProject.isValid(projects: Collection<Project>): Boolean =
    projects.none {
        it.name == name &&
        it.workspaceId == workspaceId &&
        it.clientId == clientId
    }