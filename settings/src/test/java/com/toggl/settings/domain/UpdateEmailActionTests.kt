package com.toggl.settings.domain

import com.toggl.models.validation.Email
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
@DisplayName("The UpdateEmail action")
class UpdateEmailActionTests : CoroutineTest() {
    private val initialState = createSettingsState()
    private val reducer = createSettingsReducer(dispatcherProvider = dispatcherProvider)
    private val validEmail = Email.from("you@test.com") as Email.Valid

    @Test
    fun `shouldn't return any effects`() = runBlockingTest {
        reducer.testReduceNoEffects(initialState, SettingsAction.UpdateEmail(validEmail))
    }

    @Test
    fun `should change user's name`() = runBlockingTest {
        reducer.testReduceState(
            initialState,
            SettingsAction.UpdateEmail(validEmail)
        ) { state ->
            state.user.email shouldBe validEmail
        }
    }
}