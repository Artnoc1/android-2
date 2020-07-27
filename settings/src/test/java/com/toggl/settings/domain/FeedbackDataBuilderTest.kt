package com.toggl.settings.domain

import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.DateFormat
import com.toggl.models.domain.DurationFormat
import com.toggl.models.domain.SmartAlertsOption
import com.toggl.models.domain.UserPreferences
import com.toggl.repository.Repository
import com.toggl.repository.interfaces.SettingsRepository
import com.toggl.settings.common.CoroutineTest
import io.kotest.matchers.numerics.shouldBeExactly
import io.kotest.matchers.types.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.OffsetDateTime

@ExperimentalCoroutinesApi
@DisplayName("The FeedbackDataBuilder helper generated feedback data")
class FeedbackDataBuilderTest : CoroutineTest() {
    val repository = mockk<Repository>()
    val timeService = mockk<TimeService>()
    val settingsRepository = mockk<SettingsRepository>()
    val completelyModifiedUserPrefs = UserPreferences(
        manualModeEnabled = true,
        twentyFourHourClockEnabled = true,
        groupSimilarTimeEntriesEnabled = false,
        cellSwipeActionsEnabled = false,
        calendarIntegrationEnabled = true,
        calendarIds = listOf("1"),
        selectedWorkspaceId = 2,
        dateFormat = DateFormat.DDMMYYYY_dot,
        durationFormat = DurationFormat.Classic,
        firstDayOfTheWeek = DayOfWeek.SATURDAY,
        smartAlertsOption = SmartAlertsOption.MinutesBefore10
    )
    val feedbackDataBuilder: FeedbackDataBuilder = FeedbackDataBuilder(
        repository,
        timeService,
        settingsRepository
    )

    init {
        coEvery { settingsRepository.loadUserPreferences() }.returns(completelyModifiedUserPrefs)
        coEvery { repository.timeEntriesCount() }.returns(Int.MIN_VALUE)
        coEvery { repository.workspacesCount() }.returns(Int.MIN_VALUE)
        coEvery { timeService.now() }.returns(OffsetDateTime.MIN)
    }

    @Test
    fun `should contain the time entries count`() = runBlockingTest {
        val expectedCount = 1
        coEvery { repository.timeEntriesCount() }.returns(expectedCount)

        val feedbackData = feedbackDataBuilder.assembleFeedbackData()

        feedbackData.numberOfTimeEntries shouldBeExactly expectedCount
    }

    @Test
    fun `should contain the workspaces entries count`() = runBlockingTest {
        val expectedCount = 1
        coEvery { repository.workspacesCount() }.returns(expectedCount)

        val feedbackData = feedbackDataBuilder.assembleFeedbackData()

        feedbackData.numberOfWorkspaces shouldBeExactly expectedCount
    }

    @Test
    fun `should tell if the user has manual mode enabled`() = runBlockingTest {
        val expectedManualMode = completelyModifiedUserPrefs.manualModeEnabled

        val feedbackData = feedbackDataBuilder.assembleFeedbackData()

        feedbackData.manualModeIsOn shouldBe expectedManualMode
    }

    @Test
    fun `should have current device time`() = runBlockingTest {
        val expectedTime = mockk<OffsetDateTime>()
        every { timeService.now() }.returns(expectedTime)

        val feedbackData = feedbackDataBuilder.assembleFeedbackData()

        feedbackData.deviceTime shouldBe expectedTime
    }

    @Test
    fun `everything else should be null or have an bogus value until actually implemented`() = runBlockingTest {
        val feedbackData = feedbackDataBuilder.assembleFeedbackData()

        feedbackData.accountTimeZone.shouldBeNull()
        feedbackData.numberOfUnsyncedTimeEntries.shouldBe(Int.MIN_VALUE)
        feedbackData.numberOfUnsyncableTimeEntries.shouldBe(Int.MIN_VALUE)
        feedbackData.lastSyncAttempt.shouldBeNull()
        feedbackData.lastSuccessfulSync.shouldBeNull()
        feedbackData.lastLogin.shouldBeNull()
    }
}