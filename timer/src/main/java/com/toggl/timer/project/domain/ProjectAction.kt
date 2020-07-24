package com.toggl.timer.project.domain

import com.toggl.models.domain.Client
import com.toggl.models.domain.Project
import com.toggl.models.domain.Workspace

sealed class ProjectAction {
    data class NameEntered(val name: String) : ProjectAction()
    object DoneButtonTapped : ProjectAction()
    data class ProjectCreated(val project: Project) : ProjectAction()
    object PrivateProjectSwitchTapped : ProjectAction()
    object CloseButtonTapped : ProjectAction()
    object DialogDismissed : ProjectAction()
    data class ColorValueChanged(val value: Float) : ProjectAction()
    data class ColorHueSaturationChanged(val hue: Float, val saturation: Float) : ProjectAction()
    data class ColorPicked(val color: String) : ProjectAction()
    data class WorkspacePicked(val workspace: Workspace) : ProjectAction()
    data class ClientPicked(val client: Client?) : ProjectAction()
    data class CreateClientSuggestionTapped(val name: String) : ProjectAction()
    data class ClientCreated(val client: Client) : ProjectAction()
    object Close : ProjectAction()
    data class AutocompleteDescriptionEntered(val query: ProjectAutocompleteQuery) : ProjectAction()
    companion object
}

fun ProjectAction.formatForDebug() =
    when (this) {
        is ProjectAction.NameEntered -> "Project name entered: $name"
        is ProjectAction.DoneButtonTapped -> "Project creation confirmed"
        is ProjectAction.ProjectCreated -> "Project created with id ${project.id}"
        is ProjectAction.PrivateProjectSwitchTapped -> "Private project switch tapped"
        ProjectAction.CloseButtonTapped -> "Close button tapped"
        ProjectAction.DialogDismissed -> "Dialog dismissed by swipping"
        is ProjectAction.ColorValueChanged -> "Color value changed to $value"
        is ProjectAction.ColorHueSaturationChanged -> "Color hue changed to $hue and saturation to $saturation"
        is ProjectAction.ColorPicked -> "Selected color $color"
        is ProjectAction.WorkspacePicked -> "Selected workspace with id ${workspace.id}"
        is ProjectAction.ClientPicked -> client?.let { "Selected client wit id ${client.id}" } ?: "Selected No client"
        is ProjectAction.CreateClientSuggestionTapped -> "Tapped create client suggestion with name $name"
        ProjectAction.Close -> "Project view closed"
        is ProjectAction.ClientCreated -> "Created client with id ${client.id}"
        is ProjectAction.AutocompleteDescriptionEntered -> "Project auto complete description entered $query"
    }