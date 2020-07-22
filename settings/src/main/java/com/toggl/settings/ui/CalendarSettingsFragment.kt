package com.toggl.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.Recomposer
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.ui.core.setContent
import com.toggl.architecture.extensions.select
import com.toggl.settings.R
import com.toggl.settings.domain.CalendarSettingsSelector
import com.toggl.settings.ui.composables.pages.CalendarSettingsPage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@AndroidEntryPoint
class CalendarSettingsFragment : Fragment() {
    @Inject @JvmField var calendarSettingsSelector: CalendarSettingsSelector? = null // https://github.com/google/dagger/issues/1883#issuecomment-642565920 🤷‍
    private val store: SettingsStoreViewModel by viewModels()

    @ExperimentalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FrameLayout(requireContext()).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        (this as ViewGroup).setContent(Recomposer.current()) {
            val selectedState = store.state.select(calendarSettingsSelector!!)
            CalendarSettingsPage(
                selectedState,
                getString(R.string.settings),
                store::dispatch
            )
        }
    }
}
