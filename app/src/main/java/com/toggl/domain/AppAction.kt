package com.toggl.domain

import com.toggl.architecture.core.ActionWrapper
import com.toggl.calendar.common.domain.CalendarAction
import com.toggl.domain.loading.LoadingAction
import com.toggl.onboarding.common.domain.OnboardingAction
import com.toggl.settings.domain.SettingsAction
import com.toggl.timer.common.domain.TimerAction

sealed class AppAction {
    class Loading(override val action: LoadingAction) : AppAction(), ActionWrapper<LoadingAction>
    class Onboarding(override val action: OnboardingAction) : AppAction(), ActionWrapper<OnboardingAction>
    class Timer(override val action: TimerAction) : AppAction(), ActionWrapper<TimerAction>
    class Calendar(override val action: CalendarAction) : AppAction(), ActionWrapper<CalendarAction>
    class Settings(override val action: SettingsAction) : AppAction(), ActionWrapper<SettingsAction>
    class TabSelected(val tab: Tab) : AppAction()
    object BackButtonPressed : AppAction()
}

enum class Tab {
    Timer,
    Reports,
    Calendar
}