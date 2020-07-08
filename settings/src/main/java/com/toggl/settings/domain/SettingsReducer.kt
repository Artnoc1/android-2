package com.toggl.settings.domain

import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.core.Effect
import com.toggl.architecture.core.MutableValue
import com.toggl.architecture.core.Reducer
import com.toggl.architecture.extensions.effect
import com.toggl.common.feature.extensions.mutateWithoutEffects
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.push
import com.toggl.environment.services.permissions.PermissionCheckerService
import com.toggl.models.domain.UserPreferences
import com.toggl.repository.interfaces.SettingsRepository
import javax.inject.Inject

class SettingsReducer @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val permissionCheckerService: PermissionCheckerService,
    private val dispatcherProvider: DispatcherProvider
) : Reducer<SettingsState, SettingsAction> {

    override fun reduce(
        state: MutableValue<SettingsState>,
        action: SettingsAction
    ): List<Effect<SettingsAction>> =
        when (action) {
            is SettingsAction.UserPreferencesUpdated -> state.mutateWithoutEffects { copy(userPreferences = action.userPreferences) }
            is SettingsAction.SettingTapped -> state.mutateWithoutEffects {
                val editRoute = Route.SettingsEdit(action.selectedSetting)
                copy(backStack = backStack.push(editRoute))
            }
            is SettingsAction.ManualModeToggled -> state.updatePrefs { copy(manualModeEnabled = !manualModeEnabled) }
            is SettingsAction.Use24HourClockToggled -> state.updatePrefs { copy(twentyFourHourClockEnabled = !twentyFourHourClockEnabled) }
            is SettingsAction.CellSwipeActionsToggled -> state.updatePrefs { copy(cellSwipeActionsEnabled = !cellSwipeActionsEnabled) }
            is SettingsAction.GroupSimilarTimeEntriesToggled -> state.updatePrefs { copy(groupSimilarTimeEntriesEnabled = !groupSimilarTimeEntriesEnabled) }
            is SettingsAction.WorkspaceSelected -> state.updatePrefs { copy(selectedWorkspaceId = action.selectedWorkspaceId) }
            is SettingsAction.DateFormatSelected -> state.updatePrefs { copy(dateFormat = action.dateFormat) }
            is SettingsAction.DurationFormatSelected -> state.updatePrefs { copy(durationFormat = action.durationFormat) }
            is SettingsAction.FirstDayOfTheWeekSelected -> state.updatePrefs { copy(firstDayOfTheWeek = action.firstDayOfTheWeek) }
            is SettingsAction.SmartAlertsOptionSelected -> state.updatePrefs { copy(smartAlertsOption = action.smartAlertsOption) }
            is SettingsAction.UserCalendarIntegrationToggled -> state.updatePrefs {
                if (calendarIds.contains(action.calendarId)) copy(calendarIds = calendarIds - action.calendarId)
                else copy(calendarIds = calendarIds + action.calendarId)
            }
            is SettingsAction.AllowCalendarAccessToggled -> state.handleAllowCalendarAccessToggled()
            is SettingsAction.CalendarPermissionRequested -> state.mutateWithoutEffects { copy(shouldRequestCalendarPermission = true) }
            is SettingsAction.CalendarPermissionReceived -> state.mutateWithoutEffects { copy(shouldRequestCalendarPermission = false) }
            is SettingsAction.UpdateEmail -> state.mutateWithoutEffects { copy(user = user.copy(email = action.email)) }
            is SettingsAction.UpdateName -> state.mutateWithoutEffects { copy(user = user.copy(name = action.name)) }
        }

    private fun MutableValue<SettingsState>.handleAllowCalendarAccessToggled(): List<Effect<SettingsAction>> {
        val updatePrefsEffects = updatePrefs {
            copy(
                calendarIntegrationEnabled = !calendarIntegrationEnabled,
                calendarIds = if (calendarIntegrationEnabled) calendarIds else emptyList()
            )
        }
        return if (!this().userPreferences.calendarIntegrationEnabled && !permissionCheckerService.hasCalendarPermission())
            updatePrefsEffects + RequestCalendarPermissionEffect()
        else
            updatePrefsEffects
    }

    private fun MutableValue<SettingsState>.updatePrefs(updateBlock: UserPreferences.() -> UserPreferences) =
        effect(UpdateUserPreferencesEffect(this().userPreferences.updateBlock(), settingsRepository, dispatcherProvider))
}
