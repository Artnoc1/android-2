package com.toggl.common.feature.navigation

import android.net.Uri
import com.toggl.common.DeepLinkUrls
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.models.domain.EditableProject
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.models.domain.SettingsType

sealed class Route {
    object Welcome : Route()
    object Login : Route()
    object Timer : Route()
    data class StartEdit(override val parameter: EditableTimeEntry) : Route(), ParameterRoute<EditableTimeEntry>
    data class Project(override val parameter: EditableProject) : Route(), ParameterRoute<EditableProject>
    data class ContextualMenu(override val parameter: SelectedCalendarItem) : Route(), ParameterRoute<SelectedCalendarItem>
    object Settings : Route()
    data class SettingsDialog(override val parameter: SettingsType) : Route(), ParameterRoute<SettingsType>
    object CalendarSettings : Route()
    object Feedback : Route()
}

interface ParameterRoute<P> {
    val parameter: P
}

fun Route.isSameTypeAs(otherRoute: Route) =
    when (this) {
        Route.Welcome -> otherRoute is Route.Welcome
        Route.Login -> otherRoute is Route.Login
        Route.Timer -> otherRoute is Route.Timer
        is Route.StartEdit -> otherRoute is Route.StartEdit
        is Route.Project -> otherRoute is Route.Project
        is Route.ContextualMenu -> otherRoute is Route.ContextualMenu
        Route.Settings -> otherRoute is Route.Settings
        is Route.SettingsDialog -> otherRoute is Route.SettingsDialog
        Route.CalendarSettings -> otherRoute is Route.CalendarSettings
        Route.Feedback -> otherRoute is Route.Feedback
    }

fun Route.deepLink(deepLinks: DeepLinkUrls): Uri {
    return when (this) {
        Route.Welcome -> deepLinks.welcome
        Route.Login -> deepLinks.login
        Route.Timer -> deepLinks.timeEntriesLog
        is Route.StartEdit -> deepLinks.startEditDialog
        is Route.Project -> deepLinks.projectDialog
        is Route.ContextualMenu -> deepLinks.contextualMenu
        is Route.SettingsDialog -> TODO()
        Route.Settings -> deepLinks.settings
        Route.CalendarSettings -> deepLinks.calendarSettings
        Route.Feedback -> deepLinks.submitFeedback
    }
}