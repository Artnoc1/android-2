package com.toggl.common.feature.timeentry

import com.toggl.architecture.DispatcherProvider
import com.toggl.architecture.core.Effect
import com.toggl.architecture.core.MutableValue
import com.toggl.architecture.core.Reducer
import com.toggl.architecture.extensions.effect
import com.toggl.architecture.extensions.noEffect
import com.toggl.common.feature.timeentry.exceptions.TimeEntryDoesNotExistException
import com.toggl.common.services.time.TimeService
import com.toggl.models.domain.EditableTimeEntry
import com.toggl.repository.dto.StartTimeEntryDTO
import com.toggl.repository.interfaces.TimeEntryRepository
import javax.inject.Inject

class TimeEntryReducer @Inject constructor(
    private val repository: TimeEntryRepository,
    private val timeService: TimeService,
    private val dispatcherProvider: DispatcherProvider
) : Reducer<TimeEntryState, TimeEntryAction> {
    override fun reduce(state: MutableValue<TimeEntryState>, action: TimeEntryAction): List<Effect<TimeEntryAction>> {
        return when (action) {
            is TimeEntryAction.StartTimeEntry -> effect(
                StartTimeEntryEffect(
                    action.startTimeEntryDTO,
                    repository,
                    dispatcherProvider
                )
            )
            is TimeEntryAction.ContinueTimeEntry -> {
                val timeEntryToContinue = state().timeEntries[action.id]
                    ?.let(EditableTimeEntry.Companion::fromSingle)
                    ?: throw TimeEntryDoesNotExistException()
                val startEntryDTO = timeEntryToContinue.run {
                    StartTimeEntryDTO(
                        description,
                        timeService.now(),
                        billable,
                        workspaceId,
                        projectId,
                        taskId,
                        tagIds
                    )
                }
                effect(StartTimeEntryEffect(startEntryDTO, repository, dispatcherProvider))
            }
            is TimeEntryAction.DeleteTimeEntry -> {
                val timeEntry = state().timeEntries[action.id] ?: throw TimeEntryDoesNotExistException()
                effect(DeleteTimeEntryEffect(timeEntry, repository, dispatcherProvider))
            }
            is TimeEntryAction.CreateTimeEntry -> effect(
                CreateTimeEntryEffect(
                    action.createTimeEntryDTO,
                    repository,
                    dispatcherProvider
                )
            )

            is TimeEntryAction.StopRunningTimeEntry -> effect(StopTimeEntryEffect(repository, dispatcherProvider))
            is TimeEntryAction.EditTimeEntry -> effect(EditTimeEntryEffect(action.timeEntry, repository, dispatcherProvider))
            TimeEntryAction.TimeEntriesUpdated -> noEffect()
        }
    }
}
