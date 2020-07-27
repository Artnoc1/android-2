package com.toggl.repository

import android.content.SharedPreferences
import com.toggl.database.dao.ClientDao
import com.toggl.database.dao.ProjectDao
import com.toggl.database.dao.StartTimeEntryDatabaseResult
import com.toggl.database.dao.TagDao
import com.toggl.database.dao.TaskDao
import com.toggl.database.dao.TimeEntryDao
import com.toggl.database.dao.UserDao
import com.toggl.database.dao.WorkspaceDao
import com.toggl.database.models.DatabaseTimeEntry
import com.toggl.database.models.DatabaseTimeEntryWithTags
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.TimeEntry
import com.toggl.repository.dto.StartTimeEntryDTO
import com.toggl.repository.extensions.toDatabaseModel
import com.toggl.repository.extensions.toModelWithoutTags
import com.toggl.repository.interfaces.StartTimeEntryResult
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import java.time.Duration
import java.time.OffsetDateTime

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class RepositoryTest : StringSpec() {
    private val tagDao = mockk<TagDao>()
    private val projectDao = mockk<ProjectDao>()
    private val timeEntryDao = mockk<TimeEntryDao>()
    private val workspaceDao = mockk<WorkspaceDao>()
    private val clientDao = mockk<ClientDao>()
    private val taskDao = mockk<TaskDao>()
    private val userDao = mockk<UserDao>()
    private val sharedPreferences = mockk<SharedPreferences>()
    private val timeService = mockk<TimeService>()
    private var repository = Repository(projectDao, timeEntryDao, workspaceDao, clientDao, tagDao, taskDao, userDao, sharedPreferences, timeService)

    override fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        clearMocks(timeEntryDao, workspaceDao, clientDao, timeService, taskDao)
    }

    init {
        "loadTimeEntries calls getAllTimeEntriesWithTags on the DAO" {
            every { timeEntryDao.getAllTimeEntriesWithTags() } returns flowOf(emptyList())

            val loaded = repository.loadTimeEntries()

            verify(exactly = 1) { timeEntryDao.getAllTimeEntriesWithTags() }
            verify {
                workspaceDao wasNot called
                timeService wasNot called
                clientDao wasNot called
            }

            loaded.collect { timeEntries -> timeEntries.shouldBeEmpty() }
        }

        "loadClients calls getAll on the DAO" {
            every { clientDao.getAll() } returns listOf()

            val loaded = repository.loadClients()

            verify(exactly = 1) { clientDao.getAll() }
            verify {
                workspaceDao wasNot called
                timeService wasNot called
                timeEntryDao wasNot called
                projectDao wasNot called
            }
            loaded.shouldBeEmpty()
        }

        "loadTags calls getAll on the DAO" {
            every { tagDao.getAll() } returns listOf()

            val loaded = repository.loadTags()

            verify(exactly = 1) { tagDao.getAll() }
            verify {
                workspaceDao wasNot called
                timeService wasNot called
                timeEntryDao wasNot called
                projectDao wasNot called
            }
            loaded.shouldBeEmpty()
        }

        "loadTasks calls getAll on the DAO" {
            every { taskDao.getAll() } returns listOf()

            val loaded = repository.loadTasks()

            verify(exactly = 1) { taskDao.getAll() }
            verify {
                workspaceDao wasNot called
                timeService wasNot called
                timeEntryDao wasNot called
                projectDao wasNot called
            }
            loaded.shouldBeEmpty()
        }

        "loadWorkspaces calls getAll on the DAO" {
            every { workspaceDao.getAll() } returns listOf()

            repository.loadWorkspaces()

            verify(exactly = 1) { workspaceDao.getAll() }
            verify {
                timeEntryDao wasNot called
                timeService wasNot called
                clientDao wasNot called
            }
        }

        "stopRunningTimeEntry updates all running time entries and returns the first one" {
            val nowTime = OffsetDateTime.parse("2019-07-17T17:17:17+01:00")
            val timeEntryOneStartTime = OffsetDateTime.parse("2019-07-17T17:15:17+01:00")
            val timeEntryRunningOne =
                DatabaseTimeEntry(
                    1,
                    "one running",
                    timeEntryOneStartTime,
                    null,
                    false,
                    0,
                    0,
                    0,
                    false
                )
            val timeEntryTwoStartTime = OffsetDateTime.parse("2019-07-17T12:17:17+01:00")
            val timeEntryRunningTwo = timeEntryRunningOne.copy(
                id = 3,
                startTime = timeEntryTwoStartTime
            )
            val stoppedTimeEntries = listOf(timeEntryRunningOne, timeEntryRunningTwo).map {
                it.copy(duration = Duration.between(
                    timeEntryOneStartTime,
                    nowTime
                ))
            }
            val returnedTimeEntryWithTags = mockk<DatabaseTimeEntryWithTags>()
            every { timeService.now() } returns nowTime
            every { timeEntryDao.stopRunningTimeEntries(any()) } returns stoppedTimeEntries
            every { timeEntryDao.getOneTimeEntryWithTags(any()) } returns returnedTimeEntryWithTags
            every { returnedTimeEntryWithTags.timeEntry } returns timeEntryRunningOne
            every { returnedTimeEntryWithTags.tags } returns mockk()

            val result = repository.stopRunningTimeEntry()

            verify(exactly = 1) {
                timeEntryDao.stopRunningTimeEntries(nowTime)
                timeEntryDao.getOneTimeEntryWithTags(1)
            }
            verify {
                workspaceDao wasNot called
                clientDao wasNot called
            }
            result!!.id shouldBe stoppedTimeEntries.firstOrNull()!!.id
        }

        "stopRunningTimeEntry doesn't update any time entries if none are running and doesn't return any time entries" {
            val nowTime = OffsetDateTime.parse("2019-07-17T17:17:17+01:00")

            every { timeService.now() } returns nowTime
            every { timeEntryDao.stopRunningTimeEntries(any()) } returns listOf()

            val result = repository.stopRunningTimeEntry()

            verify(exactly = 1) { timeEntryDao.stopRunningTimeEntries(nowTime) }
            verify {
                workspaceDao wasNot called
                clientDao wasNot called
            }
            result shouldBe null
        }

        "startTimeEntry stops currently running time entry and inserts a new one to DAO" {
            val nowTime = OffsetDateTime.parse("2019-07-17T17:17:17+01:00")
            val timeEntryOneStartTime = OffsetDateTime.parse("2019-07-17T17:15:17+01:00")
            val timeEntryRunningOne = DatabaseTimeEntry(
                1,
                "Running",
                timeEntryOneStartTime,
                null,
                false,
                0,
                0,
                0,
                false
            )
            val timeEntryTwoStartTime = OffsetDateTime.parse("2019-07-17T12:17:17+01:00")
            val timeEntryRunningTwo = timeEntryRunningOne.copy(
                id = 3,
                startTime = timeEntryTwoStartTime
            )
            val stoppedTimeEntries = listOf(timeEntryRunningOne, timeEntryRunningTwo).map {
                it.copy(duration = Duration.between(
                    timeEntryOneStartTime,
                    nowTime
                ))
            }
            val startedTimeEntry = DatabaseTimeEntry(
                5,
                "started",
                timeEntryOneStartTime,
                null,
                false,
                1,
                0,
                0,
                false
            )
            every { timeService.now() } returns nowTime
            every { timeEntryDao.startTimeEntry(any()) } returns StartTimeEntryDatabaseResult(DatabaseTimeEntryWithTags(startedTimeEntry, emptyList()), stoppedTimeEntries)
            val startTimeEntryDTO = StartTimeEntryDTO(
                startedTimeEntry.description,
                startedTimeEntry.startTime,
                startedTimeEntry.billable,
                startedTimeEntry.workspaceId,
                startedTimeEntry.projectId,
                startedTimeEntry.taskId,
                listOf()
            )
            val result = repository.startTimeEntry(startTimeEntryDTO)

            verify(exactly = 1) {
                timeEntryDao.startTimeEntry(startTimeEntryDTO.toDatabaseModel())
            }
            verify {
                workspaceDao wasNot called
                clientDao wasNot called
            }
            result shouldBe StartTimeEntryResult(
                startedTimeEntry.toModelWithoutTags(),
                stoppedTimeEntries.firstOrNull()?.toModelWithoutTags()
            )
        }

        "editTimeEntry updates the time entry and returns it" {
            val nowTime = OffsetDateTime.parse("2019-07-17T17:17:17+01:00")
            val timeEntry = spyk<TimeEntry>(
                TimeEntry(1, "desc", nowTime, null, false, 1, null, null, false, emptyList())
            )
            every { timeEntryDao.updateTimeEntryWithTags(any()) } returns mockk()

            val result = repository.editTimeEntry(timeEntry)

            verify {
                workspaceDao wasNot called
                timeService wasNot called
                clientDao wasNot called
            }
            result shouldBe timeEntry
        }

        "deleteTimeEntry sets isDeleted, updates the DAO and returns the updated entries" {
            val timeEntry = TimeEntry(
                1337,
                "desc",
                OffsetDateTime.MAX,
                null,
                false,
                1,
                null,
                null,
                false,
                emptyList()
            )
            every { timeEntryDao.updateTimeEntryWithTags(any()) } returns mockk()

            val result =
                repository.deleteTimeEntry(timeEntry)

            verify(exactly = 1) {
                timeEntryDao.updateTimeEntryWithTags(timeEntry.copy(isDeleted = true).toDatabaseModel())
            }
            verify {
                workspaceDao wasNot called
                timeService wasNot called
                clientDao wasNot called
            }
            result shouldBe timeEntry.copy(isDeleted = true)
        }
    }
}
