package com.toggl.common.feature.navigation

import android.net.Uri
import com.toggl.common.DeepLinkUrls
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.models.domain.EditableProject
import com.toggl.models.domain.EditableTimeEntry

sealed class Route {
    object Onboarding : Route()
    object Timer : Route()
    data class StartEdit(override val parameter: EditableTimeEntry) : Route(), ParameterRoute<EditableTimeEntry>
    data class Project(override val parameter: EditableProject) : Route(), ParameterRoute<EditableProject>
    data class ContextualMenu(override val parameter: SelectedCalendarItem) : Route(), ParameterRoute<SelectedCalendarItem>
    object Settings : Route()
}

interface ParameterRoute<P> {
    val parameter: P
}

fun Route.isSameTypeAs(otherRoute: Route) =
    when (this) {
        Route.Onboarding -> otherRoute is Route.Onboarding
        Route.Timer -> otherRoute is Route.Timer
        is Route.StartEdit -> otherRoute is Route.StartEdit
        is Route.Project -> otherRoute is Route.Project
        is Route.ContextualMenu -> otherRoute is Route.ContextualMenu
        Route.Settings -> otherRoute is Route.Settings
    }

fun Route.deepLink(deepLinks: DeepLinkUrls): Uri {
    return when (this) {
        Route.Onboarding -> deepLinks.onboarding
        Route.Timer -> deepLinks.timeEntriesLog
        is Route.StartEdit -> deepLinks.startEditDialog
        is Route.Project -> deepLinks.projectDialog
        is Route.ContextualMenu -> deepLinks.contextualMenu
        Route.Settings -> deepLinks.settings
    }
}