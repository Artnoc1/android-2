package com.toggl.common.feature.navigation

import com.toggl.architecture.core.Effect
import com.toggl.architecture.core.MutableValue
import com.toggl.common.feature.extensions.mutateWithoutEffects

interface BackStackAwareState<T> {
    fun popBackStack(): T
}

fun <State : BackStackAwareState<State>, Action> MutableValue<State>.popBackStackWithoutEffects(): List<Effect<Action>> =
    mutateWithoutEffects { popBackStack() }