package com.toggl.calendar.datepicker.domain

import com.toggl.calendar.common.CoroutineTest
import com.toggl.calendar.datepicker.ui.end
import com.toggl.calendar.datepicker.ui.start
import com.toggl.common.extensions.toList
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.UserPreferences
import com.toggl.repository.interfaces.SettingsRepository
import io.kotlintest.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.DayOfWeek
import java.time.Month
import java.time.OffsetDateTime
import java.time.ZoneOffset

@ExperimentalCoroutinesApi
@DisplayName("The DatePickerSelector")
class DatePickerSelectorTest : CoroutineTest() {
    @ParameterizedTest
    @MethodSource("availableDatesTestDataForAnYear")
    fun `Selects correctly the first and last selectable dates`(testData: AvailableDatesTestData) = runBlockingTest {
        val state = createInitialState()
        val settingsRepository = mockk<SettingsRepository>()
        val timeService = mockk<TimeService>()
        every { timeService.now() }.returns(testData.today)
        coEvery { settingsRepository.loadUserPreferences() }.returns(UserPreferences.default.copy(firstDayOfTheWeek = DayOfWeek.SUNDAY))
        val selector = DatePickerSelector(settingsRepository, timeService, dispatcherProvider)

        val result = selector.select(state)

        result.weeks.last().dates.last { it.isSelectable }.date shouldBe testData.today
        result.weeks.first().dates.first { it.isSelectable }.date shouldBe testData.firstAvailableDay
    }

    @ParameterizedTest
    @MethodSource("visibleDatesTestDataForTodayAsJuly_9_2020")
    fun `Loads correctly the visible dates for july, 9th 2020`(testData: VisibleDatesTestData) = runBlockingTest {
        val state = createInitialState()
        val timeService = mockk<TimeService>()
        val settingsRepository = mockk<SettingsRepository>()
        every { timeService.now() }.returns(testData.today)
        coEvery { settingsRepository.loadUserPreferences() }.returns(UserPreferences.default.copy(firstDayOfTheWeek = testData.beginningOfTheWeek))
        val selector = DatePickerSelector(settingsRepository, timeService, dispatcherProvider)

        val result = selector.select(state)

        result.weeks.last().end.toLocalDate() shouldBe testData.lastVisibleDate.toLocalDate()
        result.weeks.first().start.toLocalDate() shouldBe testData.firstVisibleDate.toLocalDate()
    }

    @ParameterizedTest
    @MethodSource("weekLabelsTestData")
    fun `Loads correctly the weekday labels`(weekLabelsTestData: WeekLabelsTestData) = runBlockingTest {
        val state = createInitialState()
        val timeService = mockk<TimeService>()
        val settingsRepository = mockk<SettingsRepository>()
        every { timeService.now() }.returns(OffsetDateTime.now())
        coEvery { settingsRepository.loadUserPreferences() }.returns(UserPreferences.default.copy(firstDayOfTheWeek = weekLabelsTestData.firstDayOfTheWeek))
        val selector = DatePickerSelector(settingsRepository, timeService, dispatcherProvider)

        val result = selector.select(state)

        result.weekHeaderLabels
    }

    @ParameterizedTest
    @MethodSource("visibleDatesTestDataForTodayAsJuly_9_2020")
    fun `Loads correctly the currently selected week index`(testData: VisibleDatesTestData) = runBlockingTest {
        val state = createInitialState()
        val timeService = mockk<TimeService>()
        val settingsRepository = mockk<SettingsRepository>()
        every { timeService.now() }.returns(testData.today)
        coEvery { settingsRepository.loadUserPreferences() }.returns(UserPreferences.default.copy(firstDayOfTheWeek = testData.beginningOfTheWeek))
        val selector = DatePickerSelector(settingsRepository, timeService, dispatcherProvider)
        val weeks = selector.select(state).weeks

        weeks.forEachIndexed { expectedIndex, week ->
            week.dates.forEach { date ->
                selector.select(state.copy(date.date)).selectedWeek shouldBe expectedIndex
            }
        }
    }

    companion object {
        @JvmStatic
        fun availableDatesTestDataForAnYear(): List<AvailableDatesTestData> {
            val lastYear = OffsetDateTime.now().plusYears(-1)
            return (lastYear..OffsetDateTime.now())
                .toList()
                .map { AvailableDatesTestData(it, it.plusDays(-13)) }
        }

        @JvmStatic
        fun visibleDatesTestDataForTodayAsJuly_9_2020(): List<VisibleDatesTestData> {
            val today = OffsetDateTime.of(2020, Month.JULY.value, 9, 0, 0, 0, 0, ZoneOffset.UTC)
            val julyOf2020 = OffsetDateTime.of(2020, Month.JULY.value, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            val juneOf2020 = OffsetDateTime.of(2020, Month.JUNE.value, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            return listOf(
                VisibleDatesTestData(today = today, beginningOfTheWeek = DayOfWeek.SUNDAY, firstVisibleDate = juneOf2020.withDayOfMonth(21), lastVisibleDate = julyOf2020.withDayOfMonth(11)),
                VisibleDatesTestData(today = today, beginningOfTheWeek = DayOfWeek.MONDAY, firstVisibleDate = juneOf2020.withDayOfMonth(22), lastVisibleDate = julyOf2020.withDayOfMonth(12)),
                VisibleDatesTestData(today = today, beginningOfTheWeek = DayOfWeek.TUESDAY, firstVisibleDate = juneOf2020.withDayOfMonth(23), lastVisibleDate = julyOf2020.withDayOfMonth(13)),
                VisibleDatesTestData(today = today, beginningOfTheWeek = DayOfWeek.WEDNESDAY, firstVisibleDate = juneOf2020.withDayOfMonth(24), lastVisibleDate = julyOf2020.withDayOfMonth(14)),
                VisibleDatesTestData(today = today, beginningOfTheWeek = DayOfWeek.THURSDAY, firstVisibleDate = juneOf2020.withDayOfMonth(25), lastVisibleDate = julyOf2020.withDayOfMonth(15)),
                VisibleDatesTestData(today = today, beginningOfTheWeek = DayOfWeek.FRIDAY, firstVisibleDate = juneOf2020.withDayOfMonth(26), lastVisibleDate = julyOf2020.withDayOfMonth(9)),
                VisibleDatesTestData(today = today, beginningOfTheWeek = DayOfWeek.SATURDAY, firstVisibleDate = juneOf2020.withDayOfMonth(20), lastVisibleDate = julyOf2020.withDayOfMonth(10))
            )
        }

        @JvmStatic
        fun weekLabelsTestData(): List<WeekLabelsTestData> {
            return listOf(
                WeekLabelsTestData(DayOfWeek.MONDAY, listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
                WeekLabelsTestData(DayOfWeek.TUESDAY, listOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY)),
                WeekLabelsTestData(DayOfWeek.WEDNESDAY, listOf(DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY)),
                WeekLabelsTestData(DayOfWeek.THURSDAY, listOf(DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)),
                WeekLabelsTestData(DayOfWeek.FRIDAY, listOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY)),
                WeekLabelsTestData(DayOfWeek.SATURDAY, listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)),
                WeekLabelsTestData(DayOfWeek.SUNDAY, listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY))
            )
        }
    }

    data class AvailableDatesTestData(val today: OffsetDateTime, val firstAvailableDay: OffsetDateTime)
    data class VisibleDatesTestData(
        val today: OffsetDateTime,
        val beginningOfTheWeek: DayOfWeek,
        val firstVisibleDate: OffsetDateTime,
        val lastVisibleDate: OffsetDateTime
    )

    data class WeekLabelsTestData(val firstDayOfTheWeek: DayOfWeek, val expectedSequence: List<DayOfWeek>)
}