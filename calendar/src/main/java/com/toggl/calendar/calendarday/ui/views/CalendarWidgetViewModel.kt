package com.toggl.calendar.calendarday.ui.views

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CalendarWidgetViewModel : ViewModel() {
    val scrollOffset = MutableLiveData<Int>(0)
    val hourHeight = MutableLiveData<Float>(0f)

    fun updateScrollOffset(offset: Int) {
        if (scrollOffset.value != offset) {
            scrollOffset.value = offset
        }
    }

    fun updateHourHeight(height: Float) {
        if (hourHeight.value != height) {
            hourHeight.value = height
        }
    }
}