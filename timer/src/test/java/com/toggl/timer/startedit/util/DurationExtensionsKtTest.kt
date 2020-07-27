package com.toggl.timer.startedit.util

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.util.stream.Stream

@DisplayName("The Duration.asDurationString extension method")
internal class DurationExtensionsKtTest {

    companion object {
        @JvmStatic
        fun `converts durations to strings in a friendly way`() = Stream.of(
            Arguments.arguments(Duration.ofSeconds(0), "00:00:00"),
            Arguments.arguments(Duration.ofSeconds(1), "00:00:01"),
            Arguments.arguments(Duration.ofSeconds(10), "00:00:10"),
            Arguments.arguments(Duration.ofMinutes(1), "00:01:00"),
            Arguments.arguments(Duration.ofMinutes(10), "00:10:00"),
            Arguments.arguments(Duration.ofHours(1), "01:00:00"),
            Arguments.arguments(Duration.ofHours(10), "10:00:00"),
            Arguments.arguments(Duration.ofHours(99), "99:00:00"),
            Arguments.arguments(Duration.ofHours(100).plusMinutes(59).plusSeconds(59), "100:59:59")
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `converts durations to strings in a friendly way`(duration: Duration, expectedDurationString: String) {
        duration.asDurationString() shouldBe expectedDurationString
    }
}