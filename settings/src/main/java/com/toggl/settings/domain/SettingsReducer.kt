package com.toggl.settings.domain

import com.toggl.api.feedback.FeedbackApiClient
import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.Failure
import com.toggl.architecture.Loadable
import com.toggl.architecture.core.Effect
import com.toggl.architecture.core.MutableValue
import com.toggl.architecture.core.Reducer
import com.toggl.architecture.extensions.effect
import com.toggl.architecture.extensions.effectOf
import com.toggl.architecture.extensions.noEffect
import com.toggl.common.feature.extensions.mutateWithoutEffects
import com.toggl.common.services.permissions.PermissionCheckerService
import com.toggl.models.domain.PlatformInfo
import com.toggl.models.domain.SettingsType
import com.toggl.models.domain.UserPreferences
import com.toggl.repository.interfaces.SettingsRepository
import javax.inject.Inject

class SettingsReducer @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val permissionCheckerService: PermissionCheckerService,
    private val platformInfo: PlatformInfo,
    private val signOutEffect: SignOutEffect,
    private val feedbackDataBuilder: FeedbackDataBuilder,
    private val feedbackApiClient: FeedbackApiClient,
    private val dispatcherProvider: DispatcherProvider
) : Reducer<SettingsState, SettingsAction> {

    override fun reduce(
        state: MutableValue<SettingsState>,
        action: SettingsAction
    ): List<Effect<SettingsAction>> =
        when (action) {
            is SettingsAction.SettingTapped -> when (action.selectedSetting) {
                SettingsType.Name -> TODO()
                SettingsType.Email -> TODO()
                SettingsType.Workspace, SettingsType.DateFormat, SettingsType.DurationFormat, SettingsType.FirstDayOfTheWeek -> state.handleSingleChoiceSettingNavigation(
                    action.selectedSetting
                )
                SettingsType.TwentyFourHourClock -> effectOf(SettingsAction.Use24HourClockToggled)
                SettingsType.GroupSimilar -> effectOf(SettingsAction.GroupSimilarTimeEntriesToggled)
                SettingsType.CellSwipe -> effectOf(SettingsAction.CellSwipeActionsToggled)
                SettingsType.ManualMode -> effectOf(SettingsAction.ManualModeToggled)
                SettingsType.CalendarSettings -> effectOf(SettingsAction.AllowCalendarAccessToggled)
                SettingsType.SmartAlert -> TODO()
                SettingsType.SubmitFeedback -> TODO()
                SettingsType.About -> TODO()
                SettingsType.PrivacyPolicy -> TODO()
                SettingsType.TermsOfService -> TODO()
                SettingsType.Licenses -> TODO()
                SettingsType.Help -> TODO()
                SettingsType.SignOut -> effectOf(SettingsAction.SignOutTapped)
            }
            is SettingsAction.UserPreferencesUpdated -> state.mutateWithoutEffects { copy(userPreferences = action.userPreferences) }
            is SettingsAction.ManualModeToggled -> state.updatePrefs { copy(manualModeEnabled = !manualModeEnabled) }
            is SettingsAction.Use24HourClockToggled -> state.updatePrefs { copy(twentyFourHourClockEnabled = !twentyFourHourClockEnabled) }
            is SettingsAction.CellSwipeActionsToggled -> state.updatePrefs { copy(cellSwipeActionsEnabled = !cellSwipeActionsEnabled) }
            is SettingsAction.GroupSimilarTimeEntriesToggled -> state.updatePrefs { copy(groupSimilarTimeEntriesEnabled = !groupSimilarTimeEntriesEnabled) }
            is SettingsAction.WorkspaceSelected -> state.updatePrefs { copy(selectedWorkspaceId = action.selectedWorkspaceId) }
            is SettingsAction.DateFormatSelected -> state.updatePrefs { copy(dateFormat = action.dateFormat) }
            is SettingsAction.DurationFormatSelected -> state.updatePrefs { copy(durationFormat = action.durationFormat) }
            is SettingsAction.FirstDayOfTheWeekSelected -> state.updatePrefs { copy(firstDayOfTheWeek = action.firstDayOfTheWeek) }
            is SettingsAction.SmartAlertsOptionSelected -> state.updatePrefs { copy(smartAlertsOption = action.smartAlertsOption) }
            is SettingsAction.UserCalendarIntegrationToggled -> state.updatePrefs {
                if (calendarIds.contains(action.calendarId)) copy(calendarIds = calendarIds - action.calendarId)
                else copy(calendarIds = calendarIds + action.calendarId)
            }
            is SettingsAction.AllowCalendarAccessToggled -> state.handleAllowCalendarAccessToggled()
            is SettingsAction.CalendarPermissionRequested -> state.mutateWithoutEffects { copy(shouldRequestCalendarPermission = true) }
            is SettingsAction.CalendarPermissionReceived -> state.mutateWithoutEffects { copy(shouldRequestCalendarPermission = false) }
            SettingsAction.SignOutTapped -> effect(signOutEffect)
            SettingsAction.SignOutCompleted -> noEffect()
            is SettingsAction.SendFeedbackTapped -> {
                state.mutate {
                    SettingsState.localState.modify(this) {
                        it.copy(sendFeedbackRequest = Loadable.Loading)
                    }
                }
                effect(
                    SendFeedbackEffect(action.feedbackMessage, state().user, platformInfo, feedbackDataBuilder, feedbackApiClient, dispatcherProvider)
                )
            }
            is SettingsAction.FeedbackSent -> state.updateSendFeedbackRequestStateWithoutEffects(Loadable.Loaded(Unit))
            is SettingsAction.SendFeedbackResultSeen -> state.updateSendFeedbackRequestStateWithoutEffects(Loadable.Uninitialized)
            is SettingsAction.SetSendFeedbackError -> state.updateSendFeedbackRequestStateWithoutEffects(
                Loadable.Error(Failure(action.throwable, ""))
            )
            is SettingsAction.UpdateEmail -> state.mutateWithoutEffects { copy(user = user.copy(email = action.email)) }
            is SettingsAction.UpdateName -> state.mutateWithoutEffects { copy(user = user.copy(name = action.name)) }
            is SettingsAction.DialogDismissed -> state.mutateWithoutEffects { copy(localState = localState.copy(singleChoiceSettingShowing = null)) }
        }

    private fun MutableValue<SettingsState>.handleAllowCalendarAccessToggled(): List<Effect<SettingsAction>> {
        val updatePrefsEffects = updatePrefs {
            copy(
                calendarIntegrationEnabled = !calendarIntegrationEnabled,
                calendarIds = if (calendarIntegrationEnabled) calendarIds else emptyList()
            )
        }
        return if (!this().userPreferences.calendarIntegrationEnabled && !permissionCheckerService.hasCalendarPermission())
            updatePrefsEffects + RequestCalendarPermissionEffect()
        else
            updatePrefsEffects
    }

    private fun MutableValue<SettingsState>.updatePrefs(updateBlock: UserPreferences.() -> UserPreferences) =
        effect(UpdateUserPreferencesEffect(this().userPreferences.updateBlock(), settingsRepository, dispatcherProvider))

    private fun MutableValue<SettingsState>.updateSendFeedbackRequestStateWithoutEffects(loadable: Loadable<Unit>) =
        mutateWithoutEffects<SettingsState, SettingsAction> {
            SettingsState.localState.modify(this) {
                it.copy(sendFeedbackRequest = loadable)
            }
        }

    private fun MutableValue<SettingsState>.handleSingleChoiceSettingNavigation(settingsType: SettingsType): List<Effect<SettingsAction>> =
        this.mutateWithoutEffects {
            copy(localState = localState.copy(singleChoiceSettingShowing = settingsType))
        }
}
