package com.toggl.calendar.calendarday.domain

import com.toggl.calendar.common.domain.CalendarItem
import com.toggl.calendar.common.domain.duration
import com.toggl.calendar.common.domain.startTime
import com.toggl.common.feature.services.calendar.CalendarEvent
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.TimeEntry
import io.kotest.matchers.collections.shouldHaveSize
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ListAssert
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

@DisplayName("The CalendarLayoutCalculator")
class CalendarLayoutCalculatorTest {

    private fun createTimeEntryCalendarItem(
        id: Long,
        startTime: OffsetDateTime,
        duration: Duration,
        description: String
    ): CalendarItem =
        CalendarItem.TimeEntry(
            TimeEntry(
                id = id,
                description = description,
                startTime = startTime,
                duration = duration,
                billable = false,
                workspaceId = 1,
                projectId = null,
                taskId = null,
                isDeleted = false,
                tagIds = emptyList()
            )
        )

    private fun createCalendarEventCalendarItem(
        id: String,
        startTime: OffsetDateTime,
        duration: Duration,
        description: String
    ): CalendarItem =
        CalendarItem.CalendarEvent(
            CalendarEvent(
                id,
                startTime,
                duration,
                description,
                "#c2c2c2",
                id
            )
        )

    var timeService: TimeService = mockk()

    init {
        every { timeService.now() }.returns(OffsetDateTime.now())
    }

    @Test
    fun `When the calendar items do not overlap with each other`() {
        val calendarItems = listOf(
            createTimeEntryCalendarItem(
                1, OffsetDateTime.of(2018, 11, 21, 8, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(30), "Item 1"
            ),
            createTimeEntryCalendarItem(
                2,
                OffsetDateTime.of(2018, 11, 21, 9, 0, 0, 0, ZoneOffset.UTC),
                Duration.ofMinutes(30),
                "Item 2"
            ),
            createTimeEntryCalendarItem(
                3,
                OffsetDateTime.of(2018, 11, 21, 10, 0, 0, 0, ZoneOffset.UTC),
                Duration.ofMinutes(30),
                "Item 3"
            )
        )
        val calculator = CalendarLayoutCalculator(timeService)

        val layoutAttributes = calculator.calculateLayoutAttributes(calendarItems)

        layoutAttributes shouldHaveSize calendarItems.size
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[0].startTime && it.totalColumns == 1
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[1].startTime && it.totalColumns == 1
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[2].startTime && it.totalColumns == 1
        }
    }

    @Test
    fun `When two items overlap`() {
        val calendarItems = listOf(
            createTimeEntryCalendarItem(
                1,
                OffsetDateTime.of(2018, 11, 21, 8, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(90), "Item 1"
            ),
            createTimeEntryCalendarItem(
                2,
                OffsetDateTime.of(2018, 11, 21, 9, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(30), "Item 2"
            ),
            createTimeEntryCalendarItem(
                3,
                OffsetDateTime.of(2018, 11, 21, 10, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(30), "Item 3"
            )
        )

        val calculator = CalendarLayoutCalculator(timeService)

        val layoutAttributes = calculator.calculateLayoutAttributes(calendarItems)

        layoutAttributes shouldHaveSize calendarItems.size

        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[0].startTime && it.totalColumns == 2 && it.columnIndex == 0
        }

        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[1].startTime && it.totalColumns == 2 && it.columnIndex == 1
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[2].startTime && it.totalColumns == 1
        }
    }

    @Test
    fun `When two items should overlap because of minimum duration for ui purposes`() {
        val calendarItems = listOf(
            createTimeEntryCalendarItem(
                1,
                OffsetDateTime.of(2018, 11, 21, 8, 0, 0, 0, ZoneOffset.UTC), Duration.ofSeconds(10), "Item 1"
            ),
            createTimeEntryCalendarItem(
                2,
                OffsetDateTime.of(2018, 11, 21, 8, 0, 11, 0, ZoneOffset.UTC), Duration.ofSeconds(10), "Item 2"
            )
        )

        val calculator = CalendarLayoutCalculator(timeService)

        val layoutAttributes = calculator.calculateLayoutAttributes(calendarItems)

        layoutAttributes shouldHaveSize calendarItems.size

        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[0].startTime && it.totalColumns == 2 && it.columnIndex == 0
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[1].startTime && it.totalColumns == 2 && it.columnIndex == 1
        }
    }

    @Test
    fun `When three items overlap but only two columns are required`() {
        val calendarItems = listOf(
            createTimeEntryCalendarItem(
                1,
                OffsetDateTime.of(2018, 11, 21, 8, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(90), "Item 1"
            ),
            createTimeEntryCalendarItem(
                2,
                OffsetDateTime.of(2018, 11, 21, 9, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(90), "Item 2"
            ),
            createTimeEntryCalendarItem(
                3,
                OffsetDateTime.of(2018, 11, 21, 10, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(30), "Item 3"
            )
        )
        val calculator = CalendarLayoutCalculator(timeService)

        val layoutAttributes = calculator.calculateLayoutAttributes(calendarItems)

        layoutAttributes shouldHaveSize calendarItems.size
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[0].startTime && it.totalColumns == 2 && it.columnIndex == 0
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[1].startTime && it.totalColumns == 2 && it.columnIndex == 1
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[2].startTime && it.totalColumns == 2 && it.columnIndex == 0
        }
    }

    @Test
    fun `When items overlap in two different groups with different number of columns`() {
        val calendarItems = listOf(
            createTimeEntryCalendarItem(
                1,
                OffsetDateTime.of(2018, 11, 21, 8, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(180), "Item 1"
            ),
            createTimeEntryCalendarItem(
                2,
                OffsetDateTime.of(2018, 11, 21, 9, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(90), "Item 2"
            ),
            createTimeEntryCalendarItem(
                3,
                OffsetDateTime.of(2018, 11, 21, 10, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(90), "Item 3"
            ),
            createTimeEntryCalendarItem(
                4,
                OffsetDateTime.of(2018, 11, 21, 14, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(90), "Item 4"
            ),
            createTimeEntryCalendarItem(
                5,
                OffsetDateTime.of(2018, 11, 21, 15, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(90), "Item 5"
            )
        )
        val calculator = CalendarLayoutCalculator(timeService)

        val layoutAttributes = calculator.calculateLayoutAttributes(calendarItems)

        layoutAttributes shouldHaveSize calendarItems.size
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[0].startTime && it.totalColumns == 3 && it.columnIndex == 0
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[1].startTime && it.totalColumns == 3 && it.columnIndex == 1
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[2].startTime && it.totalColumns == 3 && it.columnIndex == 2
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[3].startTime && it.totalColumns == 2 && it.columnIndex == 0
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[4].startTime && it.totalColumns == 2 && it.columnIndex == 1
        }
    }

    @Test
    fun `Calendar events have their own columns to the left`() {
        val calendarItems = listOf(
            createTimeEntryCalendarItem(
                1,
                OffsetDateTime.of(2018, 11, 21, 8, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(90), "Item 1"
            ),
            createCalendarEventCalendarItem(
                "2",
                OffsetDateTime.of(2018, 11, 21, 9, 0, 0, 0, ZoneOffset.UTC),
                Duration.ofMinutes(60),
                "Item 2"
            ),
            createTimeEntryCalendarItem(
                3,
                OffsetDateTime.of(2018, 11, 21, 9, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(30), "Item 3"
            ),
            createTimeEntryCalendarItem(
                4,
                OffsetDateTime.of(2018, 11, 21, 11, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(120), "Item 4"
            ),
            createCalendarEventCalendarItem(
                "5",
                OffsetDateTime.of(2018, 11, 21, 11, 0, 0, 0, ZoneOffset.UTC),
                Duration.ofMinutes(60),
                "Item 5"
            )
        )
        val calculator = CalendarLayoutCalculator(timeService)

        val layoutAttributes = calculator.calculateLayoutAttributes(calendarItems)

        layoutAttributes shouldHaveSize calendarItems.size

        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[0].startTime && it.totalColumns == 3 && it.columnIndex == 1
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[1].startTime && it.totalColumns == 3 && it.columnIndex == 0
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[2].startTime && it.totalColumns == 3 && it.columnIndex == 2
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[3].startTime && it.totalColumns == 2 && calendarItems[3].duration == it.duration && it.columnIndex == 1
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[4].startTime && it.totalColumns == 2 && calendarItems[4].duration == it.duration && it.columnIndex == 0
        }
    }

    @Test
    fun `Overlapping calendar events are always to the left`() {
        val calendarItems = listOf(
            createTimeEntryCalendarItem(
                1,
                OffsetDateTime.of(2018, 11, 21, 8, 0, 0, 0, ZoneOffset.UTC), Duration.ofMinutes(180), "Item 1"
            ),
            createCalendarEventCalendarItem(
                "2",
                OffsetDateTime.of(2018, 11, 21, 9, 0, 0, 0, ZoneOffset.UTC),
                Duration.ofMinutes(180),
                "Item 2"
            ),
            createCalendarEventCalendarItem(
                "3",
                OffsetDateTime.of(2018, 11, 21, 10, 0, 0, 0, ZoneOffset.UTC),
                Duration.ofMinutes(180),
                "Item 3"
            )
        )

        val calculator = CalendarLayoutCalculator(timeService)

        val layoutAttributes = calculator.calculateLayoutAttributes(calendarItems)

        layoutAttributes shouldHaveSize calendarItems.size
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[0].startTime && it.totalColumns == 3 && it.columnIndex == 2
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[1].startTime && it.totalColumns == 3 && it.columnIndex == 0
        }
        assertThat(layoutAttributes).matchesOnlyOnce {
            it.startTime == calendarItems[2].startTime && it.totalColumns == 3 && it.columnIndex == 1
        }
    }

    private fun <ELEMENT> ListAssert<ELEMENT>.matchesOnlyOnce(predicate: (ELEMENT) -> Boolean) {
        anyMatch(predicate)
        filteredOn(predicate).size().isOne
    }
}