package com.toggl.calendar.common

import com.toggl.architecture.DispatcherProvider
import com.toggl.common.feature.services.calendar.CalendarEvent
import com.toggl.architecture.core.Effect
import com.toggl.architecture.core.MutableValue
import com.toggl.architecture.core.Reducer
import com.toggl.calendar.calendarday.domain.CalendarDayReducer
import com.toggl.common.feature.timeentry.TimeEntryAction
import com.toggl.common.feature.timeentry.TimeEntryActionHolder
import com.toggl.common.feature.services.calendar.CalendarService
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.TimeEntry
import com.toggl.models.domain.User
import com.toggl.models.validation.ApiToken
import com.toggl.models.validation.Email
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions
import java.time.Duration
import java.time.OffsetDateTime
import kotlin.contracts.ExperimentalContracts

val validUser = User(
    id = 0,
    apiToken = ApiToken.from("12345678901234567890123456789012") as ApiToken.Valid,
    defaultWorkspaceId = 1,
    email = Email.from("valid.mail@toggl.com") as Email.Valid,
    name = "name"
)

fun createTimeEntry(
    id: Long,
    description: String = "",
    startTime: OffsetDateTime = OffsetDateTime.now(),
    duration: Duration? = Duration.ofMinutes(2),
    billable: Boolean = false,
    projectId: Long? = null,
    workspaceId: Long = 1,
    taskId: Long? = null,
    tags: List<Long> = emptyList()
) =
    TimeEntry(
        id,
        description,
        startTime,
        duration,
        billable,
        workspaceId,
        projectId,
        taskId,
        false,
        tags
    )

fun createCalendarEvent(
    id: String = "",
    description: String = "",
    startTime: OffsetDateTime = OffsetDateTime.now(),
    duration: Duration = Duration.ofMinutes(2),
    color: String = "#c2c2c2",
    calendarId: String = ""
) = CalendarEvent(
    id,
    startTime,
    duration,
    description,
    color,
    calendarId
)

@ExperimentalContracts
fun createCalendarDayReducer(
    calendarService: CalendarService = mockk(),
    timeService: TimeService = mockk(),
    dispatcherProvider: DispatcherProvider
) = CalendarDayReducer(
    calendarService,
    timeService,
    dispatcherProvider
)

fun <T> T.toMutableValue(setFunction: (T) -> Unit) =
    MutableValue({ this }, setFunction)

suspend fun <State, Action> Reducer<State, Action>.testReduce(
    initialState: State,
    action: Action,
    testCase: suspend (State, List<Effect<Action>>) -> Unit
) {
    var state = initialState
    val mutableValue = state.toMutableValue { state = it }
    val effect = reduce(mutableValue, action)
    testCase(state, effect)
}

@ExperimentalCoroutinesApi
fun <State, Action, EX : Exception> Reducer<State, Action>.testReduceException(
    initialState: State,
    action: Action,
    exception: Class<EX>
) {
    Assertions.assertThrows(exception) {
        runBlockingTest {
            testReduce(initialState, action) { _, _ -> }
        }
    }
}

suspend fun <State, Action> Reducer<State, Action>.testReduceState(
    initialState: State,
    action: Action,
    testCase: suspend (State) -> Unit
) = testReduce(initialState, action) { state, _ -> testCase(state) }

suspend fun <State, Action> Reducer<State, Action>.testReduceEffects(
    initialState: State,
    action: Action,
    testCase: suspend (List<Effect<Action>>) -> Unit
) = testReduce(initialState, action) { _, effects -> testCase(effects) }

suspend fun <State, Action> Reducer<State, Action>.testReduceNoEffects(
    initialState: State,
    action: Action
) = testReduce(initialState, action, ::assertNoEffectsWereReturned)

@Suppress("UNUSED_PARAMETER")
suspend fun <State, Action> assertNoEffectsWereReturned(state: State, effect: List<Effect<Action>>) {
    effect.shouldBeEmpty()
}

suspend inline fun <reified Holder : TimeEntryActionHolder, reified TimeEntryActionType : TimeEntryAction> Effect<Any>.shouldEmitTimeEntryAction(
    additionalTestBlock: (TimeEntryActionType) -> Unit = {}
) {
    this.execute().shouldBeTypeOf<Holder> {
        it.timeEntryAction.shouldBeTypeOf<TimeEntryActionType> { timeEntryAction ->
            additionalTestBlock(timeEntryAction)
        }
    }
}