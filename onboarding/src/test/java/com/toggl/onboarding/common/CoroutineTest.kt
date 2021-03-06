package com.toggl.onboarding.common

import com.toggl.architecture.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

@ExperimentalCoroutinesApi
abstract class CoroutineTest {
    private val testDispatcher = TestCoroutineDispatcher()
    protected val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, Dispatchers.Main)

    @BeforeEach
    open fun beforeTest() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    open fun afterTest() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
}
