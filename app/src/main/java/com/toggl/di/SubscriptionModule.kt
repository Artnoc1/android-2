package com.toggl.di

import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.core.Subscription
import com.toggl.architecture.core.CompositeSubscription
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import com.toggl.domain.loading.LoadProjectsSubscriptions
import com.toggl.domain.loading.LoadTimeEntriesSubscription
import com.toggl.repository.Repository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@Module
@InstallIn(ApplicationComponent::class)
object SubscriptionModule {

    @Provides
    fun compositeSubscription(
        subscriptions: Set<@JvmSuppressWildcards Subscription<AppState, AppAction>>
    ): CompositeSubscription<AppState, AppAction> =
        CompositeSubscription(subscriptions)

    @Provides
    @IntoSet
    fun loadTimeEntriesSubscription(
        repository: Repository,
        dispatcherProvider: DispatcherProvider
    ): Subscription<AppState, AppAction> =
        LoadTimeEntriesSubscription(repository, dispatcherProvider)

    @Provides
    @IntoSet
    fun loadProjectSubscription(
        repository: Repository,
        dispatcherProvider: DispatcherProvider
    ): Subscription<AppState, AppAction> =
        LoadProjectsSubscriptions(repository, dispatcherProvider)
}