package com.toggl.timer.project.ui

import android.app.Dialog
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.toggl.architecture.extensions.select
import com.toggl.common.extensions.addInterceptingOnClickListener
import com.toggl.common.extensions.adjustForUserTheme
import com.toggl.common.extensions.performClickHapticFeedback
import com.toggl.common.extensions.requestFocus
import com.toggl.common.extensions.setOvalBackground
import com.toggl.common.extensions.setSafeText
import com.toggl.common.feature.extensions.toColor
import com.toggl.common.feature.navigation.handleBackPressesEmitting
import com.toggl.common.ui.LifecycleAwareAutocompletePopup
import com.toggl.common.ui.Position
import com.toggl.models.common.AutocompleteSuggestion.ProjectSuggestions
import com.toggl.models.domain.Client
import com.toggl.models.domain.Project
import com.toggl.models.domain.Workspace
import com.toggl.timer.R
import com.toggl.timer.extensions.tryHidingKeyboard
import com.toggl.timer.extensions.tryShowingKeyboardFor
import com.toggl.timer.project.domain.ColorViewModel
import com.toggl.timer.project.domain.ProjectAction
import com.toggl.timer.project.domain.ProjectAutocompleteQuery
import com.toggl.timer.project.domain.ProjectColorSelector
import com.toggl.timer.project.domain.ProjectState
import com.toggl.timer.project.ui.autocomplete.ProjectAutocompleteSuggestionViewHolder
import com.toggl.timer.project.ui.autocomplete.ProjectAutocompleteSuggestionsAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_dialog_project.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.lang.ref.WeakReference

@AndroidEntryPoint
class ProjectDialogFragment : BottomSheetDialogFragment() {

    @ExperimentalCoroutinesApi
    private val coloPickerVisibilityRequestFlow = MutableStateFlow(false)

    @FlowPreview
    @ExperimentalCoroutinesApi
    private val adapter = ColorAdapter(::onColorTapped)
    private val store: ProjectStoreViewModel by viewModels()

    private lateinit var projectNameChangedListener: TextWatcher
    private lateinit var colorPickerAnimator: ColorPickerAnimator

    @ExperimentalCoroutinesApi
    @FlowPreview
    private lateinit var autocompleteSuggestionsPopup: LifecycleAwareAutocompletePopup<ProjectSuggestions, ProjectAutocompleteSuggestionViewHolder>
    private lateinit var autocompleteSuggestionsRevealAnimator: AutocompleteSuggestionsRevealAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BottomSheetDialog)
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).also {
            it.handleBackPressesEmitting(lifecycle) { store.dispatch(ProjectAction.Close) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_dialog_project, container, false)

    @InternalCoroutinesApi
    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        color_palette.adapter = adapter
        color_palette.itemAnimator = null

        projectNameChangedListener = project_name_edit_text.addTextChangedListener {
            val action = ProjectAction.NameEntered(it.toString())
            store.dispatch(action)
        }

        colorPickerAnimator = ColorPickerAnimator(
            WeakReference(requireActivity().window),
            WeakReference(color_picker_container),
            WeakReference(custom_color_picker),
            resources.getDimension(R.dimen.basic_color_picker_height).toInt(),
            resources.getDimension(R.dimen.premium_color_picker_height).toInt()
        )

        autocompleteSuggestionsRevealAnimator = AutocompleteSuggestionsRevealAnimator(
            client_workspace_edit_text,
            cancel_pick,
            project_color_indicator,
            client_workspace_container,
            viewLifecycleOwner
        ) {
            project_name_edit_text?.clearFocus()
        }

        val suggestionsAdapter = ProjectAutocompleteSuggestionsAdapter {
            when (it) {
                is ProjectSuggestions.Workspace -> store.dispatch(ProjectAction.WorkspacePicked(it.workspace))
                is ProjectSuggestions.Client -> store.dispatch(ProjectAction.ClientPicked(it.client))
                is ProjectSuggestions.CreateClient -> store.dispatch(ProjectAction.CreateClientSuggestionTapped(it.name))
            }
            store.dispatch(ProjectAction.AutocompleteDescriptionEntered(ProjectAutocompleteQuery.None))
        }
        autocompleteSuggestionsPopup = LifecycleAwareAutocompletePopup(
            requireContext(),
            client_workspace_edit_text,
            suggestionsAdapter,
            viewLifecycleOwner
        )

        project_name_edit_text.requestFocus {
            activity?.tryShowingKeyboardFor(project_name_edit_text)
        }

        color_done_button.setOnClickListener {
            coloPickerVisibilityRequestFlow.value = false
        }

        project_color_indicator.setOnClickListener {
            coloPickerVisibilityRequestFlow.value = color_picker_container.height == 0
        }

        private_chip.addInterceptingOnClickListener {
            store.dispatch(ProjectAction.PrivateProjectSwitchTapped)
        }

        workspace_chip.setOnClickListener {
            store.dispatch(ProjectAction.AutocompleteDescriptionEntered(ProjectAutocompleteQuery.WorkspaceQuery("")))
        }

        client_chip.setOnClickListener {
            store.dispatch(ProjectAction.AutocompleteDescriptionEntered(ProjectAutocompleteQuery.ClientQuery("")))
        }

        cancel_pick.setOnClickListener {
            store.dispatch(ProjectAction.AutocompleteDescriptionEntered(ProjectAutocompleteQuery.None))
        }

        create_button.setOnClickListener {
            context?.performClickHapticFeedback()
            store.dispatch(ProjectAction.DoneButtonTapped)
        }

        hue_saturation_picker.hueFlow
            .combine(hue_saturation_picker.saturationFlow) { hue, saturation ->
                ProjectAction.ColorHueSaturationChanged(hue, saturation)
            }
            .distinctUntilChanged()
            .onEach { store.dispatch(it) }
            .launchIn(lifecycleScope)

        value_picker.valueFlow
            .distinctUntilChanged()
            .map { ProjectAction.ColorValueChanged(it) }
            .onEach { store.dispatch(it) }
            .launchIn(lifecycleScope)

        store.select(ProjectColorSelector())
            .onEach { colors -> adapter.submitList(colors) }
            .launchIn(lifecycleScope)

        store.state
            .map { it.editableProject.name }
            .distinctUntilChanged()
            .onEach { project_name_edit_text.setSafeText(it) }
            .launchIn(lifecycleScope)

        store.state
            .map { it.editableProject.isPrivate }
            .distinctUntilChanged()
            .onEach { private_chip.isChecked = it }
            .launchIn(lifecycleScope)

        store.state
            .map { it.autocompleteSuggestions }
            .distinctUntilChanged()
            .onEach { suggestionsAdapter.submitList(it) }
            .launchIn(lifecycleScope)

        store.state
            .distinctUntilChangedBy { it.editableProject.workspaceId }
            .map { it.selectedWorkspace() }
            .onEach { workspace_chip.text = it.name }
            .launchIn(lifecycleScope)

        store.state
            .distinctUntilChangedBy { it.editableProject.clientId }
            .map { it.selectedClient() }
            .onEach { client_chip.text = it?.name ?: getString(R.string.add_client) }
            .launchIn(lifecycleScope)

        val hasMoreThanOneWorkspaceFlow = store.state
            .map { it.workspaces.size > 1 }
            .distinctUntilChanged()

        coloPickerVisibilityRequestFlow.onEach { isPickingColor -> client_chip.isClickable = !isPickingColor }
            .launchIn(lifecycleScope)

        coloPickerVisibilityRequestFlow
            .combine(hasMoreThanOneWorkspaceFlow) { isPickingColor, hasMoreThanOneWorkspace -> !isPickingColor && hasMoreThanOneWorkspace }
            .onEach { canShowWorkspacePicker -> workspace_chip.isClickable = canShowWorkspacePicker }
            .launchIn(lifecycleScope)

        val autoCompleteQueryFlow = store.state
            .map { it.autocompleteQuery }
            .distinctUntilChanged()

        autoCompleteQueryFlow
            .map { it is ProjectAutocompleteQuery.None }
            .onEach { isNotPickingClientOrWorkspace -> project_color_indicator.isClickable = isNotPickingClientOrWorkspace }
            .launchIn(lifecycleScope)

        store.state
            .map { it.customColor }
            .distinctUntilChanged()
            .onEach { hsv ->
                custom_project_color_indicator.setOvalBackground(
                    hsv.toColor().adjustForUserTheme(custom_project_color_indicator.context)
                )
                val (hue, saturation, value) = hsv
                value_picker.hue = hue
                value_picker.saturation = saturation
                value_picker.value = value
                hue_saturation_picker.hue = hue
                hue_saturation_picker.saturation = saturation
                hue_saturation_picker.value = value
            }.launchIn(lifecycleScope)

        val colorFlow = store.state.map { it.editableProject.color }.distinctUntilChanged()

        colorFlow
            .onEach {
                project_color_indicator.setOvalBackground(it.adjustForUserTheme(custom_project_color_indicator.context))
            }
            .launchIn(lifecycleScope)

        val colorIsPremiumFlow = colorFlow.map {
            !Project.defaultColors.contains(it)
        }.distinctUntilChanged()

        coloPickerVisibilityRequestFlow
            .combine(colorIsPremiumFlow) { shouldShow, colorIsPremium -> shouldShow to colorIsPremium }
            .onEach { (shouldShow, colorIsPremium) -> toggleColorPicker(shouldShow, colorIsPremium) }
            .launchIn(lifecycleScope)

        val autocompleteSuggestionsSizeFlow = store.state
            .map { it.autocompleteSuggestions.size }

        autoCompleteQueryFlow
            .combine(hasMoreThanOneWorkspaceFlow) { query, hasMoreThanOneWorkspace ->
                if (query !is ProjectAutocompleteQuery.WorkspaceQuery || hasMoreThanOneWorkspace) query
                else ProjectAutocompleteQuery.None
            }
            .combine(autocompleteSuggestionsSizeFlow) { target, size -> target to size }
            .onEach { (query, size) -> toggleAutocompletePopup(query, size) }
            .launchIn(lifecycleScope)

        store.state
            .map { it.autocompleteQuery }
            .distinctUntilChanged()
            .onEach { client_workspace_edit_text.setSafeText(it.name) }
            .launchIn(lifecycleScope)

        client_workspace_edit_text.onDescriptionChanged
            .combine(autoCompleteQueryFlow) { (text, _), target ->
                when (target) {
                    ProjectAutocompleteQuery.None -> ProjectAutocompleteQuery.None
                    is ProjectAutocompleteQuery.WorkspaceQuery -> ProjectAutocompleteQuery.WorkspaceQuery(text)
                    is ProjectAutocompleteQuery.ClientQuery -> ProjectAutocompleteQuery.ClientQuery(text)
                }
            }
            .distinctUntilChanged()
            .onEach { store.dispatch(ProjectAction.AutocompleteDescriptionEntered(it)) }
            .launchIn(lifecycleScope)

        val bottomSheetBehavior = (dialog as BottomSheetDialog).behavior
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    @ExperimentalCoroutinesApi
    @FlowPreview
    private fun toggleAutocompletePopup(query: ProjectAutocompleteQuery, size: Int) {
        if (query is ProjectAutocompleteQuery.None) {
            autocompleteSuggestionsPopup.isShowing = false
            autocompleteSuggestionsPopup.dismiss()
            autocompleteSuggestionsRevealAnimator.hideEditText()
        } else {
            autocompleteSuggestionsPopup.isShowing = true
            client_workspace_edit_text.hint = getString(
                if (query is ProjectAutocompleteQuery.WorkspaceQuery) R.string.search_for_workspaces
                else R.string.search_for_clients
            )
            autocompleteSuggestionsRevealAnimator.revealEditText()
            autocompleteSuggestionsPopup.show(Position.Above, size)
        }
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onDestroyView() {
        super.onDestroyView()

        colorPickerAnimator.finish()
        project_name_edit_text.removeTextChangedListener(projectNameChangedListener)
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    private fun toggleColorPicker(shouldShowPicker: Boolean, currentColorIsPremium: Boolean) {
        if (shouldShowPicker) {
            colorPickerAnimator.showPicker(currentColorIsPremium) {
                project_name_edit_text.clearFocus()
                activity?.tryHidingKeyboard(project_name_edit_text)
            }
        } else {
            project_name_edit_text.requestFocus()
            activity?.tryShowingKeyboardFor(project_name_edit_text)
            colorPickerAnimator.hidePicker()
        }
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    private fun onColorTapped(colorViewModel: ColorViewModel) {
        when (colorViewModel) {
            is ColorViewModel.DefaultColor -> {
                store.dispatch(ProjectAction.ColorPicked(colorViewModel.color))
                coloPickerVisibilityRequestFlow.value = false
            }
            is ColorViewModel.CustomColor -> {
                store.dispatch(
                    listOf(
                        ProjectAction.ColorValueChanged(value_picker.value),
                        ProjectAction.ColorHueSaturationChanged(hue_saturation_picker.hue, hue_saturation_picker.saturation)
                    )
                )
            }
            ColorViewModel.PremiumLocked -> {
                // TODO Show the awareness popup
            }
        }
    }

    private fun ProjectState.selectedWorkspace(): Workspace {
        return workspaces[editableProject.workspaceId]
            ?: throw IllegalStateException("Editable time entry's workspace Id doesn't exist")
    }

    private fun ProjectState.selectedClient(): Client? {
        return clients[editableProject.clientId]
    }
}
