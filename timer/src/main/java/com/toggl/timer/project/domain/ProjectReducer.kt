package com.toggl.timer.project.domain

import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.core.Effect
import com.toggl.architecture.core.MutableValue
import com.toggl.architecture.core.Reducer
import com.toggl.architecture.extensions.effect
import com.toggl.architecture.extensions.effectOf
import com.toggl.architecture.extensions.effects
import com.toggl.common.Constants.AutoCompleteSuggestions.projectToken
import com.toggl.common.feature.extensions.mutateWithoutEffects
import com.toggl.common.feature.extensions.returnEffect
import com.toggl.common.feature.extensions.toHex
import com.toggl.common.feature.navigation.popBackStackWithoutEffects
import com.toggl.models.common.AutocompleteSuggestion.ProjectSuggestions
import com.toggl.models.domain.EditableProject
import com.toggl.models.domain.isValid
import com.toggl.repository.extensions.toDto
import com.toggl.repository.interfaces.ClientRepository
import com.toggl.repository.interfaces.ProjectRepository
import com.toggl.timer.startedit.util.findTokenAndQueryMatchesForAutocomplete
import javax.inject.Inject

class ProjectReducer @Inject constructor(
    private val repository: ProjectRepository,
    private val clientRepository: ClientRepository,
    private val dispatcherProvider: DispatcherProvider
) : Reducer<ProjectState, ProjectAction> {

    override fun reduce(
        state: MutableValue<ProjectState>,
        action: ProjectAction
    ): List<Effect<ProjectAction>> =
        when (action) {
            ProjectAction.CloseButtonTapped,
            ProjectAction.DialogDismissed -> effectOf(ProjectAction.Close)
            is ProjectAction.NameEntered -> state.mutateWithoutEffects {
                copy(
                    editableProject = editableProject.copy(
                        name = action.name, error = EditableProject.ProjectError.None
                    )
                )
            }
            ProjectAction.DoneButtonTapped -> {
                val (project, projectCanBeCreated) = state.mapState {
                    val listOfProjects = projects.values
                    editableProject to editableProject.isValid(listOfProjects)
                }

                if (projectCanBeCreated) createProject(project)
                else state.mutateWithoutEffects {
                    copy(
                        editableProject = editableProject.copy(
                            error = EditableProject.ProjectError.ProjectAlreadyExists
                        )
                    )
                }
            }
            ProjectAction.PrivateProjectSwitchTapped ->
                state.mutateWithoutEffects {
                    copy(
                        editableProject = editableProject.copy(
                            isPrivate = !editableProject.isPrivate
                        )
                    )
                }
            is ProjectAction.ColorValueChanged -> {
                val newCustomColor = state().customColor.copy(value = action.value)
                state.mutate {
                    copy(customColor = newCustomColor)
                }
                effectOf(ProjectAction.ColorPicked(newCustomColor.toHex()))
            }
            is ProjectAction.ColorHueSaturationChanged -> {
                val newCustomColor = state().customColor.copy(hue = action.hue, saturation = action.saturation)
                state.mutate {
                    copy(customColor = newCustomColor)
                }
                effectOf(ProjectAction.ColorPicked(newCustomColor.toHex()))
            }
            is ProjectAction.ColorPicked -> state.mutateWithoutEffects {
                copy(editableProject = editableProject.copy(color = action.color))
            }
            is ProjectAction.WorkspacePicked -> state.mutateWithoutEffects {
                copy(editableProject = editableProject.copy(workspaceId = action.workspace.id))
            }
            is ProjectAction.ClientPicked -> state.mutateWithoutEffects {
                copy(
                    editableProject = editableProject.copy(
                        clientId = action.client?.id
                    )
                )
            }
            is ProjectAction.ProjectCreated -> state.mutate {
                val (token, currentQuery) = editableTimeEntry.description.findTokenAndQueryMatchesForAutocomplete(
                    projectToken,
                    cursorPosition
                )
                val delimiter = "$token$currentQuery"
                copy(
                    editableTimeEntry = editableTimeEntry.copy(
                        projectId = action.project.id,
                        description = editableTimeEntry.description.substringBeforeLast(delimiter)
                    )
                )
            } returnEffect effectOf(ProjectAction.Close)
            ProjectAction.Close -> state.popBackStackWithoutEffects()
            is ProjectAction.CreateClientSuggestionTapped -> effect(
                CreateClientEffect(
                    dispatcherProvider,
                    clientRepository,
                    action.name,
                    state().editableProject.workspaceId
                )
            )
            is ProjectAction.ClientCreated -> state.mutateWithoutEffects {
                copy(
                    clients = clients + (action.client.id to action.client),
                    editableProject = editableProject.copy(
                        clientId = action.client.id
                    )
                )
            }
            is ProjectAction.AutocompleteDescriptionEntered -> state.mutateWithoutEffects {
                copy(
                    autocompleteQuery = action.query,
                    autocompleteSuggestions = suggestionsFor(action.query)
                )
            }
        }

    private fun createProject(editableProject: EditableProject) = effects(
        CreateProjectEffect(editableProject.toDto(), repository, dispatcherProvider)
    )

    private fun ProjectState.suggestionsFor(autocompleteQuery: ProjectAutocompleteQuery): List<ProjectSuggestions> {
        return when (autocompleteQuery) {
            ProjectAutocompleteQuery.None -> emptyList()
            is ProjectAutocompleteQuery.WorkspaceQuery -> findWorkspacesWithNameOrReturnAll(autocompleteQuery.name)
            is ProjectAutocompleteQuery.ClientQuery -> generateClientSuggestionsForQuery(autocompleteQuery.name)
        }
    }

    private fun ProjectState.findWorkspacesWithNameOrReturnAll(query: String): List<ProjectSuggestions.Workspace> {
        val filteredWorkspaces = workspaces.values.filter { it.name.contains(query) }
        return filteredWorkspaces
            .toList(defaultWhenEmpty = workspaces.values)
            .map(ProjectSuggestions::Workspace)
    }

    private fun ProjectState.generateClientSuggestionsForQuery(query: String): List<ProjectSuggestions> {
        val noClientSuggestion = ProjectSuggestions.Client(null)
        val clientsOnCurrentWorkspace = clients.values.filter { it.workspaceId == editableProject.workspaceId }
        val filteredClients = clientsOnCurrentWorkspace.filter { it.name.contains(query) }
        val existingClientSuggestions = filteredClients
            .toList(defaultWhenEmpty = clientsOnCurrentWorkspace)
            .map { ProjectSuggestions.Client(it) }
        val hasNoClientWithExactNameOnWorkspace = clientsOnCurrentWorkspace.none { it.name == query }
        val suggestions = listOf(noClientSuggestion) + existingClientSuggestions
        return if (query.isNotBlank() && hasNoClientWithExactNameOnWorkspace)
            suggestions + ProjectSuggestions.CreateClient(query)
        else suggestions
    }

    private fun <T> Collection<T>.toList(defaultWhenEmpty: Collection<T>) =
        (if (this.isEmpty()) defaultWhenEmpty else this).toList()
}
