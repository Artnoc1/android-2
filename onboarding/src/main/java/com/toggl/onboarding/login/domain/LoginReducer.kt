package com.toggl.onboarding.login.domain

import com.toggl.api.login.LoginApiClient
import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.Failure
import com.toggl.architecture.Loadable
import com.toggl.architecture.core.Effect
import com.toggl.architecture.core.MutableValue
import com.toggl.architecture.core.Reducer
import com.toggl.architecture.extensions.effect
import com.toggl.architecture.extensions.noEffect
import com.toggl.common.feature.extensions.mutateWithoutEffects
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.backStackOf
import com.toggl.models.validation.Email
import com.toggl.models.validation.Password
import com.toggl.models.validation.toEmail
import com.toggl.models.validation.toPassword
import com.toggl.repository.interfaces.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginReducer @Inject constructor(
    private val apiClient: LoginApiClient,
    private val userRepository: UserRepository,
    private val dispatcherProvider: DispatcherProvider
) : Reducer<LoginState, LoginAction> {

    override fun reduce(state: MutableValue<LoginState>, action: LoginAction): List<Effect<LoginAction>> =
        when (action) {
            LoginAction.LoginButtonTapped -> {
                val currentState = state()
                when (val email = currentState.email) {
                    is Email.Invalid -> noEffect()
                    is Email.Valid -> when (val password = currentState.password) {
                        is Password.Invalid -> noEffect()
                        is Password.Valid -> {
                            state.mutate { copy(user = Loadable.Loading) }
                            effect(LogUserInEffect(apiClient, userRepository, dispatcherProvider, email, password))
                        }
                    }
                }
            }
            is LoginAction.SetUser -> state.mutateWithoutEffects {
                copy(
                    user = Loadable.Loaded(action.user),
                    backStack = backStackOf(Route.Timer)
                )
            }
            is LoginAction.SetUserError -> state.mutateWithoutEffects {
                copy(user = Loadable.Error(Failure(action.throwable, "")))
            }
            is LoginAction.EmailEntered -> state.mutateWithoutEffects {
                copy(email = action.email.toEmail())
            }
            is LoginAction.PasswordEntered -> state.mutateWithoutEffects {
                copy(password = action.password.toPassword())
            }
        }
}
