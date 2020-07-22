package com.toggl.settings.ui.composables.pages

import androidx.compose.Composable
import androidx.compose.collectAsState
import androidx.compose.getValue
import androidx.ui.core.ContextAmbient
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.foundation.lazy.LazyColumnItems
import androidx.ui.layout.Column
import androidx.ui.layout.padding
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Scaffold
import androidx.ui.material.TopAppBar
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
    pageTitle: String,
    dispatcher: (SettingsAction) -> Unit
) {
    val observableState by calendarSettingsViewModels.collectAsState(listOf())
    TogglTheme {
        CalendarSettingsPageContent(
            observableState,
            pageTitle,
            dispatcher
        )
    }
}

@Composable
fun CalendarSettingsPageContent(
    calendarSettingsViewModels: List<CalendarSettingsViewModel>,
    pageTitle: String,
    dispatcher: (SettingsAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
                title = { Text(text = pageTitle) }
            )
        },
        bodyContent = {
            LazyColumnItems(calendarSettingsViewModels) { viewModel ->
                when(viewModel) {
                    is CalendarSettingsViewModel.AccessGranted -> {
                        val context = ContextAmbient.current
                        val setting = SettingsViewModel.Toggle(
                            context.getString(R.string.allow_calendar_access),
                            SettingsType.AllowCalendarAccess,
                            viewModel.accessGranted
                        )
                        Column {
                            SettingsRow(setting, dispatcher)
                            Text(
                                text = context.getString(R.string.allow_calendar_message),
                                style = MaterialTheme.typography.body2,
                                modifier = Modifier.padding(grid_2)
                            )
                        }
                    }
                    is CalendarSettingsViewModel.CalendarSection ->
                        Section(section = viewModel.section, dispatcher = dispatcher)
                }
            }
        }
    )
}

@ExperimentalCoroutinesApi
@Preview("Settings page light theme")
@Composable
fun PreviewCalendarSettingsPageLight() {
    ThemedPreview(false) {
        CalendarSettingsPageContent(
            calendarSettingsPreviewData,
            "Calendar Settings",
            {}
        )
    }
}

@ExperimentalCoroutinesApi
@Preview("Settings page dark theme")
@Composable
fun PreviewCalendarSettingsPageDark() {
    ThemedPreview(true) {
        CalendarSettingsPageContent(
            calendarSettingsPreviewData,
            "Calendar Settings",
            { }
        )
    }
}

val calendarSettingsPreviewData: List<CalendarSettingsViewModel> = listOf(
    CalendarSettingsViewModel.AccessGranted(false),
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