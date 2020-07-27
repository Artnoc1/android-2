package com.toggl.timer.generators

import io.kotest.properties.Gen
import java.time.Month
import java.time.OffsetDateTime
import java.time.Year
import java.time.ZoneOffset
import kotlin.random.Random

fun Gen.Companion.threeTenOffsetDateTime(
    year: Year? = null,
    month: Month? = null,
    day: Int? = null,
    hour: Int? = null,
    minute: Int? = null,
    second: Int? = null
): Gen<OffsetDateTime> = object : Gen<OffsetDateTime> {

    override fun constants(): Iterable<OffsetDateTime> = listOf(
        OffsetDateTime.now(),
        OffsetDateTime.MAX,
        OffsetDateTime.MIN
    )

    override fun random(): Sequence<OffsetDateTime> = generateSequence {
        val y = year?.value ?: Random.nextInt(Year.MIN_VALUE, Year.MAX_VALUE)
        val m = month?.value ?: Random.nextInt(Month.JANUARY.value, Month.DECEMBER.value)
        val d = day ?: Random.nextInt(1, 28)
        val h = hour ?: Random.nextInt(1, 24)
        val min = minute ?: Random.nextInt(0, 60)
        val s = second ?: Random.nextInt(0, 60)
        OffsetDateTime.of(y, m, d, h, min, s, 1, ZoneOffset.UTC)
    }
}