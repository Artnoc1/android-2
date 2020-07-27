package com.toggl.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.ui.core.setContent
import com.toggl.architecture.extensions.select
import com.toggl.settings.compose.extensions.createComposeView
import com.toggl.settings.domain.SettingsSelector
import com.toggl.settings.domain.SingleChoiceSettingSelector
import com.toggl.settings.ui.composables.SingleChoiceDialogWithHeader
import com.toggl.settings.ui.composables.pages.SettingsPage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    @Inject @JvmField var settingsSelector: SettingsSelector? = null // https://github.com/google/dagger/issues/1883#issuecomment-642565920 🤷‍
    @Inject @JvmField var singleChoiceSettingSelector: SingleChoiceSettingSelector? = null // https://github.com/google/dagger/issues/1883#issuecomment-642565920 🤷‍
    private val store: SettingsStoreViewModel by viewModels()

    @ExperimentalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = createComposeView { statusBarHeight, navigationBarHeight ->
        setContent(androidx.compose.Recomposer.current()) {
            val selectedState = store.state.select(settingsSelector!!)
            val selectedSingleChoiceState = store.state.select(singleChoiceSettingSelector!!)

            SettingsPage(
                selectedState,
                statusBarHeight,
                navigationBarHeight,
                store::dispatch
            )

            SingleChoiceDialogWithHeader(selectedSingleChoiceState, store::dispatch)
        }
    }
}
