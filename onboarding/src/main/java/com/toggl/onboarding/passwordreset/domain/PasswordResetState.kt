package com.toggl.onboarding.passwordreset.domain

import com.toggl.models.validation.Email
import com.toggl.onboarding.common.domain.OnboardingState

data class PasswordResetState(val email: Email) {
    companion object {
        fun fromOnboardingState(onboardingState: OnboardingState) =
            PasswordResetState(onboardingState.localState.email)

        fun toOnboardingState(onboardingState: OnboardingState, passwordResetState: PasswordResetState) =
            onboardingState.copy(localState = onboardingState.localState.copy(email = passwordResetState.email))
    }
}