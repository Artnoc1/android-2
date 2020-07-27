package com.toggl.onboarding.login.domain

import com.toggl.api.login.LoginApiClient
import com.toggl.architecture.Failure
import com.toggl.architecture.Loadable
import com.toggl.onboarding.common.CoroutineTest
import com.toggl.onboarding.common.testReduceState
import com.toggl.repository.interfaces.UserRepository
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The set user error action")
class SetUserErrorActionTests : CoroutineTest() {
    private val loginApi: LoginApiClient = mockk()
    private val userRepository: UserRepository = mockk()
    private val reducer = LoginReducer(loginApi, userRepository, dispatcherProvider)

    private val throwable = IllegalAccessException()

    @Test
    fun `sets the user error`() = runBlockingTest {
        val initialState = emptyState()

        reducer.testReduceState(initialState, LoginAction.SetUserError(throwable)) { newState ->
            newState shouldBe initialState.copy(
                user = Loadable.Error(Failure(throwable, ""))
            )
        }
    }
}
