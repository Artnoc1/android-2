package com.toggl.timer.startedit.ui.editduration.model

import com.toggl.common.Constants.ClockMath.secondsInAMinute
import io.kotest.matchers.beGreaterThanOrEqualTo
import io.kotest.matchers.beLessThanOrEqualTo
import io.kotest.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration
import java.util.stream.Stream
import kotlin.random.Random

@DisplayName("The DurationFieldInfo")
class DurationFieldInfoTest {

    abstract class BaseDurationFieldInfoTest {
        var field = DurationFieldInfo()

        fun inputData(digitsString: String) {
            val digits = digitsString.toCharArray().map { it - '0' }.toIntArray()
            inputDigits(digits)
        }

        fun inputDigits(digits: IntArray) {
            digits.forEach {
                field = field.push(it)
            }
        }

        fun popNTimes(count: Int) {
            for (i in 0 until count) {
                field = field.pop()
            }
        }

        companion object {

            @JvmStatic
            fun randomDurations(): Stream<Duration> {
                val seed = 1234
                val random = Random(seed)
                val streamBuilder = Stream.builder<Long>()
                streamBuilder.add(-1L)
                streamBuilder.add(Duration.ofHours(1000).seconds)
                for (i in 0..100) {
                    val seconds = random.nextLong()
                    streamBuilder.add(seconds)
                }
                return streamBuilder.build().map { Duration.ofSeconds(it) }
            }

            @JvmStatic
            fun convertsTheDurationCorrectlyTheory(): List<Arguments> =
                listOf(
                    arguments(0L, 0L),
                    arguments(0L, 1L),
                    arguments(1L, 0L),
                    arguments(12L, 30L),
                    arguments(0L, 30L),
                    arguments(123L, 1)
                )

            @JvmStatic
            fun interpretsTheDigitsInCorrectOrderTheory(): List<Arguments> =
                listOf(
                    arguments("", 0, 0),
                    arguments("1", 0, 1),
                    arguments("12", 0, 12),
                    arguments("123", 1, 23),
                    arguments("1234", 12, 34),
                    arguments("12345", 123, 45),
                    arguments("99999", 999, 99),
                    arguments("87", 0, 87)
                )

            @JvmStatic
            fun ignoresMoreThanFiveInputDigitsTheory(): List<Arguments> =
                listOf(
                    arguments("123456", 123, 45),
                    arguments("1234567", 123, 45),
                    arguments("12345678", 123, 45),
                    arguments("123456789", 123, 45)
                )

            @JvmStatic
            fun removesTheDigitsWhichWereAddedTheLastTheory() =
                listOf(
                    arguments("19", 1, 0, 1),
                    arguments("127", 1, 0, 12),
                    arguments("650", 1, 0, 65),
                    arguments("1234", 2, 0, 12),
                    arguments("12345", 2, 1, 23),
                    arguments("99999", 3, 0, 99),
                    arguments("87", 2, 0, 0)
                )

            @JvmStatic
            fun stopsPoppingWhenEmptyTheory() = listOf(
                arguments(""),
                arguments("12345"),
                arguments("661")
            )

            @JvmStatic
            fun ignoresLeadingZerosTheory() = listOf(
                arguments("", "00:00"),
                arguments("0", "00:00"),
                arguments("00", "00:00"),
                arguments("000", "00:00"),
                arguments("0000", "00:00"),
                arguments("00000", "00:00"),
                arguments("000000", "00:00")
            )

            @JvmStatic
            fun doesNotCarryMinutesToHoursWhenTheNumberOfMinutesIsMoreThanFiftyNineTheory() = listOf(
                arguments("60", "00:60"),
                arguments("181", "01:81"),
                arguments("99999", "999:99")
            )

            @JvmStatic
            fun showsTwoLeadingZerosWhenThereAreZeroHoursTheory() = listOf(
                arguments("1", "00:01"),
                arguments("2", "00:02"),
                arguments("12", "00:12"),
                arguments("89", "00:89"),
                arguments("45", "00:45"),
                arguments("90", "00:90")
            )

            @JvmStatic
            fun showsALeadingZerosWhenThereIsASingleDigitHourTheory() = listOf(
                arguments("100", "01:00"),
                arguments("200", "02:00"),
                arguments("120", "01:20"),
                arguments("950", "09:50")
            )

            @JvmStatic
            fun doesNotShowALeadingZeroWhenTheDurationIsFourDigitsLongTheory() = listOf(
                arguments("1200", "12:00"),
                arguments("4201", "42:01"),
                arguments("1234", "12:34"),
                arguments("9999", "99:99")
            )

            @JvmStatic
            fun showsFiveCharactersWhenTheNumberOfHoursIsMoreThanNinetyNineTheory() = listOf(
                arguments("10000", "100:00"),
                arguments("12345", "123:45"),
                arguments("99999", "999:99")
            )

            @JvmStatic
            fun correctlyCalculatesTheDurationTheory() = listOf(
                arguments("12", 12),
                arguments("123", 1 * 60 + 23),
                arguments("1177", 11 * 60 + 77),
                arguments("789", 7 * 60 + 89)
            )
        }
    }

    @Nested
    inner class `The fromDuration method` : BaseDurationFieldInfoTest() {

        @ParameterizedTest
        @MethodSource("randomDurations")
        fun `clamps the value between zero and the max value`(input: Duration) {
            val output = DurationFieldInfo.fromDuration(input).toDuration()

            output should beGreaterThanOrEqualTo(Duration.ZERO)
            output should beLessThanOrEqualTo(Duration.ofHours(999))
        }

        @ParameterizedTest
        @MethodSource("convertsTheDurationCorrectlyTheory")
        fun `converts the time span correctly`(hours: Long, minutes: Long) {
            val duration = Duration.ofHours(hours).plusMinutes(minutes)

            val durationField = DurationFieldInfo.fromDuration(duration)

            durationField.hours() shouldBe hours
            durationField.minutes() shouldBe minutes
        }
    }

    @Nested
    inner class `The push method` : BaseDurationFieldInfoTest() {

        @ParameterizedTest
        @MethodSource("interpretsTheDigitsInCorrectOrderTheory")
        fun `interprets the digits in correct order`(inputSequence: String, expectedHours: Int, expectedMinutes: Int) {
            inputData(inputSequence)

            field.hours() shouldBe expectedHours
            field.minutes() shouldBe expectedMinutes
        }

        @ParameterizedTest
        @MethodSource("ignoresMoreThanFiveInputDigitsTheory")
        fun `ignores more than five input digits`(inputSequence: String, expectedHours: Int, expectedMinutes: Int) {
            inputData(inputSequence)

            field.hours() shouldBe expectedHours
            field.minutes() shouldBe expectedMinutes
        }
    }

    @Nested
    inner class `The pop method` : BaseDurationFieldInfoTest() {

        @ParameterizedTest
        @MethodSource("removesTheDigitsWhichWereAddedTheLastTheory")
        fun `removes the digits which were added the last`(
            inputSequence: String,
            popCount: Int,
            expectedHours: Int,
            expectedMinutes: Int
        ) {
            inputData(inputSequence)

            popNTimes(popCount)

            field.hours() shouldBe expectedHours
            field.minutes() shouldBe expectedMinutes
        }

        @ParameterizedTest
        @MethodSource("stopsPoppingWhenEmptyTheory")
        fun `stops popping when empty`(inputSequence: String) {
            inputData(inputSequence)

            popNTimes(inputSequence.length + 3)

            field.hours() shouldBe 0
            field.minutes() shouldBe 0
        }
    }

    @Nested
    inner class `The toString method` : BaseDurationFieldInfoTest() {

        @ParameterizedTest
        @MethodSource("ignoresLeadingZerosTheory")
        fun `ignores leading zeros`(inputSequence: String, expectedOutput: String) {
            inputData(inputSequence)

            val output = field.toString()

            output shouldBe expectedOutput
        }

        @ParameterizedTest
        @MethodSource("doesNotCarryMinutesToHoursWhenTheNumberOfMinutesIsMoreThanFiftyNineTheory")
        fun `does not carry minutes to hours when the number of minutes is more than fifty nine`(
            inputSequence: String,
            expectedOutput: String
        ) {
            inputData(inputSequence)

            val output = field.toString()

            output shouldBe expectedOutput
        }

        @ParameterizedTest
        @MethodSource("showsTwoLeadingZerosWhenThereAreZeroHoursTheory")
        fun `shows two leading zeros when there are zero hours`(inputSequence: String, expectedOutput: String) {
            inputData(inputSequence)

            val output = field.toString()

            output shouldBe expectedOutput
        }

        @ParameterizedTest
        @MethodSource("showsALeadingZerosWhenThereIsASingleDigitHourTheory")
        fun `shows a leading zeros when there is a single digit hour`(inputSequence: String, expectedOutput: String) {
            inputData(inputSequence)

            val output = field.toString()

            output shouldBe expectedOutput
        }

        @ParameterizedTest
        @MethodSource("doesNotShowALeadingZeroWhenTheDurationIsFourDigitsLongTheory")
        fun `does not show a leading zero when the duration is four digits long`(inputSequence: String, expectedOutput: String) {
            inputData(inputSequence)

            val output = field.toString()

            output shouldBe expectedOutput
        }

        @ParameterizedTest
        @MethodSource("showsFiveCharactersWhenTheNumberOfHoursIsMoreThanNinetyNineTheory")
        fun `shows five characters when the number of hours is more than ninety nine`(inputSequence: String, expectedOutput: String) {
            inputData(inputSequence)

            val output = field.toString()

            output shouldBe expectedOutput
        }
    }

    @Nested
    inner class `The toDuration method` : BaseDurationFieldInfoTest() {

        @ParameterizedTest
        @ValueSource(strings = ["99861", "99901", "99999"])
        fun `clamps duration to maximum time`(inputSequence: String) {
            inputData(inputSequence)

            val output = field.toDuration()

            output shouldBe Duration.ofHours(999)
        }

        @ParameterizedTest
        @MethodSource("correctlyCalculatesTheDurationTheory")
        fun `correctly calculates the duration`(inputSequence: String, totalMinutes: Long) {
            inputData(inputSequence)

            val output = field.toDuration()

            output.seconds / secondsInAMinute shouldBe totalMinutes
        }
    }

    @Nested
    inner class `The default constructor` : BaseDurationFieldInfoTest() {

        @Test
        fun `minutes and hours are zero`() {
            val field = DurationFieldInfo()

            field.hours() shouldBe 0
            field.minutes() shouldBe 0
        }

        @Test
        fun `serializes into zeros only`() {
            DurationFieldInfo().toString() shouldBe "00:00"
        }

        @Test
        fun `converts into zero duration`() {
            DurationFieldInfo().toDuration() shouldBe Duration.ZERO
        }
    }
}