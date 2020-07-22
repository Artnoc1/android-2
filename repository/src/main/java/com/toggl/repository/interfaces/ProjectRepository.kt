package com.toggl.repository.interfaces

import com.toggl.models.domain.Project
import com.toggl.repository.dto.CreateProjectDTO
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    suspend fun createProject(project: CreateProjectDTO): Project
    fun loadProjects(): Flow<List<Project>>
}