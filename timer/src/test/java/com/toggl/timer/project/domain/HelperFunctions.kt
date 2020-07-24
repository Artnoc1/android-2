package com.toggl.timer.project.domain

import com.toggl.architecture.DispatcherProvider
import com.toggl.common.feature.navigation.backStackOf
import com.toggl.models.common.AutocompleteSuggestion
import com.toggl.models.domain.Client
import com.toggl.models.domain.Project
import com.toggl.models.domain.Task
import com.toggl.models.domain.EditableProject
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.models.domain.Workspace
import com.toggl.models.validation.HSVColor
import com.toggl.repository.interfaces.ClientRepository
import com.toggl.repository.interfaces.ProjectRepository
import io.mockk.mockk

fun createInitialState(
    editableProject: EditableProject = EditableProject.empty(1),
    projects: List<Project> = listOf(),
    workspaces: List<Workspace> = listOf(),
    clients: List<Client> = listOf(),
    customColor: HSVColor = HSVColor.defaultCustomColor,
    editableTimeEntry: EditableTimeEntry = EditableTimeEntry.empty(1),
    autocompleteQuery: ProjectAutocompleteQuery = ProjectAutocompleteQuery.None,
    autocompleteSuggestions: List<AutocompleteSuggestion.ProjectSuggestions> = emptyList()
) = ProjectState(
    editableProject = editableProject,
    projects = projects.associateBy { it.id },
    workspaces = workspaces.associateBy { it.id },
    clients = clients.associateBy { it.id },
    customColor = customColor,
    editableTimeEntry = editableTimeEntry,
    cursorPosition = editableTimeEntry.description.length,
    autocompleteQuery = autocompleteQuery,
    autocompleteSuggestions = autocompleteSuggestions,
    backStack = backStackOf()
)

fun createProjectReducer(
    projectRepository: ProjectRepository = mockk(),
    clientRepository: ClientRepository = mockk(),
    dispatcherProvider: DispatcherProvider = mockk()
): ProjectReducer =
    ProjectReducer(
        repository = projectRepository,
        clientRepository = clientRepository,
        dispatcherProvider = dispatcherProvider
    )

fun createProject(
    id: Long,
    name: String = "Project",
    color: String = "#1e1e1e",
    active: Boolean = true,
    isPrivate: Boolean = false,
    billable: Boolean? = null,
    workspaceId: Long = 1,
    clientId: Long? = null
) = Project(
    id,
    name,
    color,
    active,
    isPrivate,
    billable,
    workspaceId,
    clientId
)

fun createTask(
    id: Long,
    name: String = "Task",
    active: Boolean = true,
    projectId: Long = 1,
    workspaceId: Long = 1,
    userId: Long? = null
) = Task(
    id,
    name,
    active,
    projectId,
    workspaceId,
    userId
)

fun EditableProject.Companion.createInvalid() =
    EditableProject(
        name = "Project 1",
        color = "#123123",
        active = true,
        isPrivate = true,
        billable = null,
        workspaceId = 1,
        clientId = 1
    )

fun EditableProject.Companion.createValidBecauseWorkspacesAreDifferent() =
    EditableProject(
        name = "Project 1",
        color = "#123123",
        active = true,
        isPrivate = true,
        billable = null,
        workspaceId = 2,
        clientId = 1
    )

fun EditableProject.Companion.createValidBecauseWorkspacesAndClientsAreDifferent() =
    EditableProject(
        name = "Project 1",
        color = "#123123",
        active = true,
        isPrivate = true,
        billable = null,
        workspaceId = 2,
        clientId = 2
    )

fun EditableProject.Companion.createValidBecauseWorkspacesNamesAndClientsAreDifferent() =
    EditableProject(
        name = "Project 4",
        color = "#123123",
        active = true,
        isPrivate = true,
        billable = null,
        workspaceId = 2,
        clientId = 2
    )

fun EditableProject.Companion.createValidBecauseNamesAreDifferent() =
    EditableProject(
        name = "Project 4",
        color = "#123123",
        active = true,
        isPrivate = true,
        billable = null,
        workspaceId = 1,
        clientId = 1
    )

fun EditableProject.Companion.createValidBecauseNamesAndClientsAreDifferent() =
    EditableProject(
        name = "Project 4",
        color = "#123123",
        active = true,
        isPrivate = true,
        billable = null,
        workspaceId = 1,
        clientId = 2
    )

fun EditableProject.Companion.createValidBecauseClientsAreDifferent() =
    EditableProject(
        name = "Project 1",
        color = "#123123",
        active = true,
        isPrivate = true,
        billable = null,
        workspaceId = 1,
        clientId = 2
    )
