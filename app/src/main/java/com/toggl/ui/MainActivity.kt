package com.toggl.ui

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.Navigation
import androidx.navigation.fragment.DialogFragmentNavigator
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.ui.setupWithNavController
import com.toggl.R
import com.toggl.architecture.core.Store
import com.toggl.common.feature.navigation.BottomSheetNavigator
import com.toggl.common.feature.navigation.Router
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import com.toggl.domain.loading.LoadingAction
import com.toggl.timer.common.domain.TimerAction
import com.toggl.timer.suggestions.domain.SuggestionsAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.main_activity) {
    @Inject lateinit var router: Router
    @Inject lateinit var store: Store<AppState, AppAction>
    @Inject lateinit var bottomSheetNavigator: BottomSheetNavigator

    private val updateBottomBarVisibilityListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        bottom_navigation.isVisible = when (destination.id) {
            R.id.time_entries_log,
            R.id.reports,
            R.id.calendar,
            R.id.start_time_entry_dialog -> true
            else -> false
        }

        if (destination.id == R.id.time_entries_log) {
            store.dispatch(AppAction.Loading(LoadingAction.StartLoading))
        }
    }

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navController = setUpNavigation()
        store.state
            .map { it.backStack }
            .onEach { router.processNewBackStack(it, navController) }
            .launchIn(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        store.dispatch(AppAction.Timer(TimerAction.Suggestions(SuggestionsAction.LoadSuggestions)))
    }

    private fun setUpNavigation(): NavHostController {
        val navController = createNavHostController()
        Navigation.setViewNavController(nav_host_fragment, navController)

        bottom_navigation.setupWithNavController(navController)
        bottom_navigation.setOnNavigationItemReselectedListener(::scrollUpOnTab)
        navController.setLifecycleOwner(this)
        navController.setOnBackPressedDispatcher(onBackPressedDispatcher)

        return navController
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
}
