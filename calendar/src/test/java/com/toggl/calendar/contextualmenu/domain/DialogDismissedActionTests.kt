package com.toggl.calendar.contextualmenu.domain

import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.calendar.common.testReduceEffects
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.EditableTimeEntry
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@ExperimentalCoroutinesApi
@DisplayName("The DialogDismissed action")
internal class DialogDismissedActionTests {

    private val timeService = mockk<TimeService> { every { now() } returns OffsetDateTime.now() }
    private val reducer = ContextualMenuReducer(timeService)

    @Test
    fun `returns a close effect`() = runBlockingTest {
        val timeEntry = EditableTimeEntry.empty(1)
        val initialState = createInitialState(selectedItem = SelectedCalendarItem.SelectedTimeEntry(timeEntry))

        reducer.testReduceEffects(initialState, ContextualMenuAction.DialogDismissed) {
            it.single().execute().shouldBeInstanceOf<ContextualMenuAction.Close>()
        }
    }
}