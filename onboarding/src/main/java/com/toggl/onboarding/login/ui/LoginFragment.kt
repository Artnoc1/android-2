package com.toggl.onboarding.login.ui

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.toggl.architecture.Loadable
import com.toggl.models.validation.Email
import com.toggl.models.validation.Password
import com.toggl.onboarding.R
import com.toggl.onboarding.login.domain.LoginAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val store: LoginViewModel by viewModels()

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        login_button.setOnClickListener {
            store.dispatch(LoginAction.LoginButtonTapped)
        }

        email.doOnTextChanged { text, _, _, _ ->
            val action = LoginAction.EmailEntered(text.toString())
            store.dispatch(action)
        }

        password.doOnTextChanged { text, _, _, _ ->
            val action = LoginAction.PasswordEntered(text.toString())
            store.dispatch(action)
        }

        store.state
            .map { it.email is Email.Valid && it.password is Password.Valid && it.user !is Loadable.Loading }
            .distinctUntilChanged()
            .onEach { login_button.isEnabled = it }
            .launchIn(lifecycleScope)
    }
}
