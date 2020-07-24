package com.toggl.onboarding.welcome.ui

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import com.toggl.architecture.core.Store
import com.toggl.onboarding.welcome.domain.WelcomeAction
import com.toggl.onboarding.welcome.domain.WelcomeState

class WelcomeStoreViewModel @ViewModelInject constructor(
    store: Store<WelcomeState, WelcomeAction>
) : ViewModel(), Store<WelcomeState, WelcomeAction> by store