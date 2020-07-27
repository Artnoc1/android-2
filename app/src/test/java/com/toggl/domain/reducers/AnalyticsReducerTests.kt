package com.toggl.domain.reducers

import com.toggl.architecture.core.MutableValue
import com.toggl.architecture.extensions.noEffect
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import com.toggl.domain.loading.LoadingAction
import com.toggl.common.feature.services.analytics.AnalyticsService
import com.toggl.common.feature.services.analytics.Event
import com.toggl.timer.common.domain.TimerAction
import com.toggl.timer.log.domain.TimeEntriesLogAction
import io.kotest.matchers.shouldBe
import io.kotest.specs.FreeSpec
import io.mockk.Called
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify

class AnalyticsReducerTests : FreeSpec({
    mockkStatic("com.toggl.domain.reducers.AnalyticsReducerKt")
    val analyticsService = mockk<AnalyticsService>(relaxed = true)
    val analyticsReducer = AnalyticsReducer(analyticsService)

    "The AnalyticsReducer's reduce function " - {
        val initialState = AppState()

        "does not change the state" - {
            var state = initialState
            val mutableValue = state.toMutableValue { state = it }
            val appAction = mockk<AppAction>()

            analyticsReducer.reduce(mutableValue, appAction)

            initialState shouldBe state
        }

        "does not produce any effect" - {
            var state = initialState
            val mutableValue = state.toMutableValue { state = it }
            val appAction = mockk<AppAction>()

            val effect = analyticsReducer.reduce(mutableValue, appAction)

            effect shouldBe noEffect()
        }

        "does not call the AnalyticsService's track method if the event is null" - {
            var state = initialState
            val mutableValue = state.toMutableValue { state = it }
            val appAction = AppAction.Loading(LoadingAction.StartLoading)

            analyticsReducer.reduce(mutableValue, appAction)

            verify { analyticsService wasNot Called }
        }

        "calls the AnalyticsService's track method if the event is not null" - {
            var state = initialState
            val mutableValue = state.toMutableValue { state = it }
            val appAction = AppAction.Timer(TimerAction.TimeEntriesLog(TimeEntriesLogAction.UndoButtonTapped))
            val expectedEvent = Event.undoTapped()

            analyticsReducer.reduce(mutableValue, appAction)

            verify(exactly = 1) { analyticsService.track(expectedEvent) }
        }
    }
})

fun AppState.toMutableValue(setFunction: (AppState) -> Unit) =
    MutableValue({ this }, setFunction)
