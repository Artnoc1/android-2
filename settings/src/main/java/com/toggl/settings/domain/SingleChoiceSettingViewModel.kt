package com.toggl.settings.domain

data class SingleChoiceSettingViewModel(
    val header: String,
    val items: List<ChoiceListItem>,
    val closeAction: SettingsAction?
) {
    companion object {
        val Empty = SingleChoiceSettingViewModel("", listOf(), null)
    }
}
