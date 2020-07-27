package com.toggl.settings.domain

import android.content.Context
import com.toggl.models.domain.SettingsType
import com.toggl.models.domain.User
import com.toggl.models.validation.ApiToken
import com.toggl.models.validation.Email
import com.toggl.settings.common.CoroutineTest
import com.toggl.settings.common.createSettingsState
import com.toggl.settings.common.createUserPreferences
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The SettingsSelector")
class SettingsSelectorTests : CoroutineTest() {
    private val context = mockk<Context> { every { getString(any()) } returns "" }
    private val selector = SettingsSelector(context, SettingsStructureBlueprint.sections)

    @Test
    fun `Should translate blueprint into view models correctly`() = runBlockingTest {
        val prefs = createUserPreferences(
            manualModeEnabled = true,
            twentyFourHourClockEnabled = true,
            cellSwipeActionsEnabled = false,
            groupSimilarTimeEntriesEnabled = false,
            calendarIntegrationEnabled = true
        )
        val user = User(
            1,
            ApiToken.from("12345678901234567890123456789012") as ApiToken.Valid,
            Email.from("test@toggl.com") as Email.Valid,
            "Test Name",
            1
        )
        val initialState = createSettingsState(user = user, userPreferences = prefs)
        val sections: List<SettingsSectionViewModel> = selector.select(initialState)

        sections.size shouldBe SettingsStructureBlueprint.sections.size

        with(sections[0]) {
            verify<SettingsViewModel.ListChoice>(0, SettingsType.Name) {
                // TODO
            }
            verify<SettingsViewModel.ListChoice>(1, SettingsType.Email) {
                // TODO
            }
            verify<SettingsViewModel.ListChoice>(2, SettingsType.Workspace) {
                // TODO
            }
        }

        with(sections[1]) {
            verify<SettingsViewModel.ListChoice>(0, SettingsType.DateFormat) {
                // TODO
            }
            verify<SettingsViewModel.Toggle>(1, SettingsType.TwentyFourHourClock) {
                it.toggled shouldBe prefs.manualModeEnabled
            }
            verify<SettingsViewModel.ListChoice>(2, SettingsType.DurationFormat) {
                // TODO
            }
            verify<SettingsViewModel.ListChoice>(3, SettingsType.FirstDayOfTheWeek) {
                // TODO
            }
            verify<SettingsViewModel.Toggle>(4, SettingsType.GroupSimilar) {
                it.toggled shouldBe prefs.groupSimilarTimeEntriesEnabled
            }
        }

        with(sections[2]) {
            verify<SettingsViewModel.Toggle>(0, SettingsType.CellSwipe) {
                it.toggled shouldBe prefs.cellSwipeActionsEnabled
            }
            verify<SettingsViewModel.Toggle>(1, SettingsType.ManualMode) {
                it.toggled shouldBe prefs.manualModeEnabled
            }
        }

        with(sections[3]) {
            verify<SettingsViewModel.SubPage>(0, SettingsType.CalendarSettings)
            verify<SettingsViewModel.SubPage>(1, SettingsType.SmartAlert)
        }

        with(sections[4]) {
            verify<SettingsViewModel.SubPage>(0, SettingsType.SubmitFeedback)
            verify<SettingsViewModel.SubPage>(1, SettingsType.About)
            verify<SettingsViewModel.SubPage>(2, SettingsType.Help)
        }

        with(sections[5]) {
            verify<SettingsViewModel.ActionRow>(0, SettingsType.SignOut)
        }
    }

    private inline fun <reified VM : SettingsViewModel> SettingsSectionViewModel.verify(settingOption: Int, settingsType: SettingsType, block: (VM) -> Unit = { }) {
        settingsOptions[settingOption].shouldBeInstanceOf<VM> {
            it.settingsType shouldBe settingsType
            block(it)
        }
    }
}