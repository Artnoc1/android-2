package com.toggl.settings.domain

import android.content.Context
import com.toggl.architecture.core.Selector
import com.toggl.models.domain.SettingsType
import com.toggl.models.domain.User
import com.toggl.models.domain.UserPreferences
import com.toggl.settings.R
import java.lang.IllegalStateException
import javax.inject.Inject

class SettingsSelector @Inject constructor(
    private val context: Context,
    private val sectionsBlueprint: List<SettingsSectionBlueprint>
) : Selector<SettingsState, List<SettingsSectionViewModel>> {
    override suspend fun select(state: SettingsState): List<SettingsSectionViewModel> {
        return sectionsBlueprint.map { it.toViewModel(state.user, state.userPreferences) }
    }

    private fun SettingsSectionBlueprint.toViewModel(user: User, userPreferences: UserPreferences): SettingsSectionViewModel =
        SettingsSectionViewModel(context.getString(title), settingsList.map { it.toViewModel(user, userPreferences) })

    private fun SettingsType.toViewModel(user: User, userPreferences: UserPreferences): SettingsViewModel =
        when (this) {
            SettingsType.Name -> SettingsViewModel.ListChoice(
                context.getString(R.string.name),
                this,
                user.name
            )
            SettingsType.Email -> SettingsViewModel.ListChoice(
                context.getString(R.string.email_address),
                this,
                user.email.email
            )
            SettingsType.Workspace -> SettingsViewModel.ListChoice(
                context.getString(R.string.workspace),
                this,
                "Dummy workspace"
            )
            SettingsType.DateFormat -> SettingsViewModel.ListChoice(
                context.getString(R.string.date_format),
                this,
                userPreferences.dateFormat.label
            )
            SettingsType.TwentyFourHourClock -> SettingsViewModel.Toggle(
                context.getString(R.string.use_24_hour_clock),
                this,
                userPreferences.twentyFourHourClockEnabled
            )
            SettingsType.DurationFormat -> SettingsViewModel.ListChoice(
                context.getString(R.string.duration_format),
                this,
                "Dummy Duration"
            )
            SettingsType.FirstDayOfTheWeek -> SettingsViewModel.ListChoice(
                context.getString(R.string.first_day_of_the_week),
                this,
                "Dummy Day"
            )
            SettingsType.GroupSimilar -> SettingsViewModel.Toggle(
                context.getString(R.string.group_similar_time_entries),
                this,
                userPreferences.groupSimilarTimeEntriesEnabled
            )
            SettingsType.CellSwipe -> SettingsViewModel.Toggle(
                context.getString(R.string.cell_swipe_actions),
                this,
                userPreferences.cellSwipeActionsEnabled
            )
            SettingsType.ManualMode -> SettingsViewModel.Toggle(
                context.getString(R.string.manual_mode),
                this,
                userPreferences.manualModeEnabled
            )
            SettingsType.CalendarSettings -> SettingsViewModel.SubPage(
                context.getString(R.string.calendar_settings),
                this
            )
            SettingsType.SmartAlert -> SettingsViewModel.SubPage(
                context.getString(R.string.smart_alerts),
                this
            )
            SettingsType.SubmitFeedback -> SettingsViewModel.SubPage(
                context.getString(R.string.submit_feedback),
                this
            )
            SettingsType.About -> SettingsViewModel.SubPage(
                context.getString(R.string.about),
                this
            )
            SettingsType.PrivacyPolicy -> SettingsViewModel.SubPage(
                context.getString(R.string.privacy_policy),
                this
            )
            SettingsType.TermsOfService -> SettingsViewModel.SubPage(
                context.getString(R.string.terms_of_service),
                this
            )
            SettingsType.Licenses -> SettingsViewModel.SubPage(
                context.getString(R.string.licenses),
                this
            )
            SettingsType.Help -> SettingsViewModel.SubPage(
                context.getString(R.string.help),
                this
            )
            SettingsType.SignOut -> SettingsViewModel.ActionRow(
                context.getString(R.string.sign_out),
                this
            )
            SettingsType.AllowCalendarAccess,
            is SettingsType.Calendar -> throw IllegalStateException("This settings should not be used in the main settings page")
        }
}