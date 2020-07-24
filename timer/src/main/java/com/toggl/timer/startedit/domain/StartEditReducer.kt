package com.toggl.timer.startedit.domain

import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.core.Effect
import com.toggl.architecture.core.MutableValue
import com.toggl.architecture.core.Reducer
import com.toggl.architecture.extensions.effect
import com.toggl.architecture.extensions.effectOf
import com.toggl.architecture.extensions.noEffect
import com.toggl.architecture.extensions.toEffects
import com.toggl.common.Constants.AutoCompleteSuggestions.projectToken
import com.toggl.common.Constants.AutoCompleteSuggestions.tagToken
import com.toggl.common.Constants.TimeEntry.maxDurationInHours
import com.toggl.common.extensions.absoluteDurationBetween
import com.toggl.common.feature.extensions.mutateWithoutEffects
import com.toggl.common.feature.extensions.returnEffect
import com.toggl.common.feature.navigation.Route
import com.toggl.common.feature.navigation.popBackStackWithoutEffects
import com.toggl.common.feature.navigation.push
import com.toggl.common.feature.timeentry.TimeEntryAction.CreateTimeEntry
import com.toggl.common.feature.timeentry.TimeEntryAction.EditTimeEntry
import com.toggl.common.feature.timeentry.TimeEntryAction.StartTimeEntry
import com.toggl.common.feature.timeentry.extensions.isNew
import com.toggl.common.feature.timeentry.extensions.isRepresentingGroup
import com.toggl.common.feature.timeentry.extensions.isRunning
import com.toggl.common.feature.timeentry.extensions.isRunningOrNew
import com.toggl.common.feature.timeentry.extensions.isStopped
import com.toggl.common.feature.timeentry.extensions.wasNotYetPersisted
import com.toggl.common.services.time.TimeService
import com.toggl.models.common.AutocompleteSuggestion.StartEditSuggestions
import com.toggl.models.domain.EditableProject
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.repository.Repository
import com.toggl.repository.exceptions.StartTimeShouldNotBeNullException
import com.toggl.repository.extensions.toCreateDto
import com.toggl.repository.extensions.toStartDto
import com.toggl.timer.exceptions.EditableTimeEntryDoesNotHaveADurationSetException
import com.toggl.timer.exceptions.EditableTimeEntryDoesNotHaveAStartTimeSetException
import com.toggl.timer.exceptions.ProjectDoesNotExistException
import com.toggl.timer.exceptions.TagDoesNotExistException
import com.toggl.timer.startedit.domain.TemporalInconsistency.DurationTooLong
import com.toggl.timer.startedit.domain.TemporalInconsistency.StartTimeAfterCurrentTime
import com.toggl.timer.startedit.domain.TemporalInconsistency.StartTimeAfterStopTime
import com.toggl.timer.startedit.domain.TemporalInconsistency.StopTimeBeforeStartTime
import com.toggl.timer.startedit.util.findTokenAndQueryMatchesForAutocomplete
import java.time.Duration
import java.time.OffsetDateTime
import javax.inject.Inject

class StartEditReducer @Inject constructor(
    private val repository: Repository,
    private val timeService: TimeService,
    private val dispatcherProvider: DispatcherProvider
) : Reducer<StartEditState, StartEditAction> {

    override fun reduce(
        state: MutableValue<StartEditState>,
        action: StartEditAction
    ): List<Effect<StartEditAction>> {
        return when (action) {
            StartEditAction.CloseButtonTapped,
            StartEditAction.DialogDismissed -> effectOf(StartEditAction.Close)
            is StartEditAction.DescriptionEntered ->
                state.mutate {
                    copy(
                        cursorPosition = action.cursorPosition,
                        editableTimeEntry = editableTimeEntry.copy(
                            description = action.description
                        )
                    )
                } returnEffect updateAutocompleteSuggestions(action, state())

            StartEditAction.BillableTapped ->
                state.mutateWithoutEffects {
                    copy(
                        editableTimeEntry = editableTimeEntry.copy(
                            billable = !editableTimeEntry.billable
                        )
                    )
                }
            StartEditAction.AddProjectChipTapped,
            StartEditAction.ProjectButtonTapped ->
                state.mapState {
                    val stringBeforeToken =
                        if (editableTimeEntry.description.isEmpty()) "" else "${editableTimeEntry.description} "
                    val description = "$stringBeforeToken$projectToken"
                    effectOf(StartEditAction.DescriptionEntered(description, description.length))
                }
            StartEditAction.AddTagChipTapped,
            StartEditAction.TagButtonTapped ->
                state.mapState {
                    val stringBeforeToken =
                        if (editableTimeEntry.description.isEmpty()) "" else "${editableTimeEntry.description} "
                    val description = "$stringBeforeToken$tagToken"
                    effectOf(StartEditAction.DescriptionEntered(description, description.length))
                }
            is StartEditAction.PickerTapped ->
                state.mutateWithoutEffects {
                    copy(dateTimePickMode = action.pickerMode, temporalInconsistency = TemporalInconsistency.None)
                }
            StartEditAction.DoneButtonTapped -> {
                val editableTimeEntry = state().editableTimeEntry

                if (editableTimeEntry.wasNotYetPersisted()) {
                    if (editableTimeEntry.isRunning()) {
                        val dto = editableTimeEntry.toStartDto(timeService.now())
                        effectOf(StartEditAction.TimeEntryHandling(StartTimeEntry(dto)))
                    } else {
                        val dto = editableTimeEntry.toCreateDto()
                        effectOf(StartEditAction.TimeEntryHandling(CreateTimeEntry(dto)))
                    }
                } else {
                    val timeEntriesToEdit = editableTimeEntry.ids.mapNotNull { state().timeEntries[it] }
                    timeEntriesToEdit.map {

                        val isGroup = editableTimeEntry.isRepresentingGroup()
                        val startTime = if (isGroup) it.startTime else editableTimeEntry.startTime
                            ?: throw StartTimeShouldNotBeNullException()
                        val duration = if (isGroup) it.duration else editableTimeEntry.duration

                        StartEditAction.TimeEntryHandling(
                            EditTimeEntry(
                                it.copy(
                                    description = editableTimeEntry.description,
                                    billable = editableTimeEntry.billable,
                                    workspaceId = editableTimeEntry.workspaceId,
                                    projectId = editableTimeEntry.projectId,
                                    taskId = editableTimeEntry.taskId,
                                    tagIds = editableTimeEntry.tagIds,
                                    startTime = startTime,
                                    duration = duration
                                )
                            )
                        )
                    }.toEffects()
                } + effectOf(StartEditAction.Close)
            }
            is StartEditAction.AutocompleteSuggestionsUpdated ->
                state.mutateWithoutEffects { copy(autocompleteSuggestions = action.autocompleteSuggestions) }
            is StartEditAction.AutocompleteSuggestionTapped -> {
                state.mutate { copy(autocompleteSuggestions = emptyList()) }

                when (action.autocompleteSuggestion) {
                    is StartEditSuggestions.TimeEntry -> state.mutateWithoutEffects {
                        modifyWithTimeEntrySuggestion(action.autocompleteSuggestion)
                    }
                    is StartEditSuggestions.Project -> state.mutateWithoutEffects {
                        modifyWithProjectSuggestion(action.autocompleteSuggestion)
                    }
                    is StartEditSuggestions.Task -> state.mutateWithoutEffects {
                        modifyWithTaskSuggestion(action.autocompleteSuggestion)
                    }
                    is StartEditSuggestions.Tag -> state.mutateWithoutEffects {
                        modifyWithTagSuggestion(action.autocompleteSuggestion)
                    }
                    is StartEditSuggestions.CreateProject -> state.mutateWithoutEffects {
                        modifyWithCreateProjectSuggestion(action.autocompleteSuggestion)
                    }
                    is StartEditSuggestions.CreateTag -> {
                        val tagCreationSuggestion = action.autocompleteSuggestion
                        val workspaceId = state.mapState { editableTimeEntry.workspaceId }
                        state.mutate {
                            modifyWithCreateTagSuggestion()
                        } returnEffect effect(
                            CreateTagEffect(
                                dispatcherProvider,
                                repository,
                                tagCreationSuggestion.name,
                                workspaceId
                            )
                        )
                    }
                }
            }
            StartEditAction.DateTimePickingCancelled -> {
                state.mutateWithoutEffects { copy(dateTimePickMode = DateTimePickMode.None) }
            }
            is StartEditAction.DateTimePicked -> {
                val mode = state().dateTimePickMode
                if (mode == DateTimePickMode.None) {
                    noEffect()
                } else {
                    val editableTimeEntry = state().editableTimeEntry
                    val temporalInconsistency = detectTemporalInconsistencies(editableTimeEntry, mode, action.dateTime)
                    if (temporalInconsistency == TemporalInconsistency.None) {
                        state.mutateWithoutEffects {
                            when (mode) {
                                DateTimePickMode.StartTime, DateTimePickMode.StartDate ->
                                    handleStartTimeEdition(editableTimeEntry, action)
                                DateTimePickMode.EndTime, DateTimePickMode.EndDate ->
                                    handleEndTimeEdition(editableTimeEntry, action)
                                else -> this
                            }
                        }
                    } else {
                        state.mutate {
                            copy(dateTimePickMode = DateTimePickMode.None, temporalInconsistency = temporalInconsistency)
                        }
                        effect(ReopenPickerEffect(mode))
                    }
                }
            }
            is StartEditAction.DurationInputted -> {
                val maxDuration = Duration.ofHours(maxDurationInHours)
                if (action.duration < Duration.ZERO || action.duration > maxDuration)
                    return noEffect()

                state.mutateWithoutEffects {
                    val editableTimeEntry = state().editableTimeEntry

                    if (editableTimeEntry.isRunningOrNew())
                        copy(editableTimeEntry = editableTimeEntry.copy(startTime = timeService.now() - action.duration))
                    else
                        copy(editableTimeEntry = editableTimeEntry.copy(duration = action.duration))
                }
            }
            is StartEditAction.WheelChangedStartTime -> {
                val editableTimeEntry = state().editableTimeEntry
                val maxDuration = Duration.ofHours(maxDurationInHours)
                val now = timeService.now()
                val endTime = editableTimeEntry.endTime(now)
                val newDuration = action.startTime.absoluteDurationBetween(endTime)
                if (action.startTime > endTime || newDuration > maxDuration)
                    return noEffect()

                state.mutateWithoutEffects {
                    if (editableTimeEntry.isRunningOrNew())
                        copy(editableTimeEntry = editableTimeEntry.copy(startTime = action.startTime))
                    else
                        copy(editableTimeEntry = editableTimeEntry.copy(startTime = action.startTime, duration = newDuration))
                }
            }
            is StartEditAction.WheelChangedEndTime -> {
                val maxDuration = Duration.ofHours(maxDurationInHours)
                if (action.duration < Duration.ZERO || action.duration > maxDuration)
                    return noEffect()

                state.mutateWithoutEffects {
                    val editableTimeEntry = state().editableTimeEntry
                    if (editableTimeEntry.isNew()) throw EditableTimeEntryDoesNotHaveAStartTimeSetException()
                    if (editableTimeEntry.isRunning()) throw EditableTimeEntryDoesNotHaveADurationSetException()

                    copy(editableTimeEntry = editableTimeEntry.copy(duration = action.duration))
                }
            }
            StartEditAction.StopButtonTapped -> {
                val editableTimeEntry = state().editableTimeEntry
                if (editableTimeEntry.isStopped())
                    return noEffect()

                val startTime = editableTimeEntry.startTime
                val now = timeService.now()

                if (startTime == null)
                    return state.mutateWithoutEffects {
                        copy(
                            editableTimeEntry = editableTimeEntry.copy(
                                startTime = now,
                                duration = Duration.ZERO
                            )
                        )
                    }

                val maxDuration = Duration.ofHours(maxDurationInHours)
                val durationUntilNow = startTime.absoluteDurationBetween(now)
                if (startTime > now || durationUntilNow > maxDuration)
                    return noEffect()

                state.mutateWithoutEffects {
                    copy(
                        editableTimeEntry = editableTimeEntry.copy(
                            duration = durationUntilNow
                        )
                    )
                }
            }
            is StartEditAction.TagCreated -> {
                state.mutateWithoutEffects {
                    copy(
                        tags = tags + (action.tag.id to action.tag),
                        editableTimeEntry = editableTimeEntry.copy(
                            tagIds = editableTimeEntry.tagIds + action.tag.id
                        )
                    )
                }
            }
            is StartEditAction.TimeEntryHandling,
            StartEditAction.Close -> state.popBackStackWithoutEffects()
        }
    }

    private fun StartEditState.modifyWithCreateTagSuggestion(): StartEditState {
        val (token, currentQuery) = editableTimeEntry.description.findTokenAndQueryMatchesForAutocomplete(
            tagToken,
            cursorPosition
        )
        val delimiter = "$token$currentQuery"
        val description = editableTimeEntry.description.substringBeforeLast(delimiter)
        return copy(editableTimeEntry = editableTimeEntry.copy(description = description))
    }

    private fun StartEditState.modifyWithProjectSuggestion(autocompleteSuggestion: StartEditSuggestions.Project): StartEditState {
        val projectSuggestion = autocompleteSuggestion.project
        val (token, currentQuery) = editableTimeEntry.description.findTokenAndQueryMatchesForAutocomplete(
            projectToken,
            cursorPosition
        )
        val delimiter = "$token$currentQuery"
        val description = editableTimeEntry.description.substringBefore(delimiter)
        return copy(
            editableTimeEntry = editableTimeEntry.copy(
                description = description,
                projectId = projectSuggestion.id,
                workspaceId = projectSuggestion.workspaceId
            )
        )
    }

    private fun StartEditState.modifyWithTimeEntrySuggestion(autocompleteSuggestion: StartEditSuggestions.TimeEntry): StartEditState {
        val timeEntrySuggestion = autocompleteSuggestion.timeEntry
        return copy(
            editableTimeEntry = editableTimeEntry.copy(
                workspaceId = timeEntrySuggestion.workspaceId,
                description = timeEntrySuggestion.description,
                projectId = timeEntrySuggestion.projectId,
                tagIds = timeEntrySuggestion.tagIds,
                billable = timeEntrySuggestion.billable,
                taskId = timeEntrySuggestion.taskId
            )
        )
    }

    private fun StartEditState.modifyWithTagSuggestion(autocompleteSuggestion: StartEditSuggestions.Tag): StartEditState {
        val tagSuggestion = autocompleteSuggestion.tag
        if (tags[tagSuggestion.id] == null) throw TagDoesNotExistException()
        val (token, currentQuery) = editableTimeEntry.description.findTokenAndQueryMatchesForAutocomplete(
            tagToken,
            cursorPosition
        )
        val delimiter = "$token$currentQuery"
        return copy(
            editableTimeEntry = editableTimeEntry.copy(
                description = editableTimeEntry.description.substringBeforeLast(delimiter),
                tagIds = editableTimeEntry.tagIds + tagSuggestion.id
            )
        )
    }

    private fun StartEditState.modifyWithCreateProjectSuggestion(autocompleteSuggestion: StartEditSuggestions.CreateProject): StartEditState {
        val editableProject = EditableProject(
            name = autocompleteSuggestion.name,
            workspaceId = editableTimeEntry.workspaceId
        )
        val route = Route.Project(editableProject)
        return copy(backStack = backStack.push(route))
    }

    private fun StartEditState.modifyWithTaskSuggestion(autocompleteSuggestion: StartEditSuggestions.Task): StartEditState {
        val taskSuggestion = autocompleteSuggestion.task
        if (this.projects[taskSuggestion.projectId] == null) throw ProjectDoesNotExistException()
        val (token, currentQuery) = editableTimeEntry.description.findTokenAndQueryMatchesForAutocomplete(
            projectToken,
            cursorPosition
        )
        val delimiter = "$token$currentQuery"
        return copy(
            editableTimeEntry = editableTimeEntry.copy(
                description = editableTimeEntry.description.substringBefore(delimiter),
                projectId = taskSuggestion.projectId,
                taskId = taskSuggestion.id,
                workspaceId = taskSuggestion.workspaceId
            )
        )
    }

    private fun StartEditState.handleEndTimeEdition(
        editableTimeEntry: EditableTimeEntry,
        action: StartEditAction.DateTimePicked
    ): StartEditState {
        val startTime = editableTimeEntry.startTime ?: throw EditableTimeEntryDoesNotHaveAStartTimeSetException()
        return copy(
            dateTimePickMode = DateTimePickMode.None,
            temporalInconsistency = TemporalInconsistency.None,
            editableTimeEntry = editableTimeEntry.copy(
                duration = startTime.absoluteDurationBetween(action.dateTime)
            )
        )
    }

    private fun StartEditState.handleStartTimeEdition(
        editableTimeEntry: EditableTimeEntry,
        action: StartEditAction.DateTimePicked
    ): StartEditState {
        val originalDuration = editableTimeEntry.duration
        return if (originalDuration == null) {
            copy(
                dateTimePickMode = DateTimePickMode.None,
                temporalInconsistency = TemporalInconsistency.None,
                editableTimeEntry = editableTimeEntry.copy(
                    startTime = action.dateTime
                )
            )
        } else {
            val originalStartTime = editableTimeEntry.startTime ?: throw EditableTimeEntryDoesNotHaveAStartTimeSetException()
            copy(
                dateTimePickMode = DateTimePickMode.None,
                temporalInconsistency = TemporalInconsistency.None,
                editableTimeEntry = editableTimeEntry.copy(
                    startTime = action.dateTime,
                    duration = action.dateTime.absoluteDurationBetween(originalStartTime + originalDuration)
                )
            )
        }
    }

    private fun detectTemporalInconsistencies(
        editableTimeEntry: EditableTimeEntry,
        mode: DateTimePickMode,
        newDateTime: OffsetDateTime
    ): TemporalInconsistency {
        val maxDuration = Duration.ofHours(999)
        val now = timeService.now()
        val startTime = editableTimeEntry.startTime
        val duration = editableTimeEntry.duration
        val endTime = (startTime ?: now) + (duration ?: Duration.ZERO)

        return when {
            startTime == null -> detectTemporalInconsistenciesOnEmptyTimeEntry(mode, newDateTime, now, maxDuration)
            duration == null -> detectTemporalInconsistenciesOnRunningTimeEntry(mode, newDateTime, now, maxDuration)
            else -> detectTemporalInconsistenciesOnStoppedTimeEntry(mode, startTime, endTime, newDateTime, maxDuration)
        }
    }

    private fun detectTemporalInconsistenciesOnStoppedTimeEntry(
        mode: DateTimePickMode,
        startTime: OffsetDateTime,
        endTime: OffsetDateTime,
        newDateTime: OffsetDateTime,
        maxDuration: Duration
    ): TemporalInconsistency = when {
        mode.targetsStart() && newDateTime > endTime -> StartTimeAfterStopTime
        mode.targetsStart() && endTime.absoluteDurationBetween(newDateTime) > maxDuration -> DurationTooLong
        mode.targetsEnd() && newDateTime < startTime -> StopTimeBeforeStartTime
        mode.targetsEnd() && newDateTime.absoluteDurationBetween(startTime) > maxDuration -> DurationTooLong
        else -> TemporalInconsistency.None
    }

    private fun detectTemporalInconsistenciesOnRunningTimeEntry(
        mode: DateTimePickMode,
        newDateTime: OffsetDateTime,
        now: OffsetDateTime,
        maxDuration: Duration
    ): TemporalInconsistency = when {
        mode.targetsStart() && now.absoluteDurationBetween(newDateTime) > maxDuration -> DurationTooLong
        mode.targetsStart() && newDateTime > now -> StartTimeAfterCurrentTime
        mode.targetsEnd() -> throw EditableTimeEntryDoesNotHaveADurationSetException()
        else -> TemporalInconsistency.None
    }

    private fun detectTemporalInconsistenciesOnEmptyTimeEntry(
        mode: DateTimePickMode,
        newDateTime: OffsetDateTime,
        now: OffsetDateTime,
        maxDuration: Duration
    ): TemporalInconsistency = when {
        mode.targetsStart() && now.absoluteDurationBetween(newDateTime) > maxDuration -> DurationTooLong
        mode.targetsEnd() -> throw EditableTimeEntryDoesNotHaveAStartTimeSetException()
        else -> TemporalInconsistency.None
    }

    private fun updateAutocompleteSuggestions(
        action: StartEditAction.DescriptionEntered,
        state: StartEditState
    ): List<Effect<StartEditAction>> =
        effect(
            UpdateAutocompleteSuggestionsEffect(
                dispatcherProvider,
                action.description,
                action.cursorPosition,
                state.editableTimeEntry.workspaceId,
                state.tags,
                state.tasks,
                state.clients,
                state.projects,
                state.timeEntries
            )
        )

    private fun EditableTimeEntry.endTime(now: OffsetDateTime): OffsetDateTime =
        startTime?.let { startTime ->
            val relativeDuration = duration ?: startTime.absoluteDurationBetween(now)
            startTime + relativeDuration
        } ?: now

    private fun DateTimePickMode.targetsStart() = this == DateTimePickMode.StartDate || this == DateTimePickMode.StartTime
    private fun DateTimePickMode.targetsEnd() = this == DateTimePickMode.EndDate || this == DateTimePickMode.EndTime
}
