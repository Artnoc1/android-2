package com.toggl.domain.loading

import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.Loadable
import com.toggl.architecture.core.Subscription
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@ExperimentalCoroutinesApi
abstract class BaseLoadingSubscription(private val dispatcherProvider: DispatcherProvider) : Subscription<AppState, AppAction> {
    override fun subscribe(state: Flow<AppState>): Flow<AppAction.Loading> =
        state.map { it.user is Loadable.Loaded }
            .distinctUntilChanged()
            .flatMapLatest { isLoggedIn ->
                withContext(dispatcherProvider.io) {
                    subscribe(isLoggedIn).map { AppAction.Loading(it) }
                }
            }

    abstract fun subscribe(isUserLoggedIn: Boolean): Flow<LoadingAction>
}