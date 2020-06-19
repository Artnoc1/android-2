package com.toggl.ui

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.toggl.R
import com.toggl.TogglApplication
import com.toggl.architecture.core.Store
import com.toggl.common.BottomNavigationProvider
import com.toggl.common.deepLinks
import com.toggl.domain.AppAction
import com.toggl.domain.AppState
import com.toggl.domain.loading.LoadingAction
import com.toggl.common.feature.navigation.Router
import com.toggl.timer.common.domain.TimerAction
import com.toggl.timer.suggestions.domain.SuggestionsAction
import kotlinx.android.synthetic.main.main_activity.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class MainActivity : AppCompatActivity(R.layout.main_activity), BottomNavigationProvider {

    @Inject lateinit var router: Router
    @Inject lateinit var store: Store<AppState, AppAction>

    override var isBottomNavigationVisible: Boolean
        get() = bottom_navigation.isVisible
        set(value) { bottom_navigation.isVisible = value }

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (applicationContext as TogglApplication).appComponent.inject(this)

        store.dispatch(AppAction.Loading(LoadingAction.StartLoading))

        store.state
            .map { it.backStack }
            .onEach { router.processNewBackStack(it, nav_host_fragment.findNavController()) }
            .launchIn(lifecycleScope)

        bottom_navigation.setOnNavigationItemSelectedListener(::changeTab)
        bottom_navigation.setOnNavigationItemReselectedListener(::scrollUpOnTab)
    }

    private fun changeTab(menuItem: MenuItem): Boolean {
        nav_host_fragment.findNavController().navigate(
            when (menuItem.itemId) {
                R.id.timer_tab -> deepLinks.timeEntriesLog
                R.id.reports_tab -> deepLinks.reports
                R.id.calendar_tab -> deepLinks.calendar
                else -> throw NotImplementedError()
            }
        )
        return true
    }

    override fun onResume() {
        super.onResume()
        store.dispatch(AppAction.Timer(TimerAction.Suggestions(SuggestionsAction.LoadSuggestions)))
    }

    private fun scrollUpOnTab(menuItem: MenuItem) {
        Log.i("MainActivity", menuItem.title.toString())
    }
}
