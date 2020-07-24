package com.toggl.domain.reducers

import com.toggl.architecture.core.Effect
import com.toggl.architecture.core.MutableValue
import com.toggl.architecture.core.Reducer
import com.toggl.architecture.extensions.noEffect
import com.toggl.common.feature.extensions.mutateWithoutEffects
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.backStackOf
import com.toggl.common.feature.navigation.popBackStackWithoutEffects
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import com.toggl.domain.Tab
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationReducer @Inject constructor() : Reducer<AppState, AppAction> {
    override fun reduce(state: MutableValue<AppState>, action: AppAction): List<Effect<AppAction>> =
        when (action) {
            is AppAction.Loading,
            is AppAction.Onboarding,
            is AppAction.Timer,
            is AppAction.Calendar,
            is AppAction.Settings -> noEffect()
            AppAction.BackButtonPressed -> state.popBackStackWithoutEffects()
            is AppAction.TabSelected -> state.mutateWithoutEffects {
                copy(backStack = when (action.tab) {
                    Tab.Timer -> backStackOf(Route.Timer)
                    Tab.Reports -> backStackOf(Route.Timer, Route.Reports)
                    Tab.Calendar -> backStackOf(Route.Timer, Route.Calendar)
                })
            }
        }
}