package com.toggl.settings.domain

import android.content.Context
import com.toggl.architecture.core.Selector
import com.toggl.common.feature.navigation.getRouteParam
import com.toggl.models.domain.DateFormat
import com.toggl.models.domain.DurationFormat
import com.toggl.models.domain.SettingsType
import com.toggl.models.domain.UserPreferences
import com.toggl.models.domain.Workspace
import com.toggl.settings.R
import java.time.DayOfWeek
import javax.inject.Inject

class SingleChoiceSettingSelector @Inject constructor(
    private val context: Context
) : Selector<SettingsState, SingleChoiceSettingViewModel> {
    override suspend fun select(state: SettingsState) =
        state.backStack.getRouteParam<SettingsType>()?.toViewModel(state.userPreferences, state.workspaces)
            ?: SingleChoiceSettingViewModel.Empty

    private fun SettingsType.toViewModel(
        userPreferences: UserPreferences,
        workspaces: Map<Long, Workspace>
    ): SingleChoiceSettingViewModel {
        val settingHeader: String
        val settingChoiceListItems: List<ChoiceListItem>

        when (this) {
            SettingsType.DateFormat -> {
                settingHeader = context.getString(R.string.date_format)
                settingChoiceListItems =
                    DateFormat.values().map {
                        ChoiceListItem(
                            it.label,
                            userPreferences.dateFormat == it,
                            selectedActions = listOf(
                                SettingsAction.DateFormatSelected(it)
                            )
                        )
                    }
            }
            SettingsType.DurationFormat -> {
                settingHeader = context.getString(R.string.duration_format)
                settingChoiceListItems =
                    DurationFormat.values().map {
                        ChoiceListItem(
                            it.getTranslatedRepresentation(context),
                            userPreferences.durationFormat == it,
                            selectedActions = listOf(
                                SettingsAction.DurationFormatSelected(it)
                            )
                        )
                    }
            }
            SettingsType.FirstDayOfTheWeek -> {
                settingHeader = context.getString(R.string.first_day_of_the_week)
                settingChoiceListItems =
                    DayOfWeek.values().map {
                        ChoiceListItem(
                            it.getTranslatedRepresentation(context),
                            userPreferences.firstDayOfTheWeek == it,
                            selectedActions = listOf(
                                SettingsAction.FirstDayOfTheWeekSelected(it)
                            )
                        )
                    }
            }
            SettingsType.Workspace -> {
                settingHeader = context.getString(R.string.default_workspace)
                settingChoiceListItems = workspaces.entries.map { entry ->
                    ChoiceListItem(
                        entry.value.name,
                        userPreferences.selectedWorkspaceId == entry.key,
                        selectedActions = listOf(
                            SettingsAction.WorkspaceSelected(entry.key)
                        )
                    )
                }
            }
            else -> {
                settingHeader = "UNKNOWN"
                settingChoiceListItems = listOf()
            }
        }

        return SingleChoiceSettingViewModel(settingHeader, settingChoiceListItems, SettingsAction.DialogDismissed)
    }
}