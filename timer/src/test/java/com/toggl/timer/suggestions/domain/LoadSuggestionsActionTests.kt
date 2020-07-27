package com.toggl.timer.suggestions.domain

import com.toggl.common.services.time.TimeService
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.testReduceEffects
import com.toggl.timer.common.testReduceState
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
@DisplayName("The LoadSuggestions action")
internal class LoadSuggestionsActionTests : CoroutineTest() {

    private val timeService = mockk<TimeService>()
    private val suggestionsProvider = mockk<SuggestionProvider> { coEvery { getSuggestions(any()) } returns emptyList() }
    private val reducer = SuggestionsReducer(timeService, suggestionsProvider)

    @Test
    fun `shouldn't change the sate`() = runBlockingTest {
        val initialState = createInitialState()
        reducer.testReduceState(
            initialState,
            SuggestionsAction.LoadSuggestions
        ) { state -> state shouldBe initialState }
    }

    @Test
    fun `should emit load suggestions effect`() = runBlockingTest {
        val initialState = createInitialState()
        reducer.testReduceEffects(
            initialState,
            SuggestionsAction.LoadSuggestions
        ) { effects ->
            effects.shouldBeSingleton()
            effects.first().shouldBeTypeOf<LoadSuggestionEffect>()
        }
    }
}
