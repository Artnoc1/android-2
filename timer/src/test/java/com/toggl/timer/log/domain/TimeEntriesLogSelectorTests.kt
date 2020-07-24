package com.toggl.timer.log.domain

import com.toggl.common.feature.timeentry.extensions.totalDuration
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.Client
import com.toggl.models.domain.Project
import com.toggl.models.domain.TimeEntry
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.createTimeEntry
import com.toggl.timer.generators.timeEntries
import io.kotlintest.DisplayName
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.matchers.collections.shouldBeEmpty
import io.kotlintest.matchers.collections.shouldContain
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.numerics.shouldBeExactly
import io.kotlintest.matchers.numerics.shouldBeLessThanOrEqual
import io.kotlintest.matchers.types.shouldNotBeTypeOf
import io.kotlintest.properties.Gen
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime
import java.time.Year
import java.time.format.DateTimeFormatter

@ExperimentalCoroutinesApi
@DisplayName("The TimeEntriesLogSelect")
class TimeEntriesLogSelectorTests : CoroutineTest() {
    private val todayString = "Today"
    private val yesterdayString = "Yesterday"
    private val today = OffsetDateTime.now()
    private val timeService = mockk<TimeService> { every { now() } returns today }
    private val localToday = today.toLocalDate()
    private val yesterday = localToday.minusDays(1)
    private val selector = TimeEntriesLogSelector(todayString, yesterdayString, timeService)

    /*
     *  Test Data
     */
    val projectsMap = mapOf(
        1L to Project(
            id = 1,
            name = "First Project",
            color = "",
            active = true,
            isPrivate = true,
            billable = true,
            workspaceId = 1,
            clientId = 1
        ),
        2L to Project(
            id = 2,
            name = "Second Project",
            color = "",
            active = true,
            isPrivate = true,
            billable = true,
            workspaceId = 1,
            clientId = 2
        )
    )

    val clientsMap = mapOf(
        1L to Client(
            id = 1,
            name = "Client numero 1",
            workspaceId = 1
        ),
        2L to Client(
            id = 2,
            name = "Cliento 2",
            workspaceId = 1
        )
    )

    val timeEntries = (0L..10L).flatMap { daysInThePast ->
        val date = today.minusDays(daysInThePast)
        (0L..10L).map { id ->
            createTimeEntry(
                id = daysInThePast + id,
                description = "Time entry $id",
                startTime = date,
                duration = Duration.ofMinutes(30)
            )
        }
    }

    val shortDuration = Duration.ofMinutes(30)

    fun List<TimeEntry>.mapToYesterday() = this.map { it.copy(id = it.id * 100, startTime = it.startTime.minusDays(1)) }

    val singleItem = listOf(
        createTimeEntry(
            id = 1,
            description = "S",
            startTime = today,
            duration = shortDuration
        )
    )

    val yesterdaySingleItem = singleItem.mapToYesterday()

    val groupA = (2L..5L).map { id ->
        createTimeEntry(
            id = id,
            description = "A",
            startTime = today,
            duration = shortDuration.plusMinutes(id)
        )
    }

    val yesterdayGroupA = groupA.mapToYesterday()

    val groupB = (6L..9L).map { id ->
        createTimeEntry(
            id = id,
            description = "B",
            startTime = today,
            duration = shortDuration.plusMinutes(id)
        )
    }

    val yesterdayGroupB = groupB.mapToYesterday()

    val twoProjects = listOf(
        createTimeEntry(
            id = 10,
            description = "B",
            startTime = today,
            duration = shortDuration.plusMinutes(10),
            projectId = 1
        ),
        createTimeEntry(
            id = 11,
            description = "B",
            startTime = today,
            duration = shortDuration.plusMinutes(11),
            projectId = 2
        )
    )

    val yesterdayTwoProjects = twoProjects.mapToYesterday()

    val differentDescriptions = listOf(
        createTimeEntry(
            id = 12,
            description = "C1",
            startTime = today,
            duration = shortDuration.plusMinutes(12)
        ),
        createTimeEntry(
            id = 13,
            description = "C1",
            startTime = today,
            duration = shortDuration.plusMinutes(13)
        ),
        createTimeEntry(
            id = 14,
            description = "C2",
            startTime = today,
            duration = shortDuration.plusMinutes(14)
        )
    )

    val yesterdayDifferentDescriptions = differentDescriptions.mapToYesterday()

    val longDuration = listOf(
        createTimeEntry(
            id = 15,
            description = "D1",
            startTime = today,
            duration = Duration.ofHours(1)
        ),
        createTimeEntry(
            id = 16,
            description = "D1",
            startTime = today,
            duration = Duration.ofHours(2)
        ),
        createTimeEntry(
            id = 17,
            description = "D3",
            startTime = today,
            duration = Duration.ofHours(3)
        )
    )

    val yesterdayLongDuration = longDuration.mapToYesterday()

    val todayEntries = (
        singleItem +
            groupA +
            groupB +
            twoProjects +
            differentDescriptions +
            longDuration
        )

    val yesterdayEntries = (
        yesterdaySingleItem +
            yesterdayGroupA +
            yesterdayGroupB +
            yesterdayTwoProjects +
            yesterdayDifferentDescriptions +
            yesterdayLongDuration
        )

    val similarTimeEntries: List<TimeEntry> = yesterdayEntries + todayEntries

    val timeEntriesMap = timeEntries.associateBy { it.id }
    val similarTimeEntriesMap = similarTimeEntries.associateBy { it.id }

    /*
     *  Expected Data
     */
    val expectedTodayGroupedTimeEntries: List<TimeEntryViewModel> = listOf(
        singleItem.first().toFlatTimeEntryViewModel(projectsMap, clientsMap),
        groupA.toTimeEntryGroupViewModel(groupA.first().similarityHashCode(), false, projectsMap, clientsMap),
        groupB.toTimeEntryGroupViewModel(groupB.first().similarityHashCode(), false, projectsMap, clientsMap),
        twoProjects.first().toFlatTimeEntryViewModel(projectsMap, clientsMap),
        twoProjects[1].toFlatTimeEntryViewModel(projectsMap, clientsMap),
        differentDescriptions.dropLast(1).toTimeEntryGroupViewModel(differentDescriptions.first().similarityHashCode(), false, projectsMap, clientsMap),
        differentDescriptions.last().toFlatTimeEntryViewModel(projectsMap, clientsMap),
        longDuration.dropLast(1).toTimeEntryGroupViewModel(longDuration.first().similarityHashCode(), false, projectsMap, clientsMap),
        longDuration.last().toFlatTimeEntryViewModel(projectsMap, clientsMap)
    )

    val expectedYesterdayGroupedTimeEntries: List<TimeEntryViewModel> = listOf(
        singleItem.mapToYesterday().first().toFlatTimeEntryViewModel(projectsMap, clientsMap),
        groupA.mapToYesterday().toTimeEntryGroupViewModel(groupA.mapToYesterday().first().similarityHashCode(), false, projectsMap, clientsMap),
        groupB.mapToYesterday().toTimeEntryGroupViewModel(groupB.mapToYesterday().first().similarityHashCode(), false, projectsMap, clientsMap),
        twoProjects.mapToYesterday().first().toFlatTimeEntryViewModel(projectsMap, clientsMap),
        twoProjects.mapToYesterday()[1].toFlatTimeEntryViewModel(projectsMap, clientsMap),
        differentDescriptions.mapToYesterday().dropLast(1).toTimeEntryGroupViewModel(differentDescriptions.mapToYesterday().first().similarityHashCode(), false, projectsMap, clientsMap),
        differentDescriptions.mapToYesterday().last().toFlatTimeEntryViewModel(projectsMap, clientsMap),
        longDuration.mapToYesterday().dropLast(1).toTimeEntryGroupViewModel(longDuration.mapToYesterday().first().similarityHashCode(), false, projectsMap, clientsMap),
        longDuration.mapToYesterday().last().toFlatTimeEntryViewModel(projectsMap, clientsMap)
    )

    val expectedGroupedTimeEntries: List<TimeEntryViewModel> = listOf(
        DayHeaderViewModel(
            dayTitle = todayString,
            totalDuration = todayEntries.totalDuration()
        )
    ) + expectedTodayGroupedTimeEntries + listOf(
        DayHeaderViewModel(
            dayTitle = yesterdayString,
            totalDuration = yesterdayEntries.totalDuration()
        )
    ) + expectedYesterdayGroupedTimeEntries

    @Nested
    @DisplayName("does not")
    inner class DoesNot {

        @Test
        fun `does not add running time entries to the groups`() = runBlockingTest {

            val timeEntriesButAllRunning = timeEntries.map { it.copy(duration = null) }
            val state = createInitialState(
                timeEntries = timeEntriesButAllRunning,
                projects = projectsMap.values.toList(),
                clients = clientsMap.values.toList()
            )

            val groupedTimeEntries = selector.select(state)
            groupedTimeEntries.size shouldBe 0
        }

        @Test
        fun `add entries marked to be deleted to the groups`() = runBlockingTest {
            val timeEntryMarkedForDeletion = createTimeEntry(
                0,
                "will be deleted",
                today,
                Duration.ofMinutes(30)
            )
            val timeEntriesPlusTheOneMarkedForDeletion = timeEntries + timeEntryMarkedForDeletion
            val state = createInitialState(
                timeEntries = timeEntriesPlusTheOneMarkedForDeletion,
                projects = projectsMap.values.toList(),
                clients = clientsMap.values.toList(),
                entriesPendingDeletion = setOf(0)
            )
            val groupedTimeEntries = selector.select(state)
            groupedTimeEntries.count { te -> te is FlatTimeEntryViewModel && te.description == "will be deleted" } shouldBe 0
            groupedTimeEntries.count { te -> te is TimeEntryGroupViewModel && te.description == "will be deleted" } shouldBe 0
        }

        @Test
        fun `create headers for days that have no time entries`() = runBlockingTest {

            val state = createInitialState(
                timeEntriesMap.values.toList(),
                projectsMap.values.toList(),
                clientsMap.values.toList()
            ).copy(shouldGroup = false)

            val timeEntryViewModels = selector.select(state)

            val headers =
                timeEntryViewModels.filterIsInstance<DayHeaderViewModel>()
                    .map { it.dayTitle }

            val flatTimeEntryHeaders =
                timeEntryViewModels
                    .asSequence()
                    .filterIsInstance<FlatTimeEntryViewModel>()
                    .map { it.startTime.toLocalDate() }
                    .distinct()
                    .map {
                        when (it) {
                            localToday -> todayString
                            yesterday -> yesterdayString
                            else -> it.format(
                                DateTimeFormatter.ofPattern("eee, dd MMM")
                            )
                        }
                    }.distinct().toList()

            headers shouldContainExactlyInAnyOrder flatTimeEntryHeaders
        }

        @Test
        fun `group similar time entries when grouping is disabled`() = runBlockingTest {
            val state = createInitialState(
                timeEntriesMap.values.toList(),
                projectsMap.values.toList(),
                clientsMap.values.toList()
            ).copy(shouldGroup = false)

            val notGroupedTimeEntries = selector.select(state)

            notGroupedTimeEntries.forEach {
                it.shouldNotBeTypeOf<TimeEntryGroupViewModel>()
            }
        }
    }

    @Nested
    @DisplayName("the header titles are formatted")
    inner class TheHeaderTitles {

        @Test
        fun `as the localized string for Today when the header is for the current day`() = runBlockingTest {
            val state = createInitialState(
                timeEntriesMap.values.toList(),
                projectsMap.values.toList(),
                clientsMap.values.toList()
            ).copy(shouldGroup = false)

            val groupedTimeEntries = selector.select(state)

            val headerTitles =
                groupedTimeEntries.filterIsInstance<DayHeaderViewModel>()
                    .map { it.dayTitle }

            headerTitles shouldContain todayString
        }

        @Test
        fun `as the localized string for Today when the header is for the previous day`() = runBlockingTest {
            val state = createInitialState(
                timeEntriesMap.values.toList(),
                projectsMap.values.toList(),
                clientsMap.values.toList()
            ).copy(shouldGroup = false)

            val timeEntryViewModels = selector.select(state)

            val headerTitles =
                timeEntryViewModels.filterIsInstance<DayHeaderViewModel>()
                    .map { it.dayTitle }

            headerTitles shouldContain yesterdayString
        }

        @Test
        fun `using eee, dd MMM for all other days`() = runBlockingTest {
            val formatter = DateTimeFormatter.ofPattern("eee, dd MMM")

            val state = createInitialState(
                timeEntriesMap.values.toList(),
                projectsMap.values.toList(),
                clientsMap.values.toList()
            ).copy(shouldGroup = false)

            val timeEntryViewModels = selector.select(state)

            val headerTitles =
                timeEntryViewModels.filterIsInstance<DayHeaderViewModel>()
                    .map { it.dayTitle }
                    .filter { it != todayString && it != yesterdayString }
                    .distinct()

            val formattedDates =
                timeEntryViewModels
                    .asSequence()
                    .filterIsInstance<FlatTimeEntryViewModel>()
                    .map { it.startTime.toLocalDate() }
                    .distinct()
                    .sortedDescending()
                    .drop(2)
                    .map { it.format(formatter) }
                    .toList()

            headerTitles shouldContainExactlyInAnyOrder formattedDates
        }
    }

    @Test
    fun `groups similar time entries`() = runBlockingTest {
            val state = createInitialState(
                similarTimeEntriesMap.values.toList(),
                projectsMap.values.toList(),
                clientsMap.values.toList()
            )
            val groupedTimeEntries = selector.select(state)
            groupedTimeEntries shouldBe expectedGroupedTimeEntries
        }

    @Nested
    @DisplayName("groups random similar time entries from the same year so that there is")
    inner class GroupsRandomSimilarEntries {

        private val y = Year.of(2020)
        private val randomSimilarInOneYear = Gen.timeEntries(
            description = "constant",
            year = y
        ).random().take(10000).toList()

        @Test
        fun `no more day headers than days in a year`() = runBlockingTest {
            val state = createInitialState(
                randomSimilarInOneYear,
                projectsMap.values.toList(),
                clientsMap.values.toList()
            )

            val groupedTimeEntries = selector.select(state)
            val dayHeaderViewModels = groupedTimeEntries.count { it is DayHeaderViewModel }
            dayHeaderViewModels shouldBeLessThanOrEqual y.length()
        }

        @Test
        fun `no more time entries or groups than days in a year`() = runBlockingTest {
            val state = createInitialState(
                randomSimilarInOneYear,
                projectsMap.values.toList(),
                clientsMap.values.toList()
            )

            val groupedTimeEntries = selector.select(state)
            val timeEntryViewModels = groupedTimeEntries.count { it is TimeEntryGroupViewModel || it is FlatTimeEntryViewModel }
            timeEntryViewModels shouldBeLessThanOrEqual y.length()
        }

        @Test
        fun `the same number of time entries or groups as day headers`() = runBlockingTest {
            val randomSimilarInOneYear = Gen.timeEntries(
                description = "constant",
                year = Year.of(2020)
            ).random().take(10000).toList()

            val state = createInitialState(
                randomSimilarInOneYear,
                projectsMap.values.toList(),
                clientsMap.values.toList()
            )

            val groupedTimeEntries = selector.select(state)
            val timeEntryViewModels = groupedTimeEntries.count { it is TimeEntryGroupViewModel || it is FlatTimeEntryViewModel }
            val dayHeaderViewModels = groupedTimeEntries.count { it is DayHeaderViewModel }
            timeEntryViewModels shouldBe dayHeaderViewModels
        }
    }

    @Nested
    @DisplayName("handles group expanding so that")
    inner class HandlesGroupExpanding {
        private val y = Year.of(2020)
        private val randomSimilarInOneYear = Gen.timeEntries(
            description = "constant",
            year = y
        ).random().take(10000).toList()

        private val randomExpandedGroups = randomSimilarInOneYear
            .shuffled()
            .take(1000)
            .map(TimeEntry::similarityHashCode)
            .toSet()

        @Test
        fun `groups that should be expanded are expanded`() = runBlockingTest {
            val state = createInitialState(
                randomSimilarInOneYear,
                projectsMap.values.toList(),
                clientsMap.values.toList(),
                randomExpandedGroups
            )
            val groupedTimeEntries = selector.select(state)

            val allGroups = groupedTimeEntries.filterIsInstance<TimeEntryGroupViewModel>()
            val expandedGroups = allGroups.filter { randomExpandedGroups.contains(it.groupId) }
            expandedGroups.forEach {
                it.isExpanded.shouldBeTrue()
            }
        }

        @Test
        fun `the correct number of individual FlatTimeEntryViewModel is present for every expanded group`() = runBlockingTest {
            val state = createInitialState(
                randomSimilarInOneYear,
                projectsMap.values.toList(),
                clientsMap.values.toList(),
                randomExpandedGroups
            )
            val groupedTimeEntries = selector.select(state)

            val allGroups = groupedTimeEntries.filterIsInstance<TimeEntryGroupViewModel>()
                val expandedGroups = allGroups.filter { randomExpandedGroups.contains(it.groupId) }
                expandedGroups.forEach { group ->
                val groupTimeEntries = groupedTimeEntries
                    .filterIsInstance<FlatTimeEntryViewModel>()
                    .filter { te -> group.timeEntryIds.contains(te.id) }
                groupTimeEntries.size shouldBeExactly group.timeEntryIds.size
            }
        }

        @Test
        fun `groups that shouldn't be expanded are not expanded`() = runBlockingTest {
            val state = createInitialState(
                randomSimilarInOneYear,
                projectsMap.values.toList(),
                clientsMap.values.toList(),
                randomExpandedGroups
            )

            val groupedTimeEntries = selector.select(state)

            val allGroups = groupedTimeEntries.filterIsInstance<TimeEntryGroupViewModel>()
            val nonExpandedGroups = allGroups.filter { !randomExpandedGroups.contains(it.groupId) }
            nonExpandedGroups.forEach {
                it.isExpanded.shouldBeFalse()
            }
        }

        @Test
        fun `there shouldn't be any individual FlatTimeEntryViewModel from unexpanded groups`() = runBlockingTest {
            val state = createInitialState(
                randomSimilarInOneYear,
                projectsMap.values.toList(),
                clientsMap.values.toList(),
                randomExpandedGroups
            )
            val groupedTimeEntries = selector.select(state)

            val allGroups = groupedTimeEntries.filterIsInstance<TimeEntryGroupViewModel>()
            val nonExpandedGroups = allGroups.filter { !randomExpandedGroups.contains(it.groupId) }
            nonExpandedGroups.forEach { group ->
                val groupTimeEntries = groupedTimeEntries
                    .filterIsInstance<FlatTimeEntryViewModel>()
                    .filter { te -> group.timeEntryIds.contains(te.id) }
                groupTimeEntries.shouldBeEmpty()
            }
        }
    }
}