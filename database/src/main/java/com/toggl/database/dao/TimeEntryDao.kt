package com.toggl.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.toggl.database.models.DatabaseTimeEntry
import com.toggl.database.models.DatabaseTimeEntryTag
import com.toggl.database.models.DatabaseTimeEntryWithTags
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.OffsetDateTime

data class StartTimeEntryDatabaseResult(
    val startedTimeEntry: DatabaseTimeEntryWithTags,
    val stoppedTimeEntries: List<DatabaseTimeEntry>
)

@Dao
interface TimeEntryDao {
    // TimeEntries only

    @Transaction
    fun startTimeEntry(databaseTimeEntryWithTags: DatabaseTimeEntryWithTags): StartTimeEntryDatabaseResult {
        val (timeEntry, tags) = databaseTimeEntryWithTags
        val stoppedTimeEntries = stopRunningTimeEntries(timeEntry.startTime)
        val id = insertTimeEntry(timeEntry)
        insertAllTimeEntryTagsPairs(tags.map {
            DatabaseTimeEntryTag(id, it)
        })
        return StartTimeEntryDatabaseResult(
            getOneTimeEntryWithTags(id),
            stoppedTimeEntries
        )
    }

    @Transaction
    fun createTimeEntry(databaseTimeEntryWithTags: DatabaseTimeEntryWithTags): Long {
        val (timeEntry, tags) = databaseTimeEntryWithTags
        val id = insertTimeEntry(timeEntry)
        insertAllTimeEntryTagsPairs(tags.map {
            DatabaseTimeEntryTag(id, it)
        })
        return id
    }

    @Transaction
    fun stopRunningTimeEntries(now: OffsetDateTime): List<DatabaseTimeEntry> {
        return getAllRunningTimeEntries()
            .map { it.copy(duration = Duration.between(it.startTime, now)) }
            .also(this::updateAllTimeEntries)
    }

    @Query("SELECT * FROM time_entries WHERE NOT isDeleted AND duration is null")
    fun getAllRunningTimeEntries(): List<DatabaseTimeEntry>

    @Query("SELECT * FROM time_entries WHERE NOT isDeleted AND id = :id")
    fun getOneTimeEntry(id: Long): DatabaseTimeEntry

    @Transaction
    @Query("SELECT * FROM time_entries WHERE NOT isDeleted AND id = :id")
    fun getOneTimeEntryWithTags(id: Long): DatabaseTimeEntryWithTags

    @Insert
    fun insertAllTimeEntries(vararg databaseTimeEntries: DatabaseTimeEntry): List<Long>

    @Insert
    fun insertTimeEntry(databaseTimeEntries: DatabaseTimeEntry): Long

    @Update
    fun updateTimeEntry(databaseTimeEntry: DatabaseTimeEntry)

    @Update
    fun updateAllTimeEntries(databaseTimeEntries: List<DatabaseTimeEntry>)

    @Delete
    fun deleteTimeEntry(databaseTimeEntry: DatabaseTimeEntry)

    // TimeEntries & Tags

    @Query("SELECT * FROM time_entries WHERE NOT isDeleted")
    fun getAllTimeEntriesWithTags(): Flow<List<DatabaseTimeEntryWithTags>>

    @Query("SELECT count(*) FROM time_entries WHERE NOT isDeleted")
    fun count(): Int

    @Transaction
    @Update
    fun updateTimeEntryWithTags(databaseTimeEntryWithTags: DatabaseTimeEntryWithTags) {
        insertAllTimeEntryTagsPairs(databaseTimeEntryWithTags.tags.map {
            DatabaseTimeEntryTag(databaseTimeEntryWithTags.timeEntry.id, it)
        })

        updateTimeEntry(databaseTimeEntryWithTags.timeEntry)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllTimeEntryTagsPairs(databaseTimeEntryTags: List<DatabaseTimeEntryTag>)

    @Query("DELETE FROM time_entries")
    fun clear()
}
