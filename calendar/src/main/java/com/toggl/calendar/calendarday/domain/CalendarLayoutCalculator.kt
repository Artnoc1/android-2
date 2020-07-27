package com.toggl.calendar.calendarday.domain

import com.toggl.calendar.common.domain.CalendarItem
import com.toggl.calendar.common.domain.endTime
import com.toggl.calendar.common.domain.startTime
import com.toggl.common.feature.models.SelectedCalendarItem
import com.toggl.common.services.time.TimeService
import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

typealias CalendarItemGroup = MutableList<CalendarItem>
typealias CalendarItemGroups = MutableList<CalendarItemGroup>

@Singleton
class CalendarLayoutCalculator @Inject constructor(private val timeService: TimeService) {

    private val offsetFromNow: Duration = Duration.ofMinutes(7)
    private val minimumDurationForUIPurposes: Duration = Duration.ofMinutes(15)

    fun calculateLayoutAttributes(calendarItems: List<CalendarItem>): List<CalendarItem> {
        if (calendarItems.isEmpty())
            return emptyList()

        return calendarItems
            .sortedBy(CalendarItem::startTime)
            .fold(mutableListOf(), ::groupOverlappingItems)
            .flatMap(::calculateLayoutAttributesFor)
            .toList()
    }

    /**
     * Aggregates the calendar items into buckets. Each bucket contains the sequence of overlapping items.
     * The items in a bucket don't overlap all with each other, but cannot overlap with items in other buckets.
     *
     * @param buckets The list of aggregated items
     * @param calendarItem The item to put in a bucket
     * @return A list of buckets
     */
    private fun groupOverlappingItems(
        buckets: CalendarItemGroups,
        calendarItem: CalendarItem
    ): CalendarItemGroups {
        if (buckets.isEmpty()) {
            buckets.add(mutableListOf(calendarItem))
            return buckets
        }

        val now = timeService.now()
        val group = buckets.last()
        val maxEndTime = group.map { calculateEndTimeWith(it, now) }.maxOrNull() ?: OffsetDateTime.MIN
        if (calendarItem.startTime < maxEndTime)
            group.add(calendarItem)
        else
            buckets.add(mutableListOf(calendarItem))

        return buckets
    }

    /**
     * Calculates the layout attributes for the calendar items in a bucket.
     * The calculation is done minimizing the number of columns.
     *
     * @param bucket
     * @return An list of calendar attributes
     */
    private fun calculateLayoutAttributesFor(bucket: CalendarItemGroup): List<CalendarItem> {
        val left = bucket.filter {
            it is CalendarItem.CalendarEvent || it is CalendarItem.SelectedItem && it.selectedCalendarItem is SelectedCalendarItem.SelectedCalendarEvent
        }.toMutableList()
        val right = bucket.filter {
            it is CalendarItem.TimeEntry || it is CalendarItem.SelectedItem && it.selectedCalendarItem is SelectedCalendarItem.SelectedTimeEntry
        }.toMutableList()

        val leftColumns = calculateColumnsForItemsInSource(left)
        val rightColumns = calculateColumnsForItemsInSource(right)

        val groupColumns = leftColumns + rightColumns

        return groupColumns
            .mapIndexed { columnIndex, column ->
                column.map { attributesForItem(it, groupColumns.size, columnIndex) }
            }.flatten()
    }

    private fun attributesForItem(
        calendarItem: CalendarItem,
        totalColumns: Int,
        columnIndex: Int
    ): CalendarItem {
        return when (calendarItem) {
            is CalendarItem.TimeEntry -> calendarItem.copy(
                calendarItem.timeEntry,
                columnIndex = columnIndex,
                totalColumns = totalColumns
            )
            is CalendarItem.CalendarEvent -> calendarItem.copy(
                calendarItem.calendarEvent,
                columnIndex = columnIndex,
                totalColumns = totalColumns
            )
            is CalendarItem.SelectedItem -> calendarItem.copy(
                calendarItem.selectedCalendarItem,
                columnIndex = columnIndex,
                totalColumns = totalColumns
            )
        }
    }

    private fun calculateColumnsForItemsInSource(bucket: CalendarItemGroup): CalendarItemGroups =
        bucket.fold(mutableListOf(), ::convertIntoColumns)

    /**
     * Aggregates the items into columns, minimizing the number of columns.
     * This will try to insert an item into the first column without overlapping with other items there,
     * if that's not possible, will try with the rest of the columns until it's inserted or a new column is required.
     *
     * @param bucket
     * @param calendarItem
     */
    private fun convertIntoColumns(bucket: CalendarItemGroups, calendarItem: CalendarItem): CalendarItemGroups {
        if (bucket.isEmpty()) {
            bucket.add(mutableListOf(calendarItem))
            return bucket
        }

        val (column, position) = columnAndPositionToInsertItem(bucket, calendarItem)

        if (column != null) {
            column.add(position, calendarItem)
        } else {
            bucket.add(mutableListOf(calendarItem))
        }

        return bucket
    }

    /**
     * Returns the column and position in that column to insert the new item.
     * If the item cannot be inserted, the column is null.
     *
     * @param columns
     * @param item
     */
    private fun columnAndPositionToInsertItem(
        columns: CalendarItemGroups,
        item: CalendarItem
    ): Pair<CalendarItemGroup?, Int> {
        var positionToInsert = -1
        val now = timeService.now()
        val column = columns.firstOrNull {
            val lastItem = it.lastOrNull { el -> calculateEndTimeWith(el, now) <= item.startTime }
            val index = it.lastIndexOf(lastItem)
            when {
                index < 0 -> false
                index == it.size - 1 -> {
                    positionToInsert = it.size
                    true
                }
                it[index + 1].startTime >= calculateEndTimeWith(item, now) -> {
                    positionToInsert += 1
                    true
                }
                else -> false
            }
        }
        return column to positionToInsert
    }

    private fun calculateEndTimeWith(item: CalendarItem, now: OffsetDateTime): OffsetDateTime {
        val duration = item.duration(now, offsetFromNow)
        return if (duration <= minimumDurationForUIPurposes)
            item.startTime + minimumDurationForUIPurposes
        else item.endTime(now, offsetFromNow)
    }

    private fun CalendarItem.endTime(now: OffsetDateTime, offsetFromNow: Duration = Duration.ZERO): OffsetDateTime {
        return endTime ?: now + offsetFromNow
    }

    private fun CalendarItem.duration(now: OffsetDateTime, offsetFromNow: Duration = Duration.ZERO): Duration =
        when (this) {
            is CalendarItem.TimeEntry -> timeEntry.duration ?: (now + offsetFromNow).absoluteDurationBetween(startTime)
            is CalendarItem.CalendarEvent -> calendarEvent.duration
            is CalendarItem.SelectedItem -> when (selectedCalendarItem) {
                is SelectedCalendarItem.SelectedTimeEntry -> selectedCalendarItem.editableTimeEntry.duration
                    ?: (now + offsetFromNow).absoluteDurationBetween(startTime)
                is SelectedCalendarItem.SelectedCalendarEvent -> selectedCalendarItem.calendarEvent.duration
            }
        }

    private fun OffsetDateTime.absoluteDurationBetween(other: OffsetDateTime): Duration =
        Duration.ofMillis(ChronoUnit.MILLIS.between(this, other).absoluteValue)
}
