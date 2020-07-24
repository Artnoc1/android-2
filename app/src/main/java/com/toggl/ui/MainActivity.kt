package com.toggl.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.Navigation
import androidx.navigation.fragment.DialogFragmentNavigator
import androidx.navigation.fragment.FragmentNavigator
import com.toggl.R
import com.toggl.architecture.core.Store
import com.toggl.common.extensions.withLatestFrom
import com.toggl.common.feature.navigation.BottomSheetNavigator
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.Router
import com.toggl.common.feature.navigation.handleBackPressesEmitting
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import com.toggl.domain.Tab
import com.toggl.domain.loading.LoadingAction
import com.toggl.timer.common.domain.TimerAction
import com.toggl.timer.suggestions.domain.SuggestionsAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.lang.IllegalStateException
import javax.inject.Inject

@FlowPreview
@AndroidEntryPoint
@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity(R.layout.main_activity) {
    @Inject lateinit var router: Router
    @Inject lateinit var store: Store<AppState, AppAction>
    @Inject lateinit var bottomSheetNavigator: BottomSheetNavigator

    private val backButtonPressedChannel = BroadcastChannel<Unit>(1)
    private val updateBottomBarVisibilityListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        bottom_navigation.isVisible = when (destination.id) {
            R.id.time_entries_log,
            R.id.reports,
            R.id.calendar,
            R.id.start_time_entry_dialog -> true
            else -> false
        }
    }

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFullScreenFlags()

        val navController = setUpNavigation()
        val backStackFlow = store.state.map { it.backStack }

        backStackFlow
            .onEach { router.processNewBackStack(it, navController) }
            .launchIn(lifecycleScope)

        backStackFlow
            .onEach { updateSelectedTabIfNeeded(it) }
            .launchIn(lifecycleScope)

        backButtonPressedChannel.asFlow()
            .withLatestFrom(backStackFlow) { _, backStack -> backStack.size > 1 }
            .onEach { shouldPop ->
                if (shouldPop) {
                    store.dispatch(AppAction.BackButtonPressed)
                } else {
                    finish()
                }
            }.launchIn(lifecycleScope)
    }

    private fun updateSelectedTabIfNeeded(backStack: List<Route>) {
        val itemToSelect = when (backStack.lastOrNull()) {
            Route.Timer -> R.id.time_entries_log
            Route.Reports -> R.id.reports
            Route.Calendar -> R.id.calendar
            else -> null
        } ?: return

        if (bottom_navigation.selectedItemId == itemToSelect)
            return

        bottom_navigation.selectedItemId = itemToSelect
    }

    override fun onResume() {
        super.onResume()
        store.dispatch(AppAction.Loading(LoadingAction.StartLoading))
        store.dispatch(AppAction.Timer(TimerAction.Suggestions(SuggestionsAction.LoadSuggestions)))
    }

    private fun setUpNavigation(): NavHostController {
        val navController = createNavHostController()
        Navigation.setViewNavController(nav_host_fragment, navController)

        bottom_navigation.setOnNavigationItemSelectedListener(::tabSelected)
        bottom_navigation.setOnNavigationItemReselectedListener(::scrollUpOnTab)

        handleBackPressesEmitting { backButtonPressedChannel.offer(Unit) }

        return navController
    }

    private fun tabSelected(menuItem: MenuItem): Boolean {
        store.dispatch(AppAction.TabSelected(
            when (menuItem.itemId) {
                R.id.time_entries_log -> Tab.Timer
                R.id.reports -> Tab.Reports
                R.id.calendar -> Tab.Calendar
                else -> throw IllegalStateException()
            }
        ))

        return true
    }

    private fun scrollUpOnTab(menuItem: MenuItem) {
        Log.i("MainActivity", menuItem.title.toString())
    }

    private fun createNavHostController() =
        NavHostController(this).also {
            it.navigatorProvider.addNavigator(bottomSheetNavigator)
            it.navigatorProvider.addNavigator(DialogFragmentNavigator(this, supportFragmentManager))
            it.navigatorProvider.addNavigator(FragmentNavigator(this, supportFragmentManager, R.id.nav_host_fragment))
            it.setGraph(R.navigation.tabs_nav_graph)
            it.addOnDestinationChangedListener(updateBottomBarVisibilityListener)
        }

    private fun setFullScreenFlags() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            if (resources.getBoolean(R.bool.is_dark_theme)) 0
            else View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }
}
