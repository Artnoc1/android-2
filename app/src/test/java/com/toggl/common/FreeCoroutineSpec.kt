package com.toggl.common

import com.toggl.architecture.DispatcherProvider
import io.kotest.TestCase
import io.kotest.TestResult
import io.kotest.specs.AbstractFreeSpec
import io.kotest.specs.FreeSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@ExperimentalCoroutinesApi
open class FreeCoroutineSpec(
    var dispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher(),
    body: AbstractFreeSpec.() -> Unit = {}
) : FreeSpec(body) {
    var dispatcherProvider: DispatcherProvider = DispatcherProvider(dispatcher, dispatcher, Dispatchers.Main)

    override fun beforeTest(testCase: TestCase) {
        Dispatchers.setMain(dispatcher)
        super.beforeTest(testCase)
    }

    override fun afterTest(testCase: TestCase, result: TestResult) {
        super.afterTest(testCase, result)
        Dispatchers.resetMain()
        dispatcher.cleanupTestCoroutines()
    }
}