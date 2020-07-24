package com.toggl.calendar.datepicker.domain

import com.toggl.common.services.time.TimeService
import io.mockk.mockk
import java.time.OffsetDateTime

fun createInitialState(
    selectedDate: OffsetDateTime = OffsetDateTime.parse("2020-02-20T20:20:20+00:00")
) = CalendarDatePickerState(
    selectedDate
)

fun createReducer(
    timeService: TimeService = mockk()
) = CalendarDatePickerReducer(
    timeService
)