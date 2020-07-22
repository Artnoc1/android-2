package com.toggl.settings.ui.composables

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.layout.Column
import androidx.ui.layout.padding
import androidx.ui.material.MaterialTheme
import androidx.ui.material.Switch
import androidx.ui.tooling.preview.Preview
import androidx.ui.unit.Dp
import com.toggl.models.domain.SettingsType
import com.toggl.settings.compose.ThemedPreview
import com.toggl.settings.compose.theme.grid_2
import com.toggl.settings.domain.SettingsViewModel

@Composable
internal fun ToggleSetting(
    model: SettingsViewModel.Toggle,
    onClickAction: () -> Unit
) {
    Text(
        modifier = Modifier.padding(start = grid_2),
        text = model.label,
        style = MaterialTheme.typography.body2
    )
    Switch(
        modifier = Modifier.padding(end = grid_2),
        checked = model.toggled,
        color = MaterialTheme.colors.primary,
        onCheckedChange = { onClickAction() }
    )
}

internal val previewToggle = SettingsViewModel.Toggle("Toggle title", SettingsType.ManualMode, false)

@Preview("Toggle with light theme")
@Composable
fun PreviewToggleLight() {
    ThemedPreview {
        Column {
            SettingsRow(previewToggle) { }
            SettingsRow(previewToggle.copy(toggled = true)) { }
        }
    }
}

@Preview("Toggle with dark theme")
@Composable
fun PreviewToggleDark() {
    ThemedPreview(darkTheme = true) {
        Column {
            SettingsRow(previewToggle) { }
            SettingsRow(previewToggle.copy(toggled = true)) { }
        }
    }
}