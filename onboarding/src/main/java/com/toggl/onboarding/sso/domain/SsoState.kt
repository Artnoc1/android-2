package com.toggl.onboarding.sso.domain

import com.toggl.models.validation.Email
import com.toggl.onboarding.common.domain.OnboardingState

data class SsoState(val email: Email) {
    companion object {
        fun fromOnboardingState(onboardingState: OnboardingState) =
            SsoState(onboardingState.localState.email)

        fun toOnboardingState(onboardingState: OnboardingState, ssoState: SsoState) =
            onboardingState.copy(
                localState = onboardingState.localState.copy(
                    email = ssoState.email
                )
            )
    }
}