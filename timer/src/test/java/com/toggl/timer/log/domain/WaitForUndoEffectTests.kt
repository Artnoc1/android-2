package com.toggl.timer.log.domain

import com.toggl.common.Constants.timeEntryDeletionDelayMs
import io.kotest.matchers.shouldBe
import io.kotest.specs.FreeSpec
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest

@kotlinx.coroutines.ExperimentalCoroutinesApi
class WaitForUndoEffectTests : FreeSpec({

    "The WaitForUndoEffect" - {
        "when executed, waits and then emits CommitDeletion" - {
            val effect = WaitForUndoEffect(listOf(1, 8, 1337))

            runBlockingTest {
                var action: TimeEntriesLogAction? = null

                launch {
                    action = effect.execute()
                }

                advanceTimeBy(timeEntryDeletionDelayMs - 1)
                action shouldBe null
                advanceTimeBy(1)
                action shouldBe TimeEntriesLogAction.CommitDeletion(listOf(1, 8, 1337))
            }
        }
    }
})