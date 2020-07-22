package com.toggl.database

import com.toggl.database.models.DatabaseWorkspace
import com.toggl.database.util.prepareClient
import com.toggl.database.util.prepareProject
import com.toggl.database.util.prepareTag
import com.toggl.database.util.prepareTask
import com.toggl.database.util.prepareTimeEntry
import com.toggl.models.domain.WorkspaceFeature
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ExampleDatabaseTest : BaseDatabaseTest() {

    @Test
    fun `insert does insert`() {
        val databaseWorkspace = DatabaseWorkspace(name = "One workspace", features = listOf(WorkspaceFeature.Pro))

        database.workspaceDao().insert(databaseWorkspace)

        assert(database.workspaceDao().getAll().size == 1)
    }

    @Test
    fun `tests are independent`() {
        val databaseWorkspace = DatabaseWorkspace(name = "Another workspace", features = listOf(WorkspaceFeature.Pro))

        database.workspaceDao().insert(databaseWorkspace)

        assert(database.workspaceDao().getAll().size == 1)
    }

    @Test
    fun `a time entry can be created and queried`() {
        val dbTimeEntry = prepareTimeEntry("Such Time Entry")

        val timeEntryId = database.timeEntryDao().insertTimeEntry(dbTimeEntry)
        val queriedTimeEntry = database.timeEntryDao().getOneTimeEntry(timeEntryId)

        assertThat(queriedTimeEntry.description).isEqualTo("Such Time Entry")
    }

    @Test
    fun `a project can be created and queried`() {
        val dbProject = prepareProject("Such Project")

        val projectId = database.projectDao().insert(dbProject)
        val queriedProject = database.projectDao().getOne(projectId)

        assertThat(queriedProject.name).isEqualTo("Such Project")
    }

    @Test
    fun `a client can be created and queried`() {
        val dbClient = prepareClient("Such Client")

        val clientId = database.clientDao().insert(dbClient)
        val queriedClient = database.clientDao().getOne(clientId)

        assertThat(queriedClient.name).isEqualTo("Such Client")
    }

    @Test
    fun `a tag can be created and queried`() {
        val dbTag = prepareTag("Such Tag")

        val tagId = database.tagDao().insert(dbTag)
        val queriedTag = database.tagDao().getOne(tagId)

        assertThat(queriedTag.name).isEqualTo("Such Tag")
    }

    @Test
    fun `a task can be created and queried`() {
        val dbTask = prepareTask("Such Task")

        val taskId = database.taskDao().insert(dbTask)
        val queriedTask = database.taskDao().getOne(taskId)

        assertThat(queriedTask.name).isEqualTo("Such Task")
    }

    // Our roboeletric test don't work with coroutines properly yet
    // @Test
    // fun `a time entry can have tags added to it and be queried`() {
    //     val dbTags = (1..3).map { prepareTag("Tag $it") }
    //     val tagIds = database.tagDao().insertAll(*dbTags.toTypedArray())
    //     val dbTimeEntry = prepareTimeEntry()
    //     val timeEntryId = database.timeEntryDao().insertTimeEntry(dbTimeEntry)
    //
    //     database.timeEntryDao().insertAllTimeEntryTagsPairs(
    //         tagIds.map { DatabaseTimeEntryTag(timeEntryId, it) }
    //     )
    //     val timeEntryWithTags = database.timeEntryDao().getAllTimeEntriesWithTags()
    //     assertThat(timeEntryWithTags.first().tags).containsAll(tagIds)
    // }
}