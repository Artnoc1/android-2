package com.toggl.domain.reducers

import com.toggl.architecture.core.Effect
import com.toggl.architecture.core.HigherOrderReducer
import com.toggl.architecture.core.MutableValue
import com.toggl.architecture.core.Reducer
import com.toggl.architecture.core.isOrWraps
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.backStackOf
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import com.toggl.settings.domain.SettingsAction

class SignOutReducer(override val innerReducer: Reducer<AppState, AppAction>) : HigherOrderReducer<AppState, AppAction> {
    override fun reduce(
        state: MutableValue<AppState>,
        action: AppAction
    ): List<Effect<AppAction>> {
        if (action.isOrWraps<SettingsAction.SignOutCompleted>()) {
            state.mutate {
                AppState(
                    backStack = backStackOf(Route.Welcome),
                    calendarPermissionWasGranted = calendarPermissionWasGranted
                )
            }
        }
        return innerReducer.reduce(state, action)
    }
}