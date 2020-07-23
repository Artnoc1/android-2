package com.toggl.settings.ui.composables

import androidx.compose.Composable
import androidx.compose.collectAsState
import androidx.compose.getValue
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Dialog
import androidx.ui.foundation.Text
import androidx.ui.foundation.clickable
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredWidth
import androidx.ui.material.MaterialTheme
import androidx.ui.material.RadioButton
import androidx.ui.material.RadioGroup
import androidx.ui.tooling.preview.Preview
import com.toggl.settings.compose.ThemedPreview
import com.toggl.settings.compose.theme.Shapes
import com.toggl.settings.compose.theme.TogglTheme
import com.toggl.settings.compose.theme.grid_3
import com.toggl.settings.compose.theme.grid_4
import com.toggl.settings.compose.theme.grid_6
import com.toggl.settings.compose.theme.grid_8
import com.toggl.settings.domain.ChoiceListItem
import com.toggl.settings.domain.SettingsAction
import com.toggl.settings.domain.SingleChoiceSettingViewModel
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

@Composable
internal fun SingleChoiceDialogWithHeader(
    items: Flow<SingleChoiceSettingViewModel>,
    dispatcher: (SettingsAction) -> Unit = {}
) {
    val observableItems by items.collectAsState(initial = SingleChoiceSettingViewModel.Empty)

    if (observableItems == SingleChoiceSettingViewModel.Empty) return

    Dialog(onCloseRequest = {
        observableItems.closeAction?.run(dispatcher)
    }) {
        TogglTheme {
            Box(shape = Shapes.medium, backgroundColor = MaterialTheme.colors.background) {
                SingleChoiceListWithHeader(
                    items = observableItems.items,
                    header = observableItems.header,
                    closeAction = observableItems.closeAction,
                    dispatcher = dispatcher
                )
            }
        }
    }
}

@Composable
internal fun SingleChoiceListWithHeader(
    items: List<ChoiceListItem>,
    header: String,
    closeAction: SettingsAction?,
    dispatcher: (SettingsAction) -> Unit = {}
) {

    fun dispatchOnSelected(item: ChoiceListItem) {
        item.dispatchSelected(dispatcher)
        closeAction?.run(dispatcher)
    }

    Column(modifier = Modifier.padding(grid_3) + Modifier.fillMaxWidth()) {
        Text(
            text = header,
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.preferredHeight(grid_8)
        )
        RadioGroup {
            items.forEach { item ->
                RadioGroupItem(selected = item.isSelected, onSelect = { dispatchOnSelected(item) }) {
                    Row(
                        modifier = Modifier.preferredHeight(grid_6) +
                            Modifier.clickable(onClick = { dispatchOnSelected(item) }) +
                            Modifier.fillMaxWidth()
                    ) {
                        RadioButton(selected = item.isSelected, onSelect = { dispatchOnSelected(item) })
                        Spacer(modifier = Modifier.preferredWidth(grid_4))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onBackground
                        )
                    }
                }
            }
        }
    }
}

internal val previewItems = (0..5).map {
    if (it == 1) ChoiceListItem(
        "Choice selected",
        isSelected = true
    ) else ChoiceListItem("Choice ${Random.nextInt() % 100}")
}
internal val previewHeader = "First day of week"

@Preview("SingleChoiceListWithHeader light theme")
@Composable
fun PreviewSingleChoiceListWithHeaderLight() {
    ThemedPreview {
        SingleChoiceListWithHeader(previewItems, previewHeader, null)
    }
}

@Preview("SingleChoiceListWithHeader dark theme")
@Composable
fun PreviewSingleChoiceListWithHeaderDark() {
    ThemedPreview(darkTheme = true) {
        SingleChoiceListWithHeader(previewItems, previewHeader, null)
    }
}