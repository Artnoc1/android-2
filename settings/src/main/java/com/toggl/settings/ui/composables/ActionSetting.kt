package com.toggl.settings.ui.composables

import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Text
import androidx.ui.layout.fillMaxWidth
import androidx.ui.material.MaterialTheme
import androidx.ui.text.style.TextAlign
import androidx.ui.tooling.preview.Preview
import com.toggl.models.domain.SettingsType
import com.toggl.settings.compose.ThemedPreview
import com.toggl.settings.domain.SettingsViewModel

@Composable
internal fun ActionSetting(
    model: SettingsViewModel.ActionRow
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = model.label,
        style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.primary)
    )
}

internal val previewButton = SettingsViewModel.ActionRow("Sign Out", SettingsType.SignOut)

@Preview("Button light theme")
@Composable
fun PreviewButtonLight() {
    ThemedPreview {
        SettingsRow(previewButton) { }
    }
}

@Preview("Button dark theme")
@Composable
fun PreviewButtonDark() {
    ThemedPreview(darkTheme = true) {
        SettingsRow(previewButton) { }
    }
}