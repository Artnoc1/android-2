package com.toggl.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.toggl.api.ApiTokenProvider
import com.toggl.common.services.time.TimeService
import com.toggl.database.dao.ClientDao
import com.toggl.database.dao.ProjectDao
import com.toggl.database.dao.TagDao
import com.toggl.database.dao.TaskDao
import com.toggl.database.dao.TimeEntryDao
import com.toggl.database.dao.UserDao
import com.toggl.database.dao.WorkspaceDao
import com.toggl.database.models.DatabaseClient
import com.toggl.database.models.DatabaseProject
import com.toggl.database.models.DatabaseTag
import com.toggl.database.models.DatabaseTask
import com.toggl.database.models.DatabaseTimeEntryWithTags
import com.toggl.database.models.DatabaseUser
import com.toggl.database.models.DatabaseWorkspace
import com.toggl.models.domain.Client
import com.toggl.models.domain.DateFormat
import com.toggl.models.domain.DurationFormat
import com.toggl.models.domain.Project
import com.toggl.models.domain.SmartAlertsOption
import com.toggl.models.domain.Tag
import com.toggl.models.domain.Task
import com.toggl.models.domain.TimeEntry
import com.toggl.models.domain.User
import com.toggl.models.domain.UserPreferences
import com.toggl.models.domain.Workspace
import com.toggl.models.domain.WorkspaceFeature
import com.toggl.models.validation.ApiToken
import com.toggl.repository.dto.CreateProjectDTO
import com.toggl.repository.dto.CreateTimeEntryDTO
import com.toggl.repository.dto.StartTimeEntryDTO
import com.toggl.repository.extensions.toDatabaseModel
import com.toggl.repository.extensions.toModel
import com.toggl.repository.extensions.toModelWithoutTags
import com.toggl.repository.interfaces.AppRepository
import com.toggl.repository.interfaces.ClientRepository
import com.toggl.repository.interfaces.ProjectRepository
import com.toggl.repository.interfaces.SettingsRepository
import com.toggl.repository.interfaces.StartTimeEntryResult
import com.toggl.repository.interfaces.TagRepository
import com.toggl.repository.interfaces.TaskRepository
import com.toggl.repository.interfaces.TimeEntryRepository
import com.toggl.repository.interfaces.UserRepository
import com.toggl.repository.interfaces.WorkspaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

class Repository(
    private val projectDao: ProjectDao,
    private val timeEntryDao: TimeEntryDao,
    private val workspaceDao: WorkspaceDao,
    private val clientDao: ClientDao,
    private val tagDao: TagDao,
    private val taskDao: TaskDao,
    private val userDao: UserDao,
    private val sharedPreferences: SharedPreferences,
    private val timeService: TimeService
) : WorkspaceRepository,
    TimeEntryRepository,
    SettingsRepository,
    ProjectRepository,
    ClientRepository,
    TaskRepository,
    UserRepository,
    AppRepository,
    ApiTokenProvider,
    TagRepository {

    override fun loadProjects(): Flow<List<Project>> =
        projectDao.getAll().map { it.map(DatabaseProject::toModel) }

    override suspend fun createProject(project: CreateProjectDTO): Project {
        val databaseProject = DatabaseProject(
            name = project.name,
            color = project.color,
            active = project.active,
            isPrivate = project.isPrivate,
            billable = project.billable,
            workspaceId = project.workspaceId,
            clientId = project.clientId
        )

        return projectDao.insert(databaseProject)
            .run(projectDao::getOne)
            .run(DatabaseProject::toModel)
    }

    override fun loadTimeEntries(): Flow<List<TimeEntry>> =
        timeEntryDao.getAllTimeEntriesWithTags().map { it.map(DatabaseTimeEntryWithTags::toModel) }

    override suspend fun timeEntriesCount(): Int = timeEntryDao.count()

    override suspend fun createTag(tag: Tag): Tag {
        val databaseTag = DatabaseTag(
            name = tag.name,
            workspaceId = tag.workspaceId
        )

        return tagDao.insert(databaseTag)
            .run(tagDao::getOne)
            .run(DatabaseTag::toModel)
    }

    override suspend fun loadTags(): List<Tag> =
        tagDao.getAll().map(DatabaseTag::toModel)

    override suspend fun loadWorkspaces(): List<Workspace> =
        workspaceDao.getAll().map(DatabaseWorkspace::toModel)

    override suspend fun workspacesCount(): Int = workspaceDao.count()

    override suspend fun loadClients() = clientDao.getAll().map(DatabaseClient::toModel)

    override suspend fun createClient(client: Client): Client {
        val databaseClient = DatabaseClient(
            name = client.name,
            workspaceId = client.workspaceId
        )

        return clientDao.insert(databaseClient)
            .run(clientDao::getOne)
            .run(DatabaseClient::toModel)
    }

    override suspend fun loadTasks(): List<Task> = taskDao.getAll().map(DatabaseTask::toModel)

    override suspend fun startTimeEntry(startTimeEntryDTO: StartTimeEntryDTO): StartTimeEntryResult {
        return timeEntryDao.startTimeEntry(startTimeEntryDTO.toDatabaseModel()).let { (started, stopped) ->
            StartTimeEntryResult(
                started.toModel(),
                stopped.firstOrNull()?.toModelWithoutTags()
            )
        }
    }

    override suspend fun createTimeEntry(createTimeEntryDTO: CreateTimeEntryDTO): TimeEntry {
        val insertedId = timeEntryDao.createTimeEntry(createTimeEntryDTO.toDatabaseModel())
        return timeEntryDao.getOneTimeEntryWithTags(insertedId).toModel()
    }

    override suspend fun stopRunningTimeEntry(): TimeEntry? {
        val now = timeService.now()
        return timeEntryDao.stopRunningTimeEntries(now)
            .firstOrNull()
            ?.let { timeEntryDao.getOneTimeEntryWithTags(it.id).toModel() }
    }

    override suspend fun editTimeEntry(timeEntry: TimeEntry): TimeEntry {
        return timeEntryDao.updateTimeEntryWithTags(timeEntry.toDatabaseModel()).run { timeEntry }
    }

    override suspend fun deleteTimeEntry(timeEntry: TimeEntry): TimeEntry {
        val timeEntryWithTags = timeEntry.copy(isDeleted = true).toDatabaseModel()
        return timeEntryWithTags
            .apply(timeEntryDao::updateTimeEntryWithTags)
            .let(DatabaseTimeEntryWithTags::toModel)
    }

    override fun getApiToken(): ApiToken =
        sharedPreferences.getString(SettingsRepository.apiToken, "")
            ?.let(ApiToken.Companion::from) ?: ApiToken.Invalid

    override suspend fun loadUserPreferences(): UserPreferences =
        with(sharedPreferences) {
            val default = UserPreferences.default
            return UserPreferences(
                manualModeEnabled = getBoolean(SettingsRepository.manualModeEnabled, default.manualModeEnabled),
                twentyFourHourClockEnabled = getBoolean(SettingsRepository.twentyFourHourClockEnabled, default.twentyFourHourClockEnabled),
                groupSimilarTimeEntriesEnabled = getBoolean(SettingsRepository.groupSimilarTimeEntriesEnabled, default.groupSimilarTimeEntriesEnabled),
                cellSwipeActionsEnabled = getBoolean(SettingsRepository.cellSwipeActionsEnabled, default.cellSwipeActionsEnabled),
                calendarIntegrationEnabled = getBoolean(SettingsRepository.calendarIntegrationEnabled, false),
                calendarIds = getStringSet(SettingsRepository.calendarIds, emptySet())?.toList() ?: emptyList(),
                selectedWorkspaceId = getLong(SettingsRepository.selectedWorkspaceId, default.selectedWorkspaceId),
                dateFormat = DateFormat.valueOf(getString(SettingsRepository.dateFormat, default.dateFormat.name)!!),
                durationFormat = DurationFormat.valueOf(getString(SettingsRepository.durationFormat, default.durationFormat.name)!!),
                firstDayOfTheWeek = DayOfWeek.of(getInt(SettingsRepository.firstDayOfTheWeek, default.firstDayOfTheWeek.value)),
                smartAlertsOption = SmartAlertsOption.valueOf(getString(SettingsRepository.smartAlertsOption, default.smartAlertsOption.name)!!)
            )
        }

    override suspend fun saveUserPreferences(userPreferences: UserPreferences) {
        sharedPreferences.edit {
            putBoolean(SettingsRepository.manualModeEnabled, userPreferences.manualModeEnabled)
            putBoolean(SettingsRepository.twentyFourHourClockEnabled, userPreferences.twentyFourHourClockEnabled)
            putBoolean(SettingsRepository.groupSimilarTimeEntriesEnabled, userPreferences.groupSimilarTimeEntriesEnabled)
            putBoolean(SettingsRepository.cellSwipeActionsEnabled, userPreferences.cellSwipeActionsEnabled)
            putBoolean(SettingsRepository.calendarIntegrationEnabled, userPreferences.calendarIntegrationEnabled)
            putStringSet(SettingsRepository.calendarIds, userPreferences.calendarIds.toSet())
            putLong(SettingsRepository.selectedWorkspaceId, userPreferences.selectedWorkspaceId)
            putString(SettingsRepository.dateFormat, userPreferences.dateFormat.name)
            putString(SettingsRepository.durationFormat, userPreferences.durationFormat.name)
            putInt(SettingsRepository.firstDayOfTheWeek, userPreferences.firstDayOfTheWeek.value)
            putString(SettingsRepository.smartAlertsOption, userPreferences.smartAlertsOption.name)
        }
    }

    override suspend fun clearAllData() {
        saveUserPreferences(UserPreferences.default)
        setApiToken(ApiToken.Invalid)
        userDao.clear()
        projectDao.clear()
        timeEntryDao.clear()
        workspaceDao.clear()
        clientDao.clear()
        tagDao.clear()
        taskDao.clear()
        userDao.clear()
    }

    override suspend fun get(): User? =
        userDao.getAll().firstOrNull()?.let(DatabaseUser::toModel)

    override suspend fun set(user: User) {
        // Automatically create a default workspace
        workspaceDao.insert(DatabaseWorkspace(
            id = user.defaultWorkspaceId,
            name = "Auto created workspace",
            features = listOf(WorkspaceFeature.Pro)
        ))

        userDao.set(user.toDatabaseModel())
    }

    private fun setApiToken(apiToken: ApiToken) {
        sharedPreferences.edit {
            putString(SettingsRepository.apiToken, apiToken.toString())
        }
    }
}