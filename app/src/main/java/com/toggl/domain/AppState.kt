package com.toggl.domain

import com.toggl.architecture.Loadable
import com.toggl.calendar.common.domain.CalendarState
import com.toggl.common.feature.navigation.BackStack
import com.toggl.common.feature.navigation.BackStackAwareState
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.backStackOf
import com.toggl.common.feature.navigation.pop
import com.toggl.common.feature.services.calendar.CalendarEvent
import com.toggl.models.domain.Client
import com.toggl.models.domain.Project
import com.toggl.models.domain.Tag
import com.toggl.models.domain.Task
import com.toggl.models.domain.TimeEntry
import com.toggl.models.domain.User
import com.toggl.models.domain.UserPreferences
import com.toggl.models.domain.Workspace
import com.toggl.onboarding.common.domain.OnboardingState
import com.toggl.settings.domain.SettingsState
import com.toggl.timer.common.domain.TimerState

data class AppState(
    val user: Loadable<User> = Loadable.Uninitialized,
    val userPreferences: UserPreferences = UserPreferences.default,
    val workspaces: Map<Long, Workspace> = mapOf(),
    val projects: Map<Long, Project> = mapOf(),
    val tasks: Map<Long, Task> = mapOf(),
    val clients: Map<Long, Client> = mapOf(),
    val tags: Map<Long, Tag> = mapOf(),
    val timeEntries: Map<Long, TimeEntry> = mapOf(),
    val backStack: BackStack = backStackOf(Route.Timer),
    val calendarPermissionWasGranted: Boolean = false,
    val shouldRequestCalendarPermission: Boolean = false,
    val calendarEvents: Map<String, CalendarEvent> = mapOf(),
    val onboardingLocalState: OnboardingState.LocalState = OnboardingState.LocalState(),
    val timerLocalState: TimerState.LocalState = TimerState.LocalState(),
    val calendarLocalState: CalendarState.LocalState = CalendarState.LocalState(),
    val settingsLocalState: SettingsState.LocalState = SettingsState.LocalState()
) : BackStackAwareState<AppState> {
    override fun popBackStack(): AppState =
        copy(backStack = backStack.pop())
}
