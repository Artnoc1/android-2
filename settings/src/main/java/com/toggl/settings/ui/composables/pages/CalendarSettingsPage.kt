package com.toggl.settings.ui.composables.pages

import androidx.compose.Composable
import androidx.compose.collectAsState
import androidx.compose.getValue
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.foundation.lazy.LazyColumnItems
import androidx.ui.layout.Column
import androidx.ui.layout.padding
import androidx.ui.material.IconButton
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Scaffold
import androidx.ui.material.TopAppBar
import androidx.ui.material.icons.filled.ArrowBack
import androidx.ui.res.stringResource
import androidx.ui.tooling.preview.Preview
import com.toggl.models.domain.SettingsType
import com.toggl.settings.R
import com.toggl.settings.compose.ThemedPreview
import com.toggl.settings.compose.theme.TogglTheme
import com.toggl.settings.compose.theme.grid_2
import com.toggl.settings.domain.CalendarSettingsViewModel
import com.toggl.settings.domain.SettingsAction
import com.toggl.settings.domain.SettingsSectionViewModel
import com.toggl.settings.domain.SettingsViewModel
import com.toggl.settings.ui.composables.Section
import com.toggl.settings.ui.composables.SettingsRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

@ExperimentalCoroutinesApi
@Composable
fun CalendarSettingsPage(
    calendarSettingsViewModels: Flow<List<CalendarSettingsViewModel>>,
    dispatcher: (SettingsAction) -> Unit
) {
    val observableState by calendarSettingsViewModels.collectAsState(listOf())
    TogglTheme {
        CalendarSettingsPageContent(
            observableState,
            dispatcher
        )
    }
}

@Composable
fun CalendarSettingsPageContent(
    calendarSettingsViewModels: List<CalendarSettingsViewModel>,
    dispatcher: (SettingsAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
                title = { Text(text = stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        androidx.ui.foundation.Icon(androidx.ui.material.icons.Icons.Filled.ArrowBack)
                    }
                }
            )
        },
        bodyContent = {
            LazyColumnItems(calendarSettingsViewModels) { viewModel ->
                when (viewModel) {
                    is CalendarSettingsViewModel.IntegrationEnabled ->
                        LinkCalendarsSection(viewModel.accessGranted, dispatcher)
                    is CalendarSettingsViewModel.CalendarSection ->
                        Section(section = viewModel.section, dispatcher = dispatcher)
                }
            }
        }
    )
}

@Composable
fun LinkCalendarsSection(
    accessGranted: Boolean,
    dispatcher: (SettingsAction) -> Unit
) {

    val setting = SettingsViewModel.Toggle(
        stringResource(R.string.allow_calendar_access),
        SettingsType.AllowCalendarAccess,
        accessGranted
    )
    Column {
        SettingsRow(setting, dispatcher)
        Text(
            text = stringResource(R.string.allow_calendar_message),
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(grid_2)
        )
    }
}

@Composable
@Preview("Settings page light theme")
fun PreviewCalendarSettingsPageLight() {
    ThemedPreview(false) {
        CalendarSettingsPageContent(calendarSettingsPreviewData) { }
    }
}

@Composable
@Preview("Settings page dark theme")
fun PreviewCalendarSettingsPageDark() {
    ThemedPreview(true) {
        CalendarSettingsPageContent(calendarSettingsPreviewData) { }
    }
}

val calendarSettingsPreviewData: List<CalendarSettingsViewModel> = listOf(
    CalendarSettingsViewModel.IntegrationEnabled(false),
    CalendarSettingsViewModel.CalendarSection(SettingsSectionViewModel("someone@toggl.com", listOf(
        SettingsViewModel.Toggle("Meetings", SettingsType.Calendar("123"), true),
        SettingsViewModel.Toggle("Peer Reviews", SettingsType.Calendar("123"), false),
        SettingsViewModel.Toggle("Peer Reviews", SettingsType.Calendar("123"), false)
    ))),
    CalendarSettingsViewModel.CalendarSection(SettingsSectionViewModel("team@toggl.com", listOf(
        SettingsViewModel.Toggle("Meetings", SettingsType.Calendar("123"), false),
        SettingsViewModel.Toggle("Peer Reviews", SettingsType.Calendar("123"), true),
        SettingsViewModel.Toggle("Peer Reviews", SettingsType.Calendar("123"), false)
    )))
)