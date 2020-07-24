package com.toggl.settings.domain

import com.toggl.architecture.core.Selector
import com.toggl.common.feature.services.calendar.CalendarService
import com.toggl.common.services.permissions.PermissionCheckerService
import com.toggl.models.domain.SettingsType
import dagger.hilt.android.scopes.FragmentScoped
import javax.inject.Inject

@FragmentScoped
class CalendarSettingsSelector @Inject constructor(
    private val calendarService: CalendarService,
    private val permissionCheckerService: PermissionCheckerService
) : Selector<SettingsState, List<CalendarSettingsViewModel>> {
    override suspend fun select(state: SettingsState): List<CalendarSettingsViewModel> {

        val userCalendars = calendarService.getUserSelectedCalendars()
        val availableCalendars = calendarService.getAvailableCalendars()

        return sequence {
            val calendarIntegrationEnabled = permissionCheckerService.hasCalendarPermission()
                && state.userPreferences.calendarIntegrationEnabled

            yield(CalendarSettingsViewModel.IntegrationEnabled(calendarIntegrationEnabled))

            if (!calendarIntegrationEnabled) return@sequence

            val calendarSections = availableCalendars
                .groupBy { it.sourceName }
                .map { (groupName, calendars) ->
                    SettingsSectionViewModel(groupName, calendars.map {
                        SettingsViewModel.Toggle(
                            it.name,
                            SettingsType.Calendar(it.id),
                            userCalendars.contains(it)
                        )
                    })
                }

            yieldAll(calendarSections.map(CalendarSettingsViewModel::CalendarSection))
        }.toList()
    }
}