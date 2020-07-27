package com.toggl.timer.generators

import com.toggl.models.domain.TimeEntry
import com.toggl.timer.common.createTimeEntry
import io.kotest.properties.Gen
import java.time.Duration
import java.time.Month
import java.time.OffsetDateTime
import java.time.Year

fun Gen.Companion.timeEntries(
    description: String? = null,
    duration: Duration? = null,
    year: Year? = null,
    month: Month? = null,
    day: Int? = null,
    hour: Int? = null,
    minute: Int? = null,
    second: Int? = null
): Gen<TimeEntry> = object : Gen<TimeEntry> {

    val idGen = positiveLong().random().iterator()
    val stringGen = string().random().iterator()
    val dateTimeGen = threeTenOffsetDateTime(year, month, day, hour, minute, second).random().iterator()
    val durationGen = threeTenDuration().random().iterator()

    override fun constants(): Iterable<TimeEntry> = (1L..100L)
        .map { index ->
            createTimeEntry(
                id = index,
                description = "Test $index",
                startTime = OffsetDateTime.now().minusDays(index),
                duration = Duration.ofHours(index)
            )
        }
        .asIterable()

    override fun random(): Sequence<TimeEntry> = generateSequence {
        createTimeEntry(
            id = idGen.next(),
            description = description ?: stringGen.next(),
            startTime = dateTimeGen.next(),
            duration = duration ?: durationGen.next()
        )
    }
}