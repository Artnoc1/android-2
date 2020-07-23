package com.toggl.domain.mappings

import com.toggl.architecture.Loadable
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import com.toggl.settings.domain.SettingsAction
import com.toggl.settings.domain.SettingsState

fun mapAppStateToSettingsState(appState: AppState): SettingsState? {
    val user = appState.user() ?: return null
    return SettingsState(
        user = user,
        userPreferences = appState.userPreferences,
        workspaces = appState.workspaces,
        shouldRequestCalendarPermission = appState.shouldRequestCalendarPermission,
        localState = appState.settingsLocalState,
        backStack = appState.backStack
    )
}

fun mapSettingsStateToAppState(appState: AppState, settingsState: SettingsState?): AppState =
    settingsState?.run {
        appState.copy(
            user = Loadable.Loaded(user),
            userPreferences = userPreferences,
            workspaces = workspaces,
            shouldRequestCalendarPermission = shouldRequestCalendarPermission,
            settingsLocalState = localState,
            backStack = backStack
        )
    } ?: appState

fun mapSettingsActionToAppAction(settingsAction: SettingsAction): AppAction =
    AppAction.Settings(settingsAction)