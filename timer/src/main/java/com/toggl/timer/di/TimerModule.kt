package com.toggl.timer.di

import android.content.Context
import com.toggl.architecture.core.Store
import com.toggl.architecture.core.combine
import com.toggl.architecture.core.decorateWith
import com.toggl.architecture.core.optionalPullback
import com.toggl.architecture.core.pullback
import com.toggl.architecture.core.unwrap
import com.toggl.common.Constants
import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.common.feature.timeentry.TimeEntryReducer
import com.toggl.common.feature.timeentry.TimeEntryState
import com.toggl.common.services.time.TimeService
import com.toggl.timer.R
import com.toggl.timer.common.domain.TimerAction
import com.toggl.timer.common.domain.TimerReducer
import com.toggl.timer.common.domain.TimerState
import com.toggl.timer.log.domain.TimeEntriesLogAction
import com.toggl.timer.log.domain.TimeEntriesLogReducer
import com.toggl.timer.log.domain.TimeEntriesLogSelector
import com.toggl.timer.log.domain.TimeEntriesLogState
import com.toggl.timer.project.domain.ProjectAction
import com.toggl.timer.project.domain.ProjectReducer
import com.toggl.timer.project.domain.ProjectState
import com.toggl.timer.running.domain.RunningTimeEntryAction
import com.toggl.timer.running.domain.RunningTimeEntryReducer
import com.toggl.timer.running.domain.RunningTimeEntryState
import com.toggl.timer.startedit.domain.ProjectTagChipSelector
import com.toggl.timer.startedit.domain.StartEditAction
import com.toggl.timer.startedit.domain.StartEditReducer
import com.toggl.timer.startedit.domain.StartEditState
import com.toggl.timer.suggestions.domain.CalendarSuggestionProvider
import com.toggl.timer.suggestions.domain.ComposeSuggestionProvider
import com.toggl.timer.suggestions.domain.MostUsedSuggestionProvider
import com.toggl.timer.suggestions.domain.SuggestionProvider
import com.toggl.timer.suggestions.domain.SuggestionsAction
import com.toggl.timer.suggestions.domain.SuggestionsReducer
import com.toggl.timer.suggestions.domain.SuggestionsState
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.FragmentScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import javax.inject.Singleton

@Module
@InstallIn(FragmentComponent::class)
object FragmentTimerModule {
    @Provides
    @FragmentScoped
    internal fun timeEntriesLogSelector(
        @ApplicationContext context: Context,
        timeService: TimeService
    ): TimeEntriesLogSelector {
        val todayString = context.getString(R.string.today)
        val yesterdayString = context.getString(R.string.yesterday)

        return TimeEntriesLogSelector(
            todayString,
            yesterdayString,
            timeService
        )
    }

    @Provides
    @FragmentScoped
    internal fun projectTagChipSelector(@ApplicationContext context: Context) =
        ProjectTagChipSelector(
            context.getString(R.string.add_project),
            context.getString(R.string.add_tags)
        )
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object ViewModelTimerModule {

    @ExperimentalCoroutinesApi
    @Provides
    internal fun timeEntriesLogStore(store: Store<TimerState, TimerAction>): Store<TimeEntriesLogState, TimeEntriesLogAction> =
        store.view(
            mapToLocalState = TimeEntriesLogState.Companion::fromTimerState,
            mapToGlobalAction = TimerAction::TimeEntriesLog
        )

    @ExperimentalCoroutinesApi
    @Provides
    internal fun startTimeEntryStore(store: Store<TimerState, TimerAction>): Store<StartEditState, StartEditAction> =
        store.optionalView(
            mapToLocalState = StartEditState.Companion::fromTimerState,
            mapToGlobalAction = TimerAction::StartEditTimeEntry
        )

    @ExperimentalCoroutinesApi
    @Provides
    internal fun runningTimeEntryStore(store: Store<TimerState, TimerAction>): Store<RunningTimeEntryState, RunningTimeEntryAction> =
        store.optionalView(
            mapToLocalState = RunningTimeEntryState.Companion::fromTimerState,
            mapToGlobalAction = TimerAction::RunningTimeEntry
        )

    @ExperimentalCoroutinesApi
    @Provides
    internal fun projectStore(store: Store<TimerState, TimerAction>): Store<ProjectState, ProjectAction> =
        store.optionalView(
            mapToLocalState = ProjectState.Companion::fromTimerState,
            mapToGlobalAction = TimerAction::Project
        )

    @ExperimentalCoroutinesApi
    @Provides
    internal fun suggestionsStore(store: Store<TimerState, TimerAction>): Store<SuggestionsState, SuggestionsAction> =
        store.optionalView(
            mapToLocalState = SuggestionsState.Companion::fromTimerState,
            mapToGlobalAction = TimerAction::Suggestions
        )
}

@Module
@InstallIn(ApplicationComponent::class)
object ApplicationTimerModule {

    @Provides
    @MaxNumberOfMostUsedSuggestions
    internal fun maxNumberOfMostusedSuggestions() = Constants.Suggestions.maxNumberOfMostUsedSuggestions

    @Provides
    @MaxNumberOfCalendarSuggestions
    internal fun maxNumberOfCalendarSuggestions() = Constants.Suggestions.maxNumberOfCalendarSuggestions

    @Provides
    internal fun suggestionProvider(
        calendarSuggestionProvider: CalendarSuggestionProvider,
        mostUsedSuggestionProvider: MostUsedSuggestionProvider
    ): SuggestionProvider =
        ComposeSuggestionProvider(
            Constants.Suggestions.maxNumberOfSuggestions,
            calendarSuggestionProvider,
            mostUsedSuggestionProvider
        )

    @ExperimentalCoroutinesApi
    @InternalCoroutinesApi
    @Provides
    @Singleton
    internal fun timerReducer(
        timeEntryReducer: TimeEntryReducer,
        timeEntriesLogReducer: TimeEntriesLogReducer,
        startEditReducer: StartEditReducer,
        runningTimeEntryReducer: RunningTimeEntryReducer,
        projectReducer: ProjectReducer,
        suggestionsReducer: SuggestionsReducer
    ): TimerReducer {

        return combine<TimerState, TimerAction>(
            timeEntriesLogReducer.decorateWith(timeEntryReducer).pullback(
                mapToLocalState = TimeEntriesLogState.Companion::fromTimerState,
                mapToLocalAction = TimerAction::unwrap,
                mapToGlobalState = TimeEntriesLogState.Companion::toTimerState,
                mapToGlobalAction = TimerAction::TimeEntriesLog
            ),
            startEditReducer.decorateWith(timeEntryReducer).optionalPullback(
                mapToLocalState = StartEditState.Companion::fromTimerState,
                mapToLocalAction = TimerAction::unwrap,
                mapToGlobalState = StartEditState.Companion::toTimerState,
                mapToGlobalAction = TimerAction::StartEditTimeEntry
            ),
            runningTimeEntryReducer.decorateWith(timeEntryReducer).optionalPullback(
                mapToLocalState = RunningTimeEntryState.Companion::fromTimerState,
                mapToLocalAction = TimerAction::unwrap,
                mapToGlobalState = RunningTimeEntryState.Companion::toTimerState,
                mapToGlobalAction = TimerAction::RunningTimeEntry
            ),
            projectReducer.optionalPullback(
                mapToLocalState = ProjectState.Companion::fromTimerState,
                mapToLocalAction = TimerAction::unwrap,
                mapToGlobalState = ProjectState.Companion::toTimerState,
                mapToGlobalAction = TimerAction::Project
            ),
            suggestionsReducer.decorateWith(timeEntryReducer).optionalPullback(
                mapToLocalState = SuggestionsState.Companion::fromTimerState,
                mapToLocalAction = TimerAction::unwrap,
                mapToGlobalState = SuggestionsState.Companion::toTimerState,
                mapToGlobalAction = TimerAction::Suggestions
            )
        )
    }

    private fun TimeEntriesLogReducer.decorateWith(timeEntryReducer: TimeEntryReducer) =
        this.decorateWith(
            timeEntryReducer,
            mapToLocalState = { TimeEntryState(it.timeEntries) },
            mapToLocalAction = { TimeEntryAction.fromTimeEntryActionHolder(it) },
            mapToGlobalState = { globalState, localState -> globalState.copy(timeEntries = localState.timeEntries) },
            mapToGlobalAction = { localAction -> TimeEntriesLogAction.TimeEntryHandling(localAction) }
        )

    private fun RunningTimeEntryReducer.decorateWith(timeEntryReducer: TimeEntryReducer) =
        this.decorateWith(
            timeEntryReducer,
            mapToLocalState = { TimeEntryState(it.timeEntries) },
            mapToLocalAction = { TimeEntryAction.fromTimeEntryActionHolder(it) },
            mapToGlobalState = { globalState, localState -> globalState.copy(timeEntries = localState.timeEntries) },
            mapToGlobalAction = { localAction -> RunningTimeEntryAction.TimeEntryHandling(localAction) }
        )

    private fun StartEditReducer.decorateWith(timeEntryReducer: TimeEntryReducer) =
        this.decorateWith(
            timeEntryReducer,
            mapToLocalState = { TimeEntryState(it.timeEntries) },
            mapToLocalAction = { TimeEntryAction.fromTimeEntryActionHolder(it) },
            mapToGlobalState = { globalState, localState -> globalState.copy(timeEntries = localState.timeEntries) },
            mapToGlobalAction = { localAction -> StartEditAction.TimeEntryHandling(localAction) }
        )

    private fun SuggestionsReducer.decorateWith(timeEntryReducer: TimeEntryReducer) =
        this.decorateWith(
            timeEntryReducer,
            mapToLocalState = { TimeEntryState(it.timeEntries) },
            mapToLocalAction = { TimeEntryAction.fromTimeEntryActionHolder(it) },
            mapToGlobalState = { globalState, localState -> globalState.copy(timeEntries = localState.timeEntries) },
            mapToGlobalAction = { localAction -> SuggestionsAction.TimeEntryHandling(localAction) }
        )
}