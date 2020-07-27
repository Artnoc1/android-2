package com.toggl.timer.startedit.domain

import com.toggl.architecture.extensions.noEffect
import com.toggl.timer.common.FreeCoroutineSpec
import com.toggl.timer.common.toMutableValue
import io.kotest.properties.Gen
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class BillableTappedActionTests : FreeCoroutineSpec() {
    init {
        val reducer = createReducer()

        "The BillableTapped action" - {
            "should invert the values of the editable time entry" - {
                Gen.bool().constants().forEach { originalBillableValue ->
                    var state = createInitialState().let {
                        it.copy(
                            editableTimeEntry = it.editableTimeEntry.copy(billable = originalBillableValue)
                        )
                    }
                    val mutableValue = state.toMutableValue { state = it }

                    val effects = reducer.reduce(mutableValue, StartEditAction.BillableTapped)
                    effects shouldBe noEffect()
                    state.editableTimeEntry.billable shouldBe !originalBillableValue
                }
            }
        }
    }
}