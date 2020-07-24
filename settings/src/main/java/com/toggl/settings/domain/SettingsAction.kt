package com.toggl.settings.domain

import arrow.optics.optics
import com.toggl.models.domain.DateFormat
import com.toggl.models.domain.DurationFormat
import com.toggl.models.domain.SettingsType
import com.toggl.models.domain.SmartAlertsOption
import com.toggl.models.domain.UserPreferences
import com.toggl.models.validation.Email
import java.time.DayOfWeek

@optics
sealed class SettingsAction {
    data class UserPreferencesUpdated(val userPreferences: UserPreferences) : SettingsAction()
    data class UserCalendarIntegrationToggled(val calendarId: String) : SettingsAction()
    data class WorkspaceSelected(val selectedWorkspaceId: Long) : SettingsAction()
    data class DateFormatSelected(val dateFormat: DateFormat) : SettingsAction()
    data class DurationFormatSelected(val durationFormat: DurationFormat) : SettingsAction()
    data class FirstDayOfTheWeekSelected(val firstDayOfTheWeek: DayOfWeek) : SettingsAction()
    data class SmartAlertsOptionSelected(val smartAlertsOption: SmartAlertsOption) : SettingsAction()
    object ManualModeToggled : SettingsAction()
    object Use24HourClockToggled : SettingsAction()
    object CellSwipeActionsToggled : SettingsAction()
    object GroupSimilarTimeEntriesToggled : SettingsAction()
    object OpenCalendarSettingsTapped : SettingsAction()
    object AllowCalendarAccessToggled : SettingsAction()
    object CalendarPermissionRequested : SettingsAction()
    object SignOutTapped : SettingsAction()
    object SignOutCompleted : SettingsAction()
    data class CalendarPermissionReceived(val granted: Boolean) : SettingsAction()
    data class SendFeedbackTapped(val feedbackMessage: String) : SettingsAction()
    object OpenSubmitFeedbackTapped : SettingsAction()
    object FeedbackSent : SettingsAction()
    object SendFeedbackResultSeen : SettingsAction()
    data class SetSendFeedbackError(val throwable: Throwable) : SettingsAction()
    data class UpdateEmail(val email: Email.Valid) : SettingsAction()
    data class UpdateName(val name: String) : SettingsAction()
    object FinishedEditingSetting : SettingsAction()
    data class OpenSelectionDialog(val settingType: SettingsType) : SettingsAction()
    companion object
}

fun SettingsAction.formatForDebug() =
    when (this) {
        is SettingsAction.UserPreferencesUpdated -> "User preferences updated: $userPreferences"
        is SettingsAction.ManualModeToggled -> "Manual mode toggled"
        is SettingsAction.Use24HourClockToggled -> "Use 24 hour clock toggled"
        is SettingsAction.WorkspaceSelected -> "Selected workspace with id $selectedWorkspaceId"
        is SettingsAction.DateFormatSelected -> "Date format $dateFormat selected"
        is SettingsAction.DurationFormatSelected -> "Duration format $durationFormat selected"
        is SettingsAction.FirstDayOfTheWeekSelected -> "Selected $firstDayOfTheWeek as first day of the week"
        is SettingsAction.GroupSimilarTimeEntriesToggled -> "Group similar time entries toggled"
        is SettingsAction.CellSwipeActionsToggled -> "Cell swipe actions toggled"
        is SettingsAction.SmartAlertsOptionSelected -> "Smart alerts options toggled to $smartAlertsOption"
        is SettingsAction.UserCalendarIntegrationToggled -> "Toggled calendar integration for calendar $calendarId"
        is SettingsAction.AllowCalendarAccessToggled -> "Toggled calendar access"
        is SettingsAction.CalendarPermissionReceived -> "Calendar permission was ${if (granted) "" else "not"} granted"
        is SettingsAction.SendFeedbackTapped -> "Send feedback message: $feedbackMessage"
        is SettingsAction.SetSendFeedbackError -> "Setting SendFeedback error to $throwable"
        SettingsAction.CalendarPermissionRequested -> "Calendar permission requested"
        SettingsAction.FeedbackSent -> "Feedback sent"
        SettingsAction.SendFeedbackResultSeen -> "Send feedback result seen"
        SettingsAction.SignOutTapped -> "Sign out tapped"
        SettingsAction.SignOutCompleted -> "Sign out completed"
        is SettingsAction.UpdateEmail -> "Updated email to $email"
        is SettingsAction.UpdateName -> "Updated name to $name"
        SettingsAction.OpenCalendarSettingsTapped -> "Open calendar settings tapped"
        is SettingsAction.FinishedEditingSetting -> "Dialog dismissed"
        is SettingsAction.OpenSelectionDialog -> "Selection dialog opened for setting $settingType"
        SettingsAction.OpenSubmitFeedbackTapped -> "Open submit feedback tapped"
    }