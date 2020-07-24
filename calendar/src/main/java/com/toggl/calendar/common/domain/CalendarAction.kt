package com.toggl.calendar.common.domain

import com.toggl.architecture.core.ActionWrapper
import com.toggl.calendar.calendarday.domain.CalendarDayAction
import com.toggl.calendar.calendarday.domain.formatForDebug
import com.toggl.calendar.contextualmenu.domain.ContextualMenuAction
import com.toggl.calendar.contextualmenu.domain.formatForDebug
import com.toggl.calendar.datepicker.domain.CalendarDatePickerAction
import com.toggl.calendar.datepicker.domain.formatForDebug

sealed class CalendarAction {
    data class CalendarDay(override val action: CalendarDayAction) : CalendarAction(), ActionWrapper<CalendarDayAction>
    data class DatePicker(override val action: CalendarDatePickerAction) : CalendarAction(), ActionWrapper<CalendarDatePickerAction>
    data class ContextualMenu(override val action: ContextualMenuAction) : CalendarAction(), ActionWrapper<ContextualMenuAction>

    companion object
}

fun CalendarAction.formatForDebug() =
    when (this) {
        is CalendarAction.CalendarDay -> action.formatForDebug()
        is CalendarAction.DatePicker -> action.formatForDebug()
        is CalendarAction.ContextualMenu -> action.formatForDebug()
    }