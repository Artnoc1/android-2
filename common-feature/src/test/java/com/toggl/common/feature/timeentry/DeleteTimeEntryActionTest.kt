package com.toggl.common.feature.timeentry

import com.toggl.common.feature.common.CoroutineTest
import com.toggl.common.feature.common.createTimeEntry
import com.toggl.common.feature.common.testReduceEffects
import com.toggl.common.feature.common.testReduceException
import com.toggl.common.feature.common.testReduceState
import com.toggl.common.feature.timeentry.exceptions.TimeEntryDoesNotExistException
import com.toggl.repository.interfaces.TimeEntryRepository
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The DeleteTimeEntryAction")
class DeleteTimeEntryActionTest : CoroutineTest() {

    private val repository = mockk<TimeEntryRepository>()
    private val reducer = TimeEntryReducer(repository, mockk(), dispatcherProvider)
    private val initialState = TimeEntryState(mapOf(1L to createTimeEntry(1L)))

    @Test
    fun `Should return StartTimeEntryEffect`() = runBlockingTest {
        reducer.testReduceEffects(
            initialState,
            TimeEntryAction.DeleteTimeEntry(1)
        ) { effects ->
            effects.shouldBeSingleton()
            effects.first().shouldBeTypeOf<DeleteTimeEntryEffect>()
        }
    }

    @Test
    fun `Should throw if id doesn't exist in current state`() = runBlockingTest {
        reducer.testReduceException(
            initialState,
            TimeEntryAction.DeleteTimeEntry(2),
            TimeEntryDoesNotExistException::class.java
        )
    }

    @Test
    fun `Shouldn't change the state`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            TimeEntryAction.DeleteTimeEntry(1)
        ) { state -> state shouldBe initialState }
    }
}
