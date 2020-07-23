package com.toggl.settings.ui.composables

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.height
import androidx.ui.layout.padding
import androidx.ui.material.MaterialTheme
import androidx.ui.material.ripple.RippleIndication
import androidx.ui.unit.dp
import com.toggl.models.domain.SettingsType
import com.toggl.settings.compose.theme.grid_1
import com.toggl.settings.compose.theme.grid_2
import com.toggl.settings.domain.SettingsAction
import com.toggl.settings.domain.SettingsSectionViewModel
import com.toggl.settings.domain.SettingsViewModel

@Composable
internal fun Section(
    section: SettingsSectionViewModel,
    modifier: Modifier = Modifier,
    dispatcher: (SettingsAction) -> Unit
) {
    val columnModifier = Modifier
        .padding(top = grid_1, bottom = grid_1)
        .plus(modifier)

    Column(modifier = columnModifier) {
        Text(
            text = section.title,
            modifier = Modifier.fillMaxWidth().padding(grid_2),
            style = MaterialTheme.typography.subtitle1
        )

        for (settingsOption in section.settingsOptions) {
            SettingsRow(settingsOption, dispatcher)
        }
    }
}

@Composable
internal fun SettingsRow(
    setting: SettingsViewModel,
    dispatcher: (SettingsAction) -> Unit
) {

    val onClickAction = {
        val settingsType = setting.settingsType
        dispatcher(
            when (settingsType) {
                SettingsType.Name -> TODO()
                SettingsType.Email -> TODO()
                SettingsType.Workspace -> TODO()
                SettingsType.DateFormat -> TODO()
                SettingsType.TwentyFourHourClock -> SettingsAction.Use24HourClockToggled
                SettingsType.DurationFormat -> TODO()
                SettingsType.FirstDayOfTheWeek -> TODO()
                SettingsType.GroupSimilar -> SettingsAction.GroupSimilarTimeEntriesToggled
                SettingsType.CellSwipe -> SettingsAction.CellSwipeActionsToggled
                SettingsType.ManualMode -> SettingsAction.ManualModeToggled
                SettingsType.CalendarSettings -> SettingsAction.OpenCalendarSettingsTapped
                SettingsType.SmartAlert -> TODO()
                SettingsType.SubmitFeedback -> TODO()
                SettingsType.About -> TODO()
                SettingsType.PrivacyPolicy -> TODO()
                SettingsType.TermsOfService -> TODO()
                SettingsType.Licenses -> TODO()
                SettingsType.Help -> TODO()
                SettingsType.SignOut -> SettingsAction.SignOutTapped
                SettingsType.AllowCalendarAccess -> SettingsAction.AllowCalendarAccessToggled
                is SettingsType.Calendar -> SettingsAction.UserCalendarIntegrationToggled(settingsType.id)
            }
        )
    }

    val rowModifier = Modifier
        .height(48.dp)
        .fillMaxWidth() + Modifier.clickable(
            indication = RippleIndication(),
            onClick = onClickAction
        )

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalGravity = Alignment.CenterVertically
    ) {
        when (setting) {
            is SettingsViewModel.Toggle -> ToggleSetting(setting, onClickAction)
            is SettingsViewModel.ListChoice -> ListChoiceSetting(setting)
            is SettingsViewModel.SubPage -> SubPageSetting(setting)
            is SettingsViewModel.ActionRow -> ActionSetting(setting)
        }
    }
}
