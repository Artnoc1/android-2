package com.toggl.settings.domain

import com.toggl.models.domain.SettingsType
import com.toggl.settings.common.CoroutineTest
import com.toggl.settings.common.createSettingsReducer
import com.toggl.settings.common.createSettingsState
import com.toggl.settings.common.testReduceNoEffects
import com.toggl.settings.common.testReduceState
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The DialogDismissed action")
class DialogDismissedActionTests : CoroutineTest() {
    private val initialState = createSettingsState(singleChoiceSettingShowing = SettingsType.DateFormat)
    private val reducer = createSettingsReducer(dispatcherProvider = dispatcherProvider)

    @Test
    fun `Should clear the singleChoiceSettingShowing from local state`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            SettingsAction.DialogDismissed
        ) { state -> state.localState.singleChoiceSettingShowing shouldBe null }
    }

    @Test
    fun `Should produce no effects`() = runBlockingTest {
        reducer.testReduceNoEffects(initialState, SettingsAction.DialogDismissed)
    }
}