package com.toggl.onboarding.welcome.domain

import arrow.optics.optics
import com.toggl.architecture.Loadable
import com.toggl.common.feature.navigation.BackStack
import com.toggl.models.domain.User
import com.toggl.onboarding.common.domain.OnboardingState

@optics
data class WelcomeState(
    val user: Loadable<User>,
    val backStack: BackStack
) {
    companion object {
        fun fromOnboardingState(onboardingState: OnboardingState) =
            WelcomeState(
                onboardingState.user,
                onboardingState.backStack
            )

        fun toOnboardingState(onboardingState: OnboardingState, welcomeState: WelcomeState) =
            onboardingState.copy(
                user = welcomeState.user,
                backStack = welcomeState.backStack
            )
    }
}