package com.toggl.settings.ui.composables

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.layout.padding
import androidx.ui.material.MaterialTheme
import androidx.ui.tooling.preview.Preview
import com.toggl.models.domain.SettingsType
import com.toggl.settings.compose.ThemedPreview
import com.toggl.settings.compose.theme.grid_2
import com.toggl.settings.domain.SettingsViewModel

@Composable
internal fun ListChoiceSetting(
    model: SettingsViewModel.ListChoice
) {
    Text(
        modifier = Modifier.padding(start = grid_2),
        text = model.label,
        style = MaterialTheme.typography.body2
    )
    Text(
        modifier = Modifier.padding(end = grid_2),
        text = model.selectedValueTitle,
        style = MaterialTheme.typography.body2
    )
}

internal val previewListChoice = SettingsViewModel.ListChoice("First day of the week", SettingsType.FirstDayOfTheWeek, "Wednesday")

@Preview("ListChoice light theme")
@Composable
fun PreviewListChoiceLight() {
    ThemedPreview {
        SettingsRow(previewListChoice) { }
    }
}

@Preview("ListChoice dark theme")
@Composable
fun PreviewListChoiceDark() {
    ThemedPreview(darkTheme = true) {
        SettingsRow(previewListChoice) { }
    }
}