package com.toggl.di

import com.toggl.architecture.core.Reducer
import com.toggl.calendar.di.CalendarReducer
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import com.toggl.domain.reducers.AnalyticsReducer
import com.toggl.domain.loading.LoadingReducer
import com.toggl.domain.reducers.NavigationReducer
import com.toggl.domain.reducers.SignOutReducer
import com.toggl.domain.reducers.FeatureAvailabilityReducer
import com.toggl.domain.reducers.LoggingReducer
import com.toggl.domain.reducers.createAppReducer
import com.toggl.onboarding.common.domain.OnboardingReducer
import com.toggl.settings.domain.SettingsReducer
import com.toggl.timer.common.domain.TimerReducer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi

@Module
@InstallIn(ApplicationComponent::class)
object ReducerModule {

    @Provides
    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    @ProvideAppReducer
    fun appReducer(
        loadingReducer: LoadingReducer,
        onboardingReducer: OnboardingReducer,
        timerReducer: TimerReducer,
        calendarReducer: CalendarReducer,
        settingsReducer: SettingsReducer,
        navigationReducer: NavigationReducer,
        analyticsReducer: AnalyticsReducer
    ): Reducer<AppState, AppAction> =
        createAppReducer(
            loadingReducer,
            onboardingReducer,
            timerReducer,
            calendarReducer,
            settingsReducer,
            navigationReducer,
            analyticsReducer
        )

    @Provides
    @ProvideFeatureAvailabilityReducer
    fun featureAvailabilityReducer(
        @ProvideAppReducer innerReducer: Reducer<AppState, AppAction>
    ): Reducer<AppState, AppAction> =
        FeatureAvailabilityReducer(innerReducer)

    @Provides
    @ProvideAuthReducer
    fun authReducer(
        @ProvideFeatureAvailabilityReducer innerReducer: Reducer<AppState, AppAction>
    ): Reducer<AppState, AppAction> =
        SignOutReducer(innerReducer)

    @Provides
    @ProvideLoggingReducer
    fun loggingReducer(
        @ProvideAuthReducer innerReducer: Reducer<AppState, AppAction>
    ): Reducer<AppState, AppAction> =
        LoggingReducer(innerReducer)
}