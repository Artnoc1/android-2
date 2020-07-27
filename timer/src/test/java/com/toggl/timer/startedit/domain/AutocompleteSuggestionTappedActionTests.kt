package com.toggl.timer.startedit.domain

import com.toggl.common.feature.navigation.getRouteParam
import com.toggl.models.common.AutocompleteSuggestion.StartEditSuggestions
import com.toggl.models.domain.EditableProject
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.models.domain.Project
import com.toggl.models.domain.Tag
import com.toggl.models.domain.Task
import com.toggl.models.domain.TimeEntry
import com.toggl.timer.common.CoroutineTest
import com.toggl.timer.common.createTask
import com.toggl.timer.common.createTimeEntry
import com.toggl.timer.common.testReduceEffects
import com.toggl.timer.common.testReduceException
import com.toggl.timer.common.testReduceNoEffects
import com.toggl.timer.common.testReduceState
import com.toggl.timer.exceptions.ProjectDoesNotExistException
import com.toggl.timer.exceptions.TagDoesNotExistException
import com.toggl.timer.project.domain.createProject
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import io.kotest.matchers.types.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@DisplayName("The AutocompleteSuggestionTapped action")
internal class AutocompleteSuggestionTappedActionTests : CoroutineTest() {
    val initialState = createInitialState()
    val reducer = createReducer()

    @Nested
    @DisplayName("When a TimeEntry suggestion is tapped")
    inner class TimeEntrySuggestions : TheoryHolder {

        @ParameterizedTest
        @MethodSource("timeEntries")
        fun `The editableTimeEntry details should be updated`(timeEntrySuggestion: TimeEntry) = runBlockingTest {
            val initialEditableTimeEntry = EditableTimeEntry.empty(1)
            val suggestion = StartEditSuggestions.TimeEntry(timeEntrySuggestion)

            reducer.testReduceState(
                initialState,
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                it shouldBe initialState.copy(
                    autocompleteSuggestions = emptyList(),
                    editableTimeEntry = initialEditableTimeEntry.copy(
                        description = timeEntrySuggestion.description,
                        billable = timeEntrySuggestion.billable,
                        projectId = timeEntrySuggestion.projectId,
                        tagIds = timeEntrySuggestion.tagIds,
                        workspaceId = timeEntrySuggestion.workspaceId,
                        taskId = timeEntrySuggestion.taskId
                    )
                )
            }
        }

        @ParameterizedTest
        @MethodSource("timeEntries")
        fun `should return no effect`(timeEntrySuggestion: TimeEntry) = runBlockingTest {
            val suggestion = StartEditSuggestions.TimeEntry(timeEntrySuggestion)

            reducer.testReduceNoEffects(
                initialState,
                action = StartEditAction.AutocompleteSuggestionTapped(suggestion)
            )
        }
    }

    @Nested
    @DisplayName("When a Project suggestion is tapped")
    inner class ProjectSuggestions : TheoryHolder {

        @ParameterizedTest
        @MethodSource("projects")
        fun `should set the project information to the editable time entry`(project: Project) = runBlockingTest {
            val initialEditableTimeEntry = EditableTimeEntry.empty(1)
            val suggestion = StartEditSuggestions.Project(project)

            reducer.testReduceState(
                initialState,
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                it shouldBe initialState.copy(
                    autocompleteSuggestions = emptyList(),
                    editableTimeEntry = initialEditableTimeEntry.copy(
                        projectId = project.id,
                        workspaceId = project.workspaceId
                    )
                )
            }
        }

        @ParameterizedTest
        @MethodSource("projects")
        fun `should remove the substring that starts with an '@' from the description`(project: Project) = runBlockingTest {
            val initialEditableTimeEntry = EditableTimeEntry.empty(10).copy(
                description = "Such @${project.name}"
            )
            val suggestion = StartEditSuggestions.Project(project)
            val initialState = initialState.copy(
                editableTimeEntry = initialEditableTimeEntry,
                cursorPosition = initialEditableTimeEntry.description.length
            )

            reducer.testReduceState(
                initialState,
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                it shouldBe initialState.copy(
                    autocompleteSuggestions = emptyList(),
                    editableTimeEntry = initialEditableTimeEntry.copy(
                        description = "Such ",
                        projectId = project.id,
                        workspaceId = project.workspaceId
                    )
                )
            }
        }

        @ParameterizedTest
        @MethodSource("projects")
        fun `should remove the last substring that starts with a word beginning with an '@' from the description when multiple tokens are in place`(
            project: Project
        ) = runBlockingTest {
            val initialEditableTimeEntry = EditableTimeEntry.empty(10).copy(
                description = "Such # @ @ @ @${project.name}"
            )
            val suggestion = StartEditSuggestions.Project(project)
            val initialState = initialState.copy(
                editableTimeEntry = initialEditableTimeEntry,
                cursorPosition = initialEditableTimeEntry.description.length
            )

            reducer.testReduceState(
                initialState,
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                it shouldBe initialState.copy(
                    autocompleteSuggestions = emptyList(),
                    editableTimeEntry = initialEditableTimeEntry.copy(
                        description = "Such # @ @ @ ",
                        projectId = project.id,
                        workspaceId = project.workspaceId
                    )
                )
            }
        }

        @ParameterizedTest
        @MethodSource("projects")
        fun `should remove the last substring that starts with a word beginning with an '@' from the description when multiple tokens are in place up to the cursor position`(
            project: Project
        ) = runBlockingTest {
            val initialEditableTimeEntry = EditableTimeEntry.empty(10).copy(
                description = "Such # @ # @ @${project.name} Description"
            )
            val suggestion = StartEditSuggestions.Project(project)
            val initialState = initialState.copy(
                editableTimeEntry = initialEditableTimeEntry,
                cursorPosition = initialEditableTimeEntry.description.length - " Description".length
            )

            reducer.testReduceState(
                initialState,
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                it shouldBe initialState.copy(
                    autocompleteSuggestions = emptyList(),
                    editableTimeEntry = initialEditableTimeEntry.copy(
                        description = "Such # @ # @ ",
                        projectId = project.id,
                        workspaceId = project.workspaceId
                    )
                )
            }
        }

        @ParameterizedTest
        @MethodSource("projects")
        fun `should return no effect`(project: Project) = runBlockingTest {
            val suggestion = StartEditSuggestions.Project(project)

            reducer.testReduceNoEffects(
                initialState,
                action = StartEditAction.AutocompleteSuggestionTapped(suggestion)
            )
        }
    }

    @Nested
    @DisplayName("When a Tag suggestion is tapped")
    inner class TagSuggestions : TheoryHolder {

        @ParameterizedTest
        @MethodSource("tags")
        fun `The editableTimeEntry details should be updated`(tagTestData: TheoryHolder.TagTestData) = runBlockingTest {
            val tag = tagTestData.tag
            val suggestion = StartEditSuggestions.Tag(tag)

            reducer.testReduceState(
                initialState.copy(tags = tagTestData.tags),
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                it.editableTimeEntry.shouldNotBeNull()
                it.editableTimeEntry.tagIds shouldContain tag.id
            }
        }

        @ParameterizedTest
        @MethodSource("tags")
        fun `should return no effect`(tagTestData: TheoryHolder.TagTestData) = runBlockingTest {
            val tag = tagTestData.tag
            val suggestion = StartEditSuggestions.Tag(tag)

            reducer.testReduceNoEffects(
                initialState.copy(tags = tagTestData.tags),
                action = StartEditAction.AutocompleteSuggestionTapped(suggestion)
            )
        }

        @Test
        fun `should throw exception when adding non existing tag`() = runBlockingTest {
            val suggestion = StartEditSuggestions.Tag(Tag(1L, "Tag 1", 1))

            reducer.testReduceException(
                initialState.copy(tags = emptyMap()),
                action = StartEditAction.AutocompleteSuggestionTapped(suggestion),
                exception = TagDoesNotExistException::class.java
            )
        }

        @Test
        fun `The substring that starts with an '#' should be removed from the description`() = runBlockingTest {
            val initialEditableTimeEntry = EditableTimeEntry.empty(10).copy(
                description = "Such #tag"
            )
            val tag = Tag(1L, "Tag 1", 1)
            val suggestion = StartEditSuggestions.Tag(tag)
            val initialState = initialState.copy(
                editableTimeEntry = initialEditableTimeEntry,
                cursorPosition = initialEditableTimeEntry.description.length,
                tags = mapOf(1L to tag)
            )

            reducer.testReduceState(
                initialState,
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                it.editableTimeEntry.description shouldBe "Such "
            }
        }

        @Test
        fun `should remove the last substring that starts with an '#' from the description when multiple tokens are in place up to the cursor position`() =
            runBlockingTest {
                val initialEditableTimeEntry = EditableTimeEntry.empty(10).copy(
                    description = "Such #tag #tag #tag much"
                )
                val tag = Tag(1L, "Tag 1", 1)
                val suggestion = StartEditSuggestions.Tag(tag)
                val initialState = initialState.copy(
                    editableTimeEntry = initialEditableTimeEntry,
                    cursorPosition = initialEditableTimeEntry.description.length,
                    tags = mapOf(1L to tag)
                )

                reducer.testReduceState(
                    initialState,
                    StartEditAction.AutocompleteSuggestionTapped(suggestion)
                ) {
                    it.editableTimeEntry.description shouldBe "Such #tag #tag "
                }
            }
    }

    @Nested
    @DisplayName("When the Create Project suggestion is tapped")
    inner class CreateProjectSuggestions {

        private val projectName = "Project name"
        private val suggestion = StartEditSuggestions.CreateProject(projectName)

        @Test
        fun `The editableProject should be initialized`() = runBlockingTest {

            reducer.testReduceState(
                initialState,
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                val editableProject = it.backStack.getRouteParam<EditableProject>()
                editableProject.shouldNotBeNull()

                editableProject.name shouldBe projectName
                editableProject.workspaceId shouldBe 1
                Project.defaultColors.shouldContain(editableProject.color)
            }
        }

        @Test
        fun `should return no effect`() = runBlockingTest {
            reducer.testReduceNoEffects(
                initialState,
                action = StartEditAction.AutocompleteSuggestionTapped(suggestion)
            )
        }
    }

    @Nested
    @DisplayName("When a Task suggestion is tapped")
    inner class TaskSuggestions : TheoryHolder {

        @ParameterizedTest
        @MethodSource("tasks")
        fun `editableTimeEntry details should be updated`(taskTestData: TheoryHolder.TaskTestData) = runBlockingTest {
            val testTask = taskTestData.task
            val testProjects = taskTestData.projects

            val initialEditableTimeEntry = EditableTimeEntry.empty(1).copy(
                description = "@${testTask.name}"
            )
            val initialStateWithProjects = initialState.copy(projects = testProjects)
            val suggestion = StartEditSuggestions.Task(testTask)

            reducer.testReduceState(
                initialStateWithProjects,
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                // let's leave out the description for now, it will be testes separately
                val descriptionLessState = it.copy(editableTimeEntry = it.editableTimeEntry.copy(description = ""))
                descriptionLessState shouldBe initialStateWithProjects.copy(
                    autocompleteSuggestions = emptyList(),
                    editableTimeEntry = initialEditableTimeEntry.copy(
                        projectId = testTask.projectId,
                        description = "",
                        taskId = testTask.id,
                        workspaceId = testTask.workspaceId
                    )
                )
            }
        }

        @ParameterizedTest
        @MethodSource("tasks")
        fun `should return no effect`(taskTestData: TheoryHolder.TaskTestData) = runBlockingTest {
            val suggestion = StartEditSuggestions.Task(taskTestData.task)
            val initialStateWithProjects = initialState.copy(projects = taskTestData.projects)

            reducer.testReduceNoEffects(
                initialStateWithProjects,
                action = StartEditAction.AutocompleteSuggestionTapped(suggestion)
            )
        }

        @ParameterizedTest
        @MethodSource("tasksWithNonExistingProjects")
        fun `should throw exception when task points to a non existent project`(taskTestData: TheoryHolder.TaskWithNonExistingProjectTestData) = runBlockingTest {
            val suggestion = StartEditSuggestions.Task(taskTestData.task)
            val initialStateWithProjects = initialState.copy(projects = taskTestData.projects)

            reducer.testReduceException(
                initialStateWithProjects,
                action = StartEditAction.AutocompleteSuggestionTapped(suggestion),
                exception = ProjectDoesNotExistException::class.java
            )
        }

        @ParameterizedTest
        @MethodSource("tasks")
        fun `should remove the substring that starts with an '@' from the description`(taskTestData: TheoryHolder.TaskTestData) = runBlockingTest {
            val task = taskTestData.task
            val initialEditableTimeEntry = EditableTimeEntry.empty(10).copy(
                description = "Such @${task.name}"
            )
            val suggestion = StartEditSuggestions.Task(task)
            val initialState = initialState.copy(
                editableTimeEntry = initialEditableTimeEntry,
                cursorPosition = initialEditableTimeEntry.description.length,
                projects = taskTestData.projects
            )

            reducer.testReduceState(
                initialState,
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                it.editableTimeEntry.description shouldBe "Such "
            }
        }

        @ParameterizedTest
        @MethodSource("tasks")
        fun `should remove the last substring that starts with a word beginning with '@' from the description when multiple tokens are in place`(
            taskTestData: TheoryHolder.TaskTestData
        ) = runBlockingTest {
            val task = taskTestData.task
            val initialEditableTimeEntry = EditableTimeEntry.empty(10).copy(
                description = "Such # @ @ @ @${task.name}"
            )
            val suggestion = StartEditSuggestions.Task(task)
            val initialState = initialState.copy(
                editableTimeEntry = initialEditableTimeEntry,
                cursorPosition = initialEditableTimeEntry.description.length,
                projects = taskTestData.projects
            )

            reducer.testReduceState(
                initialState,
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                it.editableTimeEntry.description shouldBe "Such # @ @ @ "
            }
        }

        @ParameterizedTest
        @MethodSource("tasks")
        fun `should remove the last substring that starts with a word beginning with an '@' from the description when multiple tokens are in place up to the cursor position`(
            taskTestData: TheoryHolder.TaskTestData
        ) = runBlockingTest {
            val task = taskTestData.task
            val initialEditableTimeEntry = EditableTimeEntry.empty(10).copy(
                description = "Such # @ # @ @${task.name} Description"
            )
            val suggestion = StartEditSuggestions.Task(task)
            val initialState = initialState.copy(
                editableTimeEntry = initialEditableTimeEntry,
                cursorPosition = initialEditableTimeEntry.description.length - " Description".length,
                projects = taskTestData.projects
            )

            reducer.testReduceState(
                initialState,
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                it.editableTimeEntry.description shouldBe "Such # @ # @ "
            }
        }
    }

    @Nested
    @DisplayName("When the Create Tag suggestion is tapped")
    inner class CreateTagSuggestions {

        @Test
        fun `The TagCreated effect is emitted`() = runBlockingTest {
            val createTagSuggestion = StartEditSuggestions.CreateTag("01234")

            reducer.testReduceEffects(
                initialState,
                StartEditAction.AutocompleteSuggestionTapped(createTagSuggestion)
            ) {
                it.shouldBeSingleton()
                it.single()
                    .shouldBeTypeOf<CreateTagEffect>()
            }
        }

        @Test
        fun `The substring that starts with an '#' should be removed from the description`() = runBlockingTest {
            val initialEditableTimeEntry = EditableTimeEntry.empty(10).copy(
                description = "Such #tag"
            )
            val suggestion = StartEditSuggestions.CreateTag("tag")
            val initialState = initialState.copy(
                editableTimeEntry = initialEditableTimeEntry,
                cursorPosition = initialEditableTimeEntry.description.length
            )

            reducer.testReduceState(
                initialState,
                StartEditAction.AutocompleteSuggestionTapped(suggestion)
            ) {
                it.editableTimeEntry.description shouldBe "Such "
            }
        }

        @Test
        fun `should remove the last substring that starts with a word beginning with a '#' from the description when multiple tokens are in place up to the cursor position`() =
            runBlockingTest {
                val initialEditableTimeEntry = EditableTimeEntry.empty(10).copy(
                    description = "Such #tag #tag #tag much"
                )
                val suggestion = StartEditSuggestions.CreateTag("tag much")
                val initialState = initialState.copy(
                    editableTimeEntry = initialEditableTimeEntry,
                    cursorPosition = initialEditableTimeEntry.description.length
                )

                reducer.testReduceState(
                    initialState,
                    StartEditAction.AutocompleteSuggestionTapped(suggestion)
                ) {
                    it.editableTimeEntry.description shouldBe "Such #tag #tag "
                }
            }
    }

    interface TheoryHolder {
        companion object {

            @JvmStatic
            fun timeEntries(): Stream<TimeEntry> = Stream.of(
                createTimeEntry(10),
                createTimeEntry(20, "Expected Description"),
                createTimeEntry(30, billable = true),
                createTimeEntry(40, billable = false),
                createTimeEntry(50, projectId = 10),
                createTimeEntry(60, workspaceId = 10),
                createTimeEntry(80, taskId = 10),
                createTimeEntry(70, tags = listOf(10, 20)),
                createTimeEntry(
                    80,
                    "Expected Description",
                    billable = true,
                    projectId = 20,
                    workspaceId = 20,
                    tags = listOf(30),
                    taskId = 20
                )
            )

            @JvmStatic
            fun projects(): Stream<Project> = Stream.of(
                createProject(1),
                createProject(10, workspaceId = 10)
            )

            @JvmStatic
            fun tasks(): Stream<TaskTestData> {
                val project1 = Project(1, "a", "", true, false, true, 2, null)
                val project2 = Project(2, "b", "", true, false, true, 3, null)
                return Stream.of(
                    TaskTestData(
                        task = createTask(1, "Test", projectId = 1),
                        projects = mapOf(
                            1L to project1,
                            2L to project2
                        )
                    ),
                    TaskTestData(
                        task = createTask(2, "Test 2", projectId = 2),
                        projects = mapOf(
                            1L to project1,
                            2L to project2
                        )
                    ),
                    TaskTestData(
                        task = createTask(2, "Test 2", true, projectId = 2),
                        projects = mapOf(
                            1L to project1,
                            2L to project2
                        )
                    ),
                    TaskTestData(
                        task = createTask(2, "Test 2", true, projectId = 2),
                        projects = mapOf(
                            1L to project1,
                            2L to project2
                        )
                    )
                )
            }

            @JvmStatic
            fun tasksWithNonExistingProjects(): Stream<TaskWithNonExistingProjectTestData> = Stream.of(
                TaskWithNonExistingProjectTestData(
                    task = createTask(1, "Test", projectId = 1),
                    projects = mapOf()
                ),
                TaskWithNonExistingProjectTestData(
                    task = createTask(1, "Test", projectId = 3),
                    projects = mapOf(
                        1L to Project(1, "a", "", true, false, true, 2, null)
                    )
                )
            )

            @JvmStatic
            fun tags(): Stream<TagTestData> {
                val tag1 = Tag(1L, "Tag 1", 1)
                val tag2 = Tag(2L, "Tag 2", 1)
                val tag3 = Tag(3L, "Tag 3", 1)

                val tags = mapOf(
                    1L to tag1,
                    2L to tag2,
                    3L to tag3
                )

                return Stream.of(
                    TagTestData(tag1, tags),
                    TagTestData(tag2, tags),
                    TagTestData(tag3, tags)
                )
            }
        }

        data class TaskTestData(val task: Task, val projects: Map<Long, Project>)
        data class TaskWithNonExistingProjectTestData(val task: Task, val projects: Map<Long, Project>)
        data class TagTestData(val tag: Tag, val tags: Map<Long, Tag>)
    }
}