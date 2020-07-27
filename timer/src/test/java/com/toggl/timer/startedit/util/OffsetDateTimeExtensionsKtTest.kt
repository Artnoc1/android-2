package com.toggl.timer.startedit.util

import com.toggl.common.extensions.absoluteDurationBetween
import com.toggl.common.extensions.roundToClosestMinute
import com.toggl.common.extensions.timeOfDay
import io.kotest.matchers.beGreaterThanOrEqualTo
import io.kotest.matchers.beLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

internal class OffsetDateTimeExtensionsKtTest {

    data class DurationTestEntry(val start: OffsetDateTime, val end: OffsetDateTime, val expectedDuration: Duration)

    @ParameterizedTest
    @MethodSource("absoluteDurationBetweenTheory")
    fun absoluteDurationBetween(durationTestEntry: DurationTestEntry) {
        val result = durationTestEntry.start.absoluteDurationBetween(durationTestEntry.end)

        result shouldBe durationTestEntry.expectedDuration
    }

    @ParameterizedTest
    @MethodSource("timeOfDayTheory")
    fun timeOfDay(timeOfDayDurationPair: Pair<OffsetDateTime, Duration>) {
        val timeOfDay = timeOfDayDurationPair.first.timeOfDay()

        timeOfDay shouldNotBe beGreaterThanOrEqualTo(Duration.ofHours(24))
        timeOfDay shouldNotBe beLessThan(Duration.ofHours(0))
        timeOfDay shouldBe timeOfDayDurationPair.second
    }

    @Test
    fun roundToClosestMinuteRoundsToTheClosestMinute() {
        val tenOClock = OffsetDateTime.of(2020, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        val onePastTen = tenOClock.plusMinutes(1)

        tenOClock.roundToClosestMinute() shouldBe tenOClock
        (tenOClock + Duration.ofSeconds(1)).roundToClosestMinute() shouldBe tenOClock
        (tenOClock + Duration.ofSeconds(29)).roundToClosestMinute() shouldBe tenOClock
        (tenOClock + Duration.ofSeconds(30)).roundToClosestMinute() shouldBe tenOClock
        (tenOClock + Duration.ofSeconds(31)).roundToClosestMinute() shouldBe onePastTen
        (tenOClock + Duration.ofSeconds(59)).roundToClosestMinute() shouldBe onePastTen
        onePastTen.roundToClosestMinute() shouldBe onePastTen
    }

    companion object {

        @JvmStatic
        fun absoluteDurationBetweenTheory(): List<DurationTestEntry> {
            val now = OffsetDateTime.now()
            return listOf(
                DurationTestEntry(now, now.plusMinutes(30), Duration.ofMinutes(30)),
                DurationTestEntry(now.plusMinutes(30), now, Duration.ofMinutes(30)),

                DurationTestEntry(now.plusSeconds(59), now, Duration.ofSeconds(59)),
                DurationTestEntry(now, now.plusSeconds(59), Duration.ofSeconds(59)),

                DurationTestEntry(
                    now.plusHours(123).plusMinutes(4).plusSeconds(5),
                    now,
                    Duration.ofHours(123).plusMinutes(4).plusSeconds(5)
                ),
                DurationTestEntry(
                    now,
                    now.plusHours(123).plusMinutes(4).plusSeconds(5),
                    Duration.ofHours(123).plusMinutes(4).plusSeconds(5)
                )
            )
        }

        @JvmStatic
        fun timeOfDayTheory(): Stream<Pair<OffsetDateTime, Duration>> {
            val now = OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS)
            val inTwoDays = now.plusDays(2)
            var increment = Duration.ofMinutes(10).plusSeconds(1)

            val stream = Stream.builder<Pair<OffsetDateTime, Duration>>()
            stream.add(now to Duration.ZERO)
            while ((now + increment).isBefore(inTwoDays)) {
                stream.add(now + increment to increment.truncateDay())
                increment += Duration.ofMinutes(10)
            }
            stream.add(
                inTwoDays - Duration.ofSeconds(1) to Duration.ofHours(23) + Duration.ofMinutes(59) + Duration.ofSeconds(
                    59
                )
            )
            stream.add(inTwoDays to Duration.ZERO)
            return stream.build()
        }

        private fun Duration.truncateDay(): Duration =
            if (this < Duration.ofHours(24)) this
            else this - Duration.ofHours(24)
    }
}