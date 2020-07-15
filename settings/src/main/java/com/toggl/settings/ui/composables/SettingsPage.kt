package com.toggl.settings.ui.composables

import androidx.compose.Composable
import androidx.compose.collectAsState
import androidx.compose.getValue
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.foundation.lazy.LazyColumnItems
import androidx.ui.layout.Arrangement
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.RowScope
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.height
import androidx.ui.layout.padding
import androidx.ui.material.Divider
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Scaffold
import androidx.ui.material.TopAppBar
import androidx.ui.material.ripple.RippleIndication
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.dp
import com.toggl.models.domain.SettingsType
import com.toggl.settings.compose.ThemedPreview
import com.toggl.settings.compose.theme.TogglTheme
import com.toggl.settings.compose.theme.grid_1
import com.toggl.settings.compose.theme.grid_2
import com.toggl.settings.domain.SettingsAction
import com.toggl.settings.domain.SettingsSectionViewModel
import com.toggl.settings.domain.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Composable
fun SettingsPage(
    sectionsState: Flow<List<SettingsSectionViewModel>>,
    pageTitle: String,
    dispatcher: (SettingsAction) -> Unit = {}
) {
    val observableSectionState by sectionsState.collectAsState(listOf())
    TogglTheme {
        SettingsPageContent(observableSectionState, pageTitle, dispatcher)
    }
}

@ExperimentalCoroutinesApi
@Composable
fun SettingsPageContent(
    sectionsState: List<SettingsSectionViewModel>,
    pageTitle: String,
    dispatcher: (SettingsAction) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
                title = { Text(text = pageTitle) }
            )
        },
        bodyContent = { innerPadding ->
            SectionList(
                sectionsList = sectionsState,
                dispatcher = dispatcher,
                modifier = Modifier.padding(innerPadding)
            )
        }
    )
}

@Composable
private fun SectionList(
    sectionsList: List<SettingsSectionViewModel>,
    dispatcher: (SettingsAction) -> Unit,
    modifier: Modifier
) {
    LazyColumnItems(sectionsList) { section ->
        Section(
            section = section,
            dispatcher = dispatcher,
            modifier = modifier
        )
    }
}

@Composable
private fun Section(
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

        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = .3f))

        for (settingsOption in section.settingsOptions) {
            val onClickAction = { dispatcher(SettingsAction.SettingTapped(settingsOption.settingsType)) }
            SettingsRow(
                modifier = Modifier.clickable(
                    indication = RippleIndication(),
                    onClick = onClickAction
                )
            ) {
                when (settingsOption) {
                    is SettingsViewModel.Toggle -> ToggleSetting(settingsOption, onClickAction)
                    is SettingsViewModel.ListChoice -> ListChoiceSetting(settingsOption)
                    is SettingsViewModel.SubPage -> SubPageSetting(settingsOption)
                    is SettingsViewModel.ActionRow -> ActionSetting(settingsOption)
                }
            }
            Divider(color = MaterialTheme.colors.onSurface.copy(alpha = .2f))
        }
    }
}

@Composable
internal fun SettingsRow(
    modifier: Modifier = Modifier,
    children: @Composable RowScope.() -> Unit
) {
    val rowModifier = Modifier
        .plus(modifier)
        .height(56.dp)
        .padding(grid_1)
        .fillMaxWidth()

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalGravity = Alignment.CenterVertically
    ) {
        children()
    }
}

@ExperimentalCoroutinesApi
@Preview("Settings page light theme")
@Composable
fun PreviewSettingsPageLight() {
    ThemedPreview(false) {
        SettingsPageContent(settingsListPreviewData, "Settings")
    }
}

@ExperimentalCoroutinesApi
@Preview("Settings page dark theme")
@Composable
fun PreviewSettingsPageDark() {
    ThemedPreview(true) {
        SettingsPageContent(settingsListPreviewData, "Settings")
    }
}

val settingsListPreviewData: List<SettingsSectionViewModel> = listOf(
    SettingsSectionViewModel(
        title = "Your Profile",
        settingsOptions = listOf(
            SettingsViewModel.ListChoice("Name", SettingsType.Name, "Semanticer"),
            SettingsViewModel.ListChoice("Email Address", SettingsType.Email, "test@test.com"),
            SettingsViewModel.ListChoice("Workspace", SettingsType.Workspace, "Semanticer's workspace")
        )
    ),
    SettingsSectionViewModel(
        title = "Date and Time",
        settingsOptions = listOf(
            SettingsViewModel.ListChoice("Date format", SettingsType.DateFormat, "DD/MM/YYYY"),
            SettingsViewModel.Toggle("Use 24-hour clock", SettingsType.TwentyFourHourClock, false),
            SettingsViewModel.ListChoice("Duration format", SettingsType.DurationFormat, "Improved"),
            SettingsViewModel.ListChoice("First day of the week", SettingsType.FirstDayOfTheWeek, "Wednesday"),
            SettingsViewModel.Toggle("Group Similar time entries", SettingsType.GroupSimilar, false)
        )
    ),
    SettingsSectionViewModel(
        title = "Timer Defaults",
        settingsOptions = listOf(
            SettingsViewModel.Toggle("Cell Swipe Actions", SettingsType.CellSwipe, false),
            SettingsViewModel.Toggle("Manual mode", SettingsType.ManualMode, false)
        )
    ),
    SettingsSectionViewModel(
        title = "Calendar",
        settingsOptions = listOf(
            SettingsViewModel.SubPage("Calendar Settings", SettingsType.CalendarSettings),
            SettingsViewModel.SubPage("Smart alerts", SettingsType.SmartAlert)
        )
    ),
    SettingsSectionViewModel(
        title = "General",
        settingsOptions = listOf(
            SettingsViewModel.SubPage("Submit Feedback", SettingsType.SubmitFeedback),
            SettingsViewModel.SubPage("About", SettingsType.About),
            SettingsViewModel.SubPage("Help", SettingsType.Help)

        )
    ),
    SettingsSectionViewModel(
        title = "Sync",
        settingsOptions = listOf(
            SettingsViewModel.ActionRow("Sign Out", SettingsType.SignOut)
        )
    )
)