package com.toggl.timer.log.ui

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.MergeAdapter
import com.google.android.material.snackbar.Snackbar
import com.toggl.architecture.extensions.select
import com.toggl.common.Constants.timeEntryDeletionDelayMs
import com.toggl.common.extensions.adjustPaddingToStatusBarInsets
import com.toggl.common.extensions.performClickHapticFeedback
import com.toggl.common.services.time.TimeService
import com.toggl.models.common.SwipeDirection
import com.toggl.timer.R
import com.toggl.timer.log.domain.FlatTimeEntryViewModel
import com.toggl.timer.log.domain.TimeEntriesLogAction
import com.toggl.timer.log.domain.TimeEntriesLogSelector
import com.toggl.timer.log.domain.TimeEntryGroupViewModel
import com.toggl.timer.log.domain.TimeEntryViewModel
import com.toggl.timer.suggestions.domain.SuggestionsAction
import com.toggl.timer.suggestions.ui.SuggestionsAdapter
import com.toggl.timer.suggestions.ui.SuggestionsLogSelector
import com.toggl.timer.suggestions.ui.SuggestionsStoreViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_time_entries_log.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TimeEntriesLogFragment : Fragment(R.layout.fragment_time_entries_log) {

    @Inject lateinit var timeService: TimeService
    @Inject lateinit var timeEntriesLogSelector: TimeEntriesLogSelector
    @Inject lateinit var suggestionsLogSelector: SuggestionsLogSelector

    private val store: TimeEntriesLogStoreViewModel by viewModels()
    private val suggestionsStore: SuggestionsStoreViewModel by viewModels()

    private val timeEntriesAdapter = TimeEntriesLogAdapter(
        {
            context?.performClickHapticFeedback()
            store.dispatch(TimeEntriesLogAction.TimeEntryTapped(it))
        },
        {
            context?.performClickHapticFeedback()
            store.dispatch(TimeEntriesLogAction.TimeEntryGroupTapped(it))
        },
        { store.dispatch(TimeEntriesLogAction.ContinueButtonTapped(it)) },
        { store.dispatch(TimeEntriesLogAction.ToggleTimeEntryGroupTapped(it)) }
    )

    private val suggestionsAdapter = SuggestionsAdapter {
        context?.performClickHapticFeedback()
        suggestionsStore.dispatch(SuggestionsAction.SuggestionTapped(it))
    }

    private val suggestionsHeaderAdapter by lazy { SectionHeaderAdapter(getString(R.string.log_suggestions_title)) }
    private val timeEntriesHeaderAdapter by lazy { SectionHeaderAdapter(getString(R.string.log_time_entries_title)) }

    private val mergeAdapter by lazy {
        MergeAdapter(
            suggestionsHeaderAdapter,
            suggestionsAdapter,
            timeEntriesHeaderAdapter,
            timeEntriesAdapter
        )
    }

    private var snackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        app_bar_layout.adjustPaddingToStatusBarInsets()
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        recycler_view.adapter = mergeAdapter
        val swipeCallback = createSwipeActionCallback(requireContext())
        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(recycler_view)

        suggestionsStore.select(suggestionsLogSelector)
            .distinctUntilChanged()
            .onEach {
                suggestionsAdapter.submitList(it)
                suggestionsHeaderAdapter.isVisible = it.isNotEmpty()
            }
            .launchIn(lifecycleScope)

        store.select(timeEntriesLogSelector)
            .distinctUntilChanged()
            .onEach {
                timeEntriesAdapter.submitList(it)
                timeEntriesHeaderAdapter.isVisible = it.isNotEmpty()
            }
            .launchIn(lifecycleScope)

        store.state
            .map { it.entriesPendingDeletion }
            .distinctUntilChanged()
            .onEach { showUndoDeletionSnackbar(it) }
            .launchIn(lifecycleScope)
    }

    override fun onDestroyView() {
        snackbar = null
        recycler_view.adapter = null
        (activity as AppCompatActivity).setSupportActionBar(null)
        super.onDestroyView()
    }

    private fun showUndoDeletionSnackbar(idsToDelete: Set<Long>) {
        snackbar?.dismiss()

        if (idsToDelete.isEmpty())
            return

        val deletionMessage = resources.getQuantityString(R.plurals.entriesDeleted, idsToDelete.size, idsToDelete.size)
        snackbar = Snackbar.make(coordinator_layout, deletionMessage, timeEntryDeletionDelayMs.toInt()).apply {
            anchorView = running_time_entry_layout
            setAction(getString(R.string.undo).toUpperCase(Locale.getDefault())) {
                store.dispatch(TimeEntriesLogAction.UndoButtonTapped)
            }
            show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings_action -> {
                store.dispatch(TimeEntriesLogAction.OpenSettingsButtonTapped)
                return true
            }
            else -> {}
        }
        return false
    }

    private fun onLogItemSwiped(adapterPosition: Int, swipeDirection: SwipeDirection) {
        val currentItems: List<TimeEntryViewModel> = timeEntriesAdapter.currentList
        if (adapterPosition >= currentItems.size) {
            timeEntriesAdapter.notifyDataSetChanged()
            return
        }

        when (val item = currentItems[adapterPosition]) {
            is FlatTimeEntryViewModel -> TimeEntriesLogAction.TimeEntrySwiped(item.id, swipeDirection)
            is TimeEntryGroupViewModel -> TimeEntriesLogAction.TimeEntryGroupSwiped(item.timeEntryIds, swipeDirection)
            else -> null
        }?.let(store::dispatch)

        if (swipeDirection == SwipeDirection.Right) {
            timeEntriesAdapter.notifyItemChanged(adapterPosition)
        }
    }

    private fun createSwipeActionCallback(context: Context): SwipeActionCallback {
        val leftSwipeColor = ContextCompat.getColor(context, R.color.stop_time_entry_button_background)
        val rightSwipeColor = ContextCompat.getColor(context, R.color.start_time_entry_button_background)
        val actionTextColor = ContextCompat.getColor(context, R.color.text_on_surface)
        val actionFontSize = resources.getDimension(R.dimen.swipe_action_font_size)
        val actionPadding = resources.getDimension(R.dimen.swipe_action_text_padding)

        val swipeLeftParams = SwipeActionParams(
            text = getString(R.string.delete),
            backgroundColor = leftSwipeColor,
            textColor = actionTextColor,
            fontSize = actionFontSize,
            textPadding = actionPadding
        )
        val swipeRightParams = SwipeActionParams(
            text = getString(R.string.continue_this),
            backgroundColor = rightSwipeColor,
            textColor = actionTextColor,
            fontSize = actionFontSize,
            textPadding = actionPadding
        )

        return SwipeActionCallback(
            swipeLeftParams = swipeLeftParams,
            swipeRightParams = swipeRightParams,
            onSwipeActionListener = ::onLogItemSwiped
        )
    }
}
