package com.toggl.domain.reducers

import com.toggl.architecture.core.Reducer
import com.toggl.architecture.core.combine
import com.toggl.architecture.core.optionalPullback
import com.toggl.architecture.core.pullback
import com.toggl.architecture.core.unwrap
import com.toggl.calendar.di.CalendarReducer
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import com.toggl.domain.loading.LoadingReducer
import com.toggl.domain.mappings.mapAppStateToCalendarState
import com.toggl.domain.mappings.mapAppStateToLoadingState
import com.toggl.domain.mappings.mapAppStateToOnboardingState
import com.toggl.domain.mappings.mapAppStateToSettingsState
import com.toggl.domain.mappings.mapAppStateToTimerState
import com.toggl.domain.mappings.mapCalendarActionToAppAction
import com.toggl.domain.mappings.mapCalendarStateToAppState
import com.toggl.domain.mappings.mapLoadingActionToAppAction
import com.toggl.domain.mappings.mapLoadingStateToAppState
import com.toggl.domain.mappings.mapOnboardingActionToAppAction
import com.toggl.domain.mappings.mapOnboardingStateToAppState
import com.toggl.domain.mappings.mapSettingsActionToAppAction
import com.toggl.domain.mappings.mapSettingsStateToAppState
import com.toggl.domain.mappings.mapTimerActionToAppAction
import com.toggl.domain.mappings.mapTimerStateToAppState
import com.toggl.onboarding.common.domain.OnboardingReducer
import com.toggl.settings.domain.SettingsReducer
import com.toggl.timer.common.domain.TimerReducer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

typealias AppReducer = Reducer<AppState, AppAction>

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
fun createAppReducer(
    loadingReducer: LoadingReducer,
    onboardingReducer: OnboardingReducer,
    timerReducer: TimerReducer,
    calendarReducer: CalendarReducer,
    settingsReducer: SettingsReducer,
    navigationReducer: NavigationReducer,
    analyticsReducer: AnalyticsReducer
): AppReducer =
    combine(
        analyticsReducer,
        navigationReducer,
        loadingReducer.pullback(
            mapToLocalState = ::mapAppStateToLoadingState,
            mapToLocalAction = AppAction::unwrap,
            mapToGlobalState = ::mapLoadingStateToAppState,
            mapToGlobalAction = ::mapLoadingActionToAppAction
        ),
        timerReducer.pullback(
            mapToLocalState = ::mapAppStateToTimerState,
            mapToLocalAction = AppAction::unwrap,
            mapToGlobalState = ::mapTimerStateToAppState,
            mapToGlobalAction = ::mapTimerActionToAppAction
        ),
        onboardingReducer.pullback(
            mapToLocalState = ::mapAppStateToOnboardingState,
            mapToLocalAction = AppAction::unwrap,
            mapToGlobalState = ::mapOnboardingStateToAppState,
            mapToGlobalAction = ::mapOnboardingActionToAppAction
        ),
        calendarReducer.pullback(
            mapToLocalState = ::mapAppStateToCalendarState,
            mapToLocalAction = AppAction::unwrap,
            mapToGlobalState = ::mapCalendarStateToAppState,
            mapToGlobalAction = ::mapCalendarActionToAppAction
        ),
        settingsReducer.optionalPullback(
            mapToLocalState = ::mapAppStateToSettingsState,
            mapToLocalAction = AppAction::unwrap,
            mapToGlobalState = ::mapSettingsStateToAppState,
            mapToGlobalAction = ::mapSettingsActionToAppAction
        )
    )
