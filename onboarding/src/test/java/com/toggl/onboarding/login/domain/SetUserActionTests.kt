package com.toggl.onboarding.login.domain

import com.toggl.api.login.LoginApiClient
import com.toggl.architecture.Loadable
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.backStackOf
import com.toggl.onboarding.common.CoroutineTest
import com.toggl.onboarding.common.testReduceState
import com.toggl.onboarding.common.validUser
import com.toggl.repository.interfaces.UserRepository
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The set user action")
class SetUserActionTests : CoroutineTest() {
    private val loginApi: LoginApiClient = mockk()
    private val userRepository: UserRepository = mockk()
    private val reducer = LoginReducer(loginApi, userRepository, dispatcherProvider)

    @Test
    fun `sets the user from the state and the route to the timer page`() = runBlockingTest {
        val initialState = emptyState()

        reducer.testReduceState(initialState, LoginAction.SetUser(validUser)) { newState ->
            newState shouldBe initialState.copy(
                backStack = backStackOf(Route.Timer),
                user = Loadable.Loaded(validUser)
            )
        }
    }
}