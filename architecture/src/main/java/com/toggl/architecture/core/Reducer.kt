package com.toggl.architecture.core

import com.toggl.architecture.extensions.map
import com.toggl.architecture.extensions.noEffect

interface Reducer<State, Action> {
    fun reduce(state: MutableValue<State>, action: Action): List<Effect<Action>>
}

interface HigherOrderReducer<State, Action> : Reducer<State, Action> {
    val innerReducer: Reducer<State, Action>
}

fun <State, Action> combine(vararg reducers: Reducer<State, Action>): Reducer<State, Action> =
    CombinedReducer(reducers.toList())

internal class CombinedReducer<State, Action>(private val reducers: List<Reducer<State, Action>>) : Reducer<State, Action> {
    override fun reduce(state: MutableValue<State>, action: Action): List<Effect<Action>> =
        reducers.flatMap { it.reduce(state, action) }
}

fun <LocalState, GlobalState, LocalAction, GlobalAction> Reducer<GlobalState, GlobalAction>.decorateWith(
    reducer: Reducer<LocalState, LocalAction>,
    mapToLocalState: (GlobalState) -> LocalState,
    mapToLocalAction: (GlobalAction) -> LocalAction?,
    mapToGlobalState: (GlobalState, LocalState) -> GlobalState,
    mapToGlobalAction: (LocalAction) -> GlobalAction
): Reducer<GlobalState, GlobalAction> =
    CombinedReducer(listOf(this, reducer.pullback(mapToLocalState, mapToLocalAction, mapToGlobalState, mapToGlobalAction)))

fun <LocalState, GlobalState, LocalAction, GlobalAction>
    Reducer<LocalState, LocalAction>.pullback(
        mapToLocalState: (GlobalState) -> LocalState,
        mapToLocalAction: (GlobalAction) -> LocalAction?,
        mapToGlobalState: (GlobalState, LocalState) -> GlobalState,
        mapToGlobalAction: (LocalAction) -> GlobalAction
    ): Reducer<GlobalState, GlobalAction> =
    PullbackReducer(this, mapToLocalState, mapToLocalAction, mapToGlobalState, mapToGlobalAction)

class PullbackReducer<LocalState, GlobalState, LocalAction, GlobalAction>(
    private val innerReducer: Reducer<LocalState, LocalAction>,
    private val mapToLocalState: (GlobalState) -> LocalState,
    private val mapToLocalAction: (GlobalAction) -> LocalAction?,
    private val mapToGlobalState: (GlobalState, LocalState) -> GlobalState,
    private val mapToGlobalAction: (LocalAction) -> GlobalAction
) : Reducer<GlobalState, GlobalAction> {
    override fun reduce(
        state: MutableValue<GlobalState>,
        action: GlobalAction
    ): List<Effect<GlobalAction>> {
        val localAction = mapToLocalAction(action)
            ?: return noEffect()

        return innerReducer
            .reduce(state.map(mapToLocalState, mapToGlobalState), localAction)
            .map { effect -> effect.map { action -> action?.run(mapToGlobalAction) } }
    }
}

fun <LocalState, GlobalState, LocalAction, GlobalAction>
    Reducer<LocalState, LocalAction>.optionalPullback(
        mapToLocalState: (GlobalState) -> LocalState?,
        mapToLocalAction: (GlobalAction) -> LocalAction?,
        mapToGlobalState: (GlobalState, LocalState?) -> GlobalState,
        mapToGlobalAction: (LocalAction) -> GlobalAction
    ): Reducer<GlobalState, GlobalAction> =
    OptionalReducer(this, mapToLocalState, mapToLocalAction, mapToGlobalState, mapToGlobalAction)

class OptionalReducer<LocalState, GlobalState, LocalAction, GlobalAction>(
    private val innerReducer: Reducer<LocalState, LocalAction>,
    private val mapToLocalState: (GlobalState) -> LocalState?,
    private val mapToLocalAction: (GlobalAction) -> LocalAction?,
    private val mapToGlobalState: (GlobalState, LocalState?) -> GlobalState,
    private val mapToGlobalAction: (LocalAction) -> GlobalAction
) : Reducer<GlobalState, GlobalAction> {
    override fun reduce(
        state: MutableValue<GlobalState>,
        action: GlobalAction
    ): List<Effect<GlobalAction>> {
        val localAction = mapToLocalAction(action)
            ?: return noEffect()

        var localState: LocalState = state.map(mapToLocalState, mapToGlobalState).invoke() ?: return noEffect()
        val localMutableValue = MutableValue({ localState }) { localState = it }

        val effects = innerReducer
            .reduce(localMutableValue, localAction)
            .map { effect -> effect.map { action -> action?.run(mapToGlobalAction) } }

        state.mutate { mapToGlobalState(this, localState) }

        return effects
    }
}