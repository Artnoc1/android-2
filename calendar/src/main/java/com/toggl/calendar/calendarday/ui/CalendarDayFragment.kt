package com.toggl.calendar.calendarday.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.toggl.calendar.R
import com.toggl.calendar.calendarday.domain.CalendarDayAction
import com.toggl.calendar.calendarday.domain.CalendarItemsSelector
import com.toggl.common.services.permissions.PermissionRequesterService
import com.toggl.common.services.permissions.requestCalendarPermissionIfNeeded
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_calendarday.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts

@AndroidEntryPoint
class CalendarDayFragment : Fragment(R.layout.fragment_calendarday) {

    @Inject lateinit var calendarItemsSelector: CalendarItemsSelector
    @Inject lateinit var permissionService: PermissionRequesterService

    private val store: CalendarDayStoreViewModel by viewModels()

    @FlowPreview
    @ExperimentalContracts
    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenResumed {
            permissionService.requestCalendarPermissionIfNeeded()
        }

        store.state
            .map { calendarItemsSelector.select(it).invoke(it.date) }
            .distinctUntilChanged()
            .onEach { calendar_widget.updateList(it) }
            .launchIn(lifecycleScope)

        calendar_widget.itemTappedFlow
            .onEach {
                store.dispatch(CalendarDayAction.ItemTapped(it))
            }.launchIn(lifecycleScope)

        calendar_widget.emptySpaceLongPressedFlow
            .onEach {
                store.dispatch(CalendarDayAction.EmptyPositionLongPressed(it))
            }.launchIn(lifecycleScope)

        calendar_widget.startTimeFlow
            .onEach {
                store.dispatch(CalendarDayAction.StartTimeDragged(it))
            }.launchIn(lifecycleScope)

        calendar_widget.endTimeFlow
            .onEach {
                store.dispatch(CalendarDayAction.StopTimeDragged(it))
            }.launchIn(lifecycleScope)

        calendar_widget.offsetFlow
            .onEach {
                store.dispatch(CalendarDayAction.TimeEntryDragged(it))
            }.launchIn(lifecycleScope)
    }
}
