package com.toggl.settings.domain

import arrow.optics.optics
import com.toggl.models.domain.DateFormat
import com.toggl.models.domain.DurationFormat
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
    object AllowCalendarAccessToggled : SettingsAction()
    object CalendarPermissionRequested : SettingsAction()
    object SignOutTapped : SettingsAction()
    object SignOutCompleted : SettingsAction()
    data class CalendarPermissionReceived(val granted: Boolean) : SettingsAction()
    data class SendFeedbackTapped(val feedbackMessage: String) : SettingsAction()
    object FeedbackSent : SettingsAction()
    object SendFeedbackResultSeen : SettingsAction()
    data class SetSendFeedbackError(val throwable: Throwable) : SettingsAction()
    data class UpdateEmail(val email: Email.Valid) : SettingsAction()
    data class UpdateName(val name: String) : SettingsAction()
    companion object
}

fun SettingsAction.formatForDebug() =
    when (this) {
        is SettingsAction.UserPreferencesUpdated -> "User preferences updated: $userPreferences"
        is SettingsAction.SettingTapped -> "Settings tapped: $selectedSetting"
        is SettingsAction.ManualModeToggled -> "ManualModeToggled"
        is SettingsAction.Use24HourClockToggled -> "Use24HourClockToggled"
        is SettingsAction.WorkspaceSelected -> "WorkspaceSelected selectedWorkspaceId: $selectedWorkspaceId"
        is SettingsAction.DateFormatSelected -> "DateFormatSelected dateFormat: $dateFormat"
        is SettingsAction.DurationFormatSelected -> "DurationFormatSelected durationFormat: $durationFormat"
        is SettingsAction.FirstDayOfTheWeekSelected -> "FirstDayOfTheWeekSelected firstDayOfTheWeek: $firstDayOfTheWeek"
        is SettingsAction.GroupSimilarTimeEntriesToggled -> "GroupSimilarTimeEntriesToggled"
        is SettingsAction.CellSwipeActionsToggled -> "CellSwipeActionsToggled"
        is SettingsAction.SmartAlertsOptionSelected -> "SmartAlertsOptionSelected smartAlertsOption: $smartAlertsOption"
        is SettingsAction.UserCalendarIntegrationToggled -> "UserCalendarIntegrationToggled calendarId: $calendarId"
        is SettingsAction.AllowCalendarAccessToggled -> "AllowCalendarAccessToggled"
        is SettingsAction.CalendarPermissionReceived -> "CalendarPermissionReceived was granted = $granted"
        is SettingsAction.SendFeedbackTapped -> "SendFeedbackTapped feedbackMessage: $feedbackMessage"
        is SettingsAction.SetSendFeedbackError -> "Setting SendFeedback error $throwable"
        SettingsAction.CalendarPermissionRequested -> "CalendarPermissionRequested"
        SettingsAction.FeedbackSent -> "FeedbackSent"
        SettingsAction.SendFeedbackResultSeen -> "SendFeedbackResultSeen"
        SettingsAction.SignOutTapped -> "SignOutTapped"
        SettingsAction.SignOutCompleted -> "SignOutCompleted"
        is SettingsAction.UpdateEmail -> "UpdateEmail email $email"
        is SettingsAction.UpdateName -> "UpdateName name $name"
    }