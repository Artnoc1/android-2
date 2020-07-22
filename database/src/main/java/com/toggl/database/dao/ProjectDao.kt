package com.toggl.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.toggl.database.models.DatabaseProject
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects")
    fun getAll(): Flow<List<DatabaseProject>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getOne(id: Long): DatabaseProject

    @Insert
    fun insertAll(vararg databaseProjects: DatabaseProject): List<Long>

    @Insert
    fun insert(databaseProject: DatabaseProject): Long

    @Update
    fun update(databaseProject: DatabaseProject)

    @Update
    fun updateAll(databaseProjects: List<DatabaseProject>)

    @Delete
    fun delete(databaseProject: DatabaseProject)

    @Query("DELETE FROM projects")
    fun clear()
}