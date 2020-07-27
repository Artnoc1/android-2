package com.toggl.settings.domain

import com.toggl.settings.common.CoroutineTest
import com.toggl.settings.common.createSettingsReducer
import com.toggl.settings.common.createSettingsState
import com.toggl.settings.common.testReduceEffects
import com.toggl.settings.common.testReduceState
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The SignOutTapped action")
class SignOutTappedActionTest : CoroutineTest() {
    private val reducer = createSettingsReducer(dispatcherProvider = dispatcherProvider)
    private val initialSate = createSettingsState()

    @Test
    fun `shouldn't change the state`() = runBlockingTest {
        reducer.testReduceState(
            initialSate,
            SettingsAction.SignOutTapped
        ) { state -> state shouldBe initialSate }
    }

    @Test
    fun `should return sign out effect`() = runBlockingTest {
        reducer.testReduceEffects(
            initialSate,
            SettingsAction.SignOutTapped
        ) { effects ->
            effects.shouldBeSingleton()
            effects.first().shouldBeInstanceOf<SignOutEffect>()
        }
    }
}