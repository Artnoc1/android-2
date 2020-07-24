package com.toggl.onboarding.welcome.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.toggl.common.extensions.adjustMarginToNavigationBarInsets
import com.toggl.onboarding.R
import com.toggl.onboarding.welcome.domain.WelcomeAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_welcome.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class WelcomeFragment : Fragment(R.layout.fragment_welcome) {
    private val store: WelcomeStoreViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view_pager.adapter = WelcomeCarouselAdapter(requireContext())
        tab_indicator.setupWithViewPager(view_pager)

        continue_with_sso.adjustMarginToNavigationBarInsets()

        continue_with_email.setOnClickListener {
            store.dispatch(WelcomeAction.ContinueWithEmailButtonTapped)
        }

        continue_with_sso.setOnClickListener {
            store.dispatch(WelcomeAction.LoginWithSsoButtonTapped)
        }

        continue_with_google.setOnClickListener {
            store.dispatch(WelcomeAction.ContinueWithGoogleButtonTapped)
        }
    }

    override fun onDestroyView() {
        view_pager.adapter = null
        super.onDestroyView()
    }
}
