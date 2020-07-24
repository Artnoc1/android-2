package com.toggl.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.Recomposer
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.ui.core.setContent
import com.toggl.architecture.Loadable
import com.toggl.settings.R
import com.toggl.settings.domain.SettingsAction
import com.toggl.settings.ui.composables.FeedbackPage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class FeedbackFragment : Fragment() {

    private val store: SettingsStoreViewModel by viewModels()

    @ExperimentalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FrameLayout(requireContext()).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        (this as ViewGroup).setContent(Recomposer.current()) {
            val sendFeedbackRequest = store.state.map { it.localState.sendFeedbackRequest }.distinctUntilChanged()

            FeedbackPage(
                sendFeedbackRequest = sendFeedbackRequest,
                onBack = { store.dispatch(SettingsAction.FinishedEditingSetting) },
                onFeedbackSent = { feedbackText ->
                    store.dispatch(SettingsAction.SendFeedbackTapped(feedbackText))
                }
            )

            sendFeedbackRequest
                .onEach { request ->
                    val msg = when (request) {
                        is Loadable.Loaded -> getString(R.string.feedback_thank_you)
                        is Loadable.Error -> request.failure.errorMessage
                        else -> return@onEach
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    store.dispatch(SettingsAction.SendFeedbackResultSeen)
                }
                .launchIn(lifecycleScope)
        }
    }
}