package com.toggl.settings.domain

import com.toggl.architecture.Loadable
import com.toggl.common.feature.navigation.BackStack
import com.toggl.models.domain.User
import com.toggl.models.domain.UserPreferences
import com.toggl.models.domain.Workspace

data class SettingsState(
    val user: User,
    val userPreferences: UserPreferences,
    val workspaces: Map<Long, Workspace>,
    val shouldRequestCalendarPermission: Boolean,
    val localState: LocalState,
    val backStack: BackStack
) {
    data class LocalState internal constructor(
        internal val sendFeedbackRequest: Loadable<Unit>
    ) {
        constructor(): this(sendFeedbackRequest = Loadable.Uninitialized)

        companion object
    }

    companion object
}