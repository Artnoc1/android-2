package com.toggl.timer.common.domain

import com.toggl.models.domain.EditableTimeEntry
import com.toggl.timer.common.createTimeEntry
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime

@DisplayName("EditableTimeEntry")
internal class EditableTimeEntryTests {

    @Test
    fun `for groups, should sum durations of TEs in the represented group and have no start time`() {
        val timeEntries = listOf(
            createTimeEntry(1, duration = Duration.ofMinutes(3)),
            createTimeEntry(2, duration = Duration.ofMinutes(5)),
            createTimeEntry(3)
        )

        val ids = listOf<Long>(1, 2)
        val editable = EditableTimeEntry.fromGroup(timeEntries.filter { it.id in ids })

        editable.duration shouldBe Duration.ofMinutes(8)
        editable.startTime shouldBe null
    }

    @Test
    fun `for single TEs, should copy over the duration and start time`() {
        val now = OffsetDateTime.now()
        val timeEntry = createTimeEntry(1, duration = Duration.ofMinutes(3), startTime = now)

        val editable = EditableTimeEntry.fromSingle(timeEntry)

        editable.duration shouldBe Duration.ofMinutes(3)
        editable.startTime shouldBe now
    }

    @Test
    fun `for non-started TEs, initiates an empty editable`() {
        val editable = EditableTimeEntry.empty(1)

        editable.duration shouldBe null
        editable.startTime shouldBe null
        editable.ids shouldBe emptyList()
        editable.description shouldBe ""
    }
}