package com.toggl.calendar.datepicker.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.toggl.architecture.extensions.select
import com.toggl.calendar.R
import com.toggl.calendar.calendarday.ui.CalendarDayStoreViewModel
import com.toggl.calendar.datepicker.domain.CalendarDatePickerAction
import com.toggl.calendar.datepicker.domain.DatePickerSelector
import com.toggl.calendar.datepicker.domain.DayHeaderSelector
import com.toggl.common.extensions.absoluteDurationBetween
import com.toggl.common.feature.extensions.formatForDisplaying
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.common.feature.navigation.getRouteParam
import com.toggl.models.common.SwipeDirection
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_calendardatepicker.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.time.DayOfWeek
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.TextStyle
import java.util.Locale
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer

@AndroidEntryPoint
class CalendarDatePickerFragment : Fragment(R.layout.fragment_calendardatepicker) {

    private val store: CalendarDatePickerStoreViewModel by activityViewModels()
    private val calendarDayStoreViewModel: CalendarDayStoreViewModel by activityViewModels()

    @Inject
    lateinit var datePickerSelector: DatePickerSelector

    @Inject
    lateinit var dayHeaderSelector: DayHeaderSelector
    private var currentRunningDayHeaderViewModel: CalendarDayHeaderViewModel.DayWithRunningTimeEntry? = null

    private lateinit var adapter: WeekStripeDatePickerAdapter
    private lateinit var handler: Handler
    private var canSwitchDays = true
    private var currentPage = 0
    private var headerUpdateTimer: Timer? = null

    @SuppressLint("ClickableViewAccessibility")
    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handler = Handler(Looper.getMainLooper())
        adapter = WeekStripeDatePickerAdapter {
            if (canSwitchDays) {
                store.dispatch(CalendarDatePickerAction.DaySelected(it))
            }
        }
        week_stripe_pager.adapter = adapter
        currentPage = week_stripe_pager.currentItem

        val pageChangeListener = object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                if (position == currentPage) return
                val swipeDirection = if (position < currentPage) SwipeDirection.Left else SwipeDirection.Right
                currentPage = position
                store.dispatch(CalendarDatePickerAction.WeekStripeSwiped(swipeDirection))
            }
        }

        week_labels_container.setOnTouchListener { _, event ->
            week_stripe_pager.onTouchEvent(event)
        }
        week_stripe_pager.addOnPageChangeListener(pageChangeListener)

        val datePickerViewModelFlow = store.select(datePickerSelector)

        datePickerViewModelFlow
            .onEach {
                adapter.updateWeekDays(it.weeks)
                week_stripe_pager.removeOnPageChangeListener(pageChangeListener)
                week_stripe_pager.currentItem = it.selectedWeek
                currentPage = it.selectedWeek
                week_stripe_pager.addOnPageChangeListener(pageChangeListener)
            }
            .launchIn(lifecycleScope)

        datePickerViewModelFlow
            .distinctUntilChangedBy { it.selectedDay }
            .onEach { adapter.updateSelectedDate(it.selectedDay) }
            .launchIn(lifecycleScope)

        datePickerViewModelFlow
            .map { it.weekHeaderLabels }
            .onEach { updateWeekHeaders(it) }
            .launchIn(lifecycleScope)

        calendarDayStoreViewModel.state
            .map { it.backStack.getRouteParam<SelectedCalendarItem>() == null }
            .onEach {
                canSwitchDays = it
                week_stripe_pager.isUserInputEnabled = it
            }
            .launchIn(lifecycleScope)

        calendarDayStoreViewModel.select(dayHeaderSelector)
            .onEach { setupDayHeader(it) }
            .launchIn(lifecycleScope)
    }

    override fun onResume() {
        super.onResume()
        currentRunningDayHeaderViewModel?.run { setupDayHeader(this) }
    }

    override fun onPause() {
        clearUpdateTimer()
        super.onPause()
    }

    override fun onDestroyView() {
        clearUpdateTimer()
        super.onDestroyView()
    }

    private fun setupDayHeader(calendarDayHeaderViewModel: CalendarDayHeaderViewModel) {
        clearUpdateTimer()
        currentRunningDayHeaderViewModel = null
        when (calendarDayHeaderViewModel) {
            is CalendarDayHeaderViewModel.StoppedDay -> updateDayHeader(
                calendarDayHeaderViewModel.dayLabel,
                calendarDayHeaderViewModel.hoursSumLabel
            )
            is CalendarDayHeaderViewModel.DayWithRunningTimeEntry -> scheduleDayHeaderRunningTimeEntryDayUpdates(calendarDayHeaderViewModel)
        }
    }

    private fun clearUpdateTimer() {
        headerUpdateTimer?.cancel()
        headerUpdateTimer?.purge()
        headerUpdateTimer = null
    }

    private fun scheduleDayHeaderRunningTimeEntryDayUpdates(dayHeaderVM: CalendarDayHeaderViewModel.DayWithRunningTimeEntry) {
        currentRunningDayHeaderViewModel = dayHeaderVM
        headerUpdateTimer = fixedRateTimer(period = Duration.ofSeconds(1).toMillis()) {
            val totalDuration = dayHeaderVM.durationWithoutRunningTimeEntry +
                dayHeaderVM.runningTimeEntryStartTime.absoluteDurationBetween(OffsetDateTime.now())
            handler.post { updateDayHeader(dayHeaderVM.dayLabel, totalDuration.formatForDisplaying()) }
        }
    }

    private fun updateDayHeader(dayLabel: String, hoursSumLabel: String) {
        if (!isAdded || !isVisible) return
        headerDateTextView.text = dayLabel
        headerTimeEntriesDurationTextView.text = hoursSumLabel
    }

    private fun updateWeekHeaders(weekHeaders: List<DayOfWeek>) {
        weekDayOneLabel.text = weekHeaders[0].getDisplayName(TextStyle.NARROW, Locale.getDefault())
        weekDayTwoLabel.text = weekHeaders[1].getDisplayName(TextStyle.NARROW, Locale.getDefault())
        weekDayThreeLabel.text = weekHeaders[2].getDisplayName(TextStyle.NARROW, Locale.getDefault())
        weekDayFourLabel.text = weekHeaders[3].getDisplayName(TextStyle.NARROW, Locale.getDefault())
        weekDayFiveLabel.text = weekHeaders[4].getDisplayName(TextStyle.NARROW, Locale.getDefault())
        weekDaySixLabel.text = weekHeaders[5].getDisplayName(TextStyle.NARROW, Locale.getDefault())
        weekDaySevenLabel.text = weekHeaders[6].getDisplayName(TextStyle.NARROW, Locale.getDefault())
    }
}