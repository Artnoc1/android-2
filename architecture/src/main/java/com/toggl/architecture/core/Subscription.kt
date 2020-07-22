package com.toggl.architecture.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.merge

interface Subscription<State, Action : Any> {
    fun subscribe(state: Flow<State>): Flow<Action>
}

@ExperimentalCoroutinesApi
class CompositeSubscription<State, Action : Any>(
    private val subscriptions: Collection<Subscription<State, Action>>
) : Subscription<State, Action> {
    override fun subscribe(state: Flow<State>): Flow<Action> {
        val subscriptionFlows: List<Flow<Action>> = subscriptions.map { sub ->
            sub.subscribe(state.distinctUntilChanged())
        }
        return subscriptionFlows.merge()
    }
}