package com.toggl.settings.domain

data class ChoiceListItem(
    val label: String = "",
    val isSelected: Boolean = false,
    val selectedActions: List<SettingsAction>? = null
) {
    fun dispatchSelected(dispatcher: (SettingsAction) -> Unit = {}) {
        selectedActions?.forEach(dispatcher)
    }
}