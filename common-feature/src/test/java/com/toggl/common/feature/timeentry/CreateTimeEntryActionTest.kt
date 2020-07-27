package com.toggl.common.feature.timeentry

import com.toggl.common.feature.common.CoroutineTest
import com.toggl.common.feature.common.testReduceEffects
import com.toggl.common.feature.common.testReduceState
import com.toggl.repository.dto.CreateTimeEntryDTO
import com.toggl.repository.interfaces.TimeEntryRepository
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime

@ExperimentalCoroutinesApi
@DisplayName("The CreateTimeEntryAction")
class CreateTimeEntryActionTest : CoroutineTest() {

    private val repository = mockk<TimeEntryRepository>()
    private val reducer = TimeEntryReducer(repository, mockk(), dispatcherProvider)
    private val initialState = TimeEntryState(mapOf())
    private val createTimeEntryDTO = CreateTimeEntryDTO("", OffsetDateTime.MAX, Duration.ZERO, false, 1, 1, 1, listOf())

    @Test
    fun `Should return CreateTimeEntryEffect`() = runBlockingTest {
        reducer.testReduceEffects(
            initialState,
            TimeEntryAction.CreateTimeEntry(createTimeEntryDTO)
        ) { effects ->
            effects.shouldBeSingleton()
            effects.first().shouldBeTypeOf<CreateTimeEntryEffect>()
        }
    }

    @Test
    fun `Shouldn't change the state`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            TimeEntryAction.CreateTimeEntry(createTimeEntryDTO)
        ) { state -> state shouldBe initialState }
    }
}
