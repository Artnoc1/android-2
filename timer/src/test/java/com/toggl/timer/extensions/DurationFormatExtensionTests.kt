package com.toggl.timer.extensions

import com.toggl.common.feature.extensions.formatForDisplaying
import com.toggl.timer.generators.threeTenDuration
import io.kotest.matchers.numerics.shouldBeGreaterThanOrEqual
import io.kotest.properties.Gen
import io.kotest.properties.assertAll
import io.kotest.specs.FreeSpec
import java.time.Duration

class DurationFormatExtensionTests : FreeSpec({
    "The formatForDisplaying method" - {
        "Uses the hh:mm:ss format" - {
            assertAll(Gen.threeTenDuration(), fn = { duration: Duration ->
                val formatted = duration.formatForDisplaying()
                formatted.length shouldBeGreaterThanOrEqual 8
            })
        }
    }
})