package com.toggl.onboarding.sso.domain

sealed class SsoAction {
    object ContinueButtonTapped : SsoAction()
    data class EmailEntered(val email: String) : SsoAction()

    companion object
}

fun SsoAction.formatForDebug() =
    when (this) {
        SsoAction.ContinueButtonTapped -> "Continue button tapped"
        is SsoAction.EmailEntered -> "Email entered $email"
    }