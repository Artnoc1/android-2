package com.toggl.calendar.calendarday.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.toggl.calendar.R
import com.toggl.calendar.datepicker.domain.CalendarDatePickerAction
import com.toggl.calendar.datepicker.ui.CalendarDatePickerStoreViewModel
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.common.feature.navigation.getRouteParam
import com.toggl.common.services.permissions.PermissionRequesterService
import com.toggl.common.services.permissions.requestCalendarPermissionIfNeeded
import com.toggl.models.common.SwipeDirection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_calendarday.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts

@AndroidEntryPoint
class CalendarDayFragment : Fragment(R.layout.fragment_calendarday) {
    private val store: CalendarDayStoreViewModel by activityViewModels()
    private val datePickerStore: CalendarDatePickerStoreViewModel by activityViewModels()

    @Inject lateinit var permissionService: PermissionRequesterService

    private lateinit var daysAdapter: CalendarDayPageFragmentAdapter

    private var currentPage = 0

    @FlowPreview
    @ExperimentalContracts
    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        daysAdapter = CalendarDayPageFragmentAdapter(this)
        calendar_day_pager.adapter = daysAdapter

        lifecycleScope.launchWhenResumed {
            permissionService.requestCalendarPermissionIfNeeded()
        }

        val dayChangedCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == currentPage) return
                val swipeDirection = if (position < currentPage) SwipeDirection.Left else SwipeDirection.Right
                currentPage = position
                datePickerStore.dispatch(CalendarDatePickerAction.DaySwiped(swipeDirection))
            }
        }
        calendar_day_pager.registerOnPageChangeCallback(dayChangedCallback)

        store.state
            .map { it.selectedDate.daysSinceToday() }
            .distinctUntilChanged()
            .onEach {
                calendar_day_pager.unregisterOnPageChangeCallback(dayChangedCallback)
                calendar_day_pager.currentItem = numberOfDaysInTheCalendar - it - 1
                currentPage = numberOfDaysInTheCalendar - it - 1
                calendar_day_pager.registerOnPageChangeCallback(dayChangedCallback)
            }.launchIn(lifecycleScope)

        store.state
            .map { it.backStack.getRouteParam<SelectedCalendarItem>() }
            .onEach { calendar_day_pager.isUserInputEnabled = it == null }
            .launchIn(lifecycleScope)
    }

    class CalendarDayPageFragmentAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount() = numberOfDaysInTheCalendar

        override fun createFragment(position: Int): Fragment = CalendarDayPageFragment.create(numberOfDaysInTheCalendar - position - 1L)
    }

    private fun OffsetDateTime.daysSinceToday(): Int {
        val today = OffsetDateTime.now()
        return today.dayOfYear - this.dayOfYear
    }

    companion object {
        private const val numberOfDaysInTheCalendar = 14
    }
}