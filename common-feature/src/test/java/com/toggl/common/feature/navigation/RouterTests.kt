package com.toggl.common.feature.navigation

import android.net.Uri
import androidx.navigation.NavController
import com.toggl.common.DeepLinkUrls
import com.toggl.common.feature.common.CoroutineTest
import com.toggl.models.domain.EditableProject
import com.toggl.models.domain.EditableTimeEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
@DisplayName("The Router")
class RouterTests : CoroutineTest() {

    private val deepLinks = DeepLinkUrls(
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk()
    )

    private val router = Router(deepLinks)
    private val navController = mockk<NavController> {
        every { navigate(any<Uri>()) } returns Unit
        every { popBackStack() } returns true
    }

    @Test
    fun `calls navigate once for each entity in the stack`() = runBlockingTest {

        val stack = listOf<Route>(
            Route.StartEdit(EditableTimeEntry.empty(1)),
            Route.Project(EditableProject.empty(1))
        )

        router.processNewBackStack(stack, navController)

        verifyOrder {
            navController.navigate(deepLinks.startEditDialog)
            navController.navigate(deepLinks.projectDialog)
        }
    }

    @Test
    fun `calls calls pop when the new stack is smaller`() = runBlockingTest {

        val oldStack = listOf<Route>(
            Route.StartEdit(EditableTimeEntry.empty(1)),
            Route.Project(EditableProject.empty(1))
        )
        val newStack = listOf<Route>(
            Route.StartEdit(EditableTimeEntry.empty(1))
        )

        router.processNewBackStack(oldStack, navController)
        router.processNewBackStack(newStack, navController)

        verifyOrder {
            navController.navigate(deepLinks.startEditDialog)
            navController.navigate(deepLinks.projectDialog)
            navController.popBackStack()
        }
    }

    @Test
    fun `does not call the navController when the routes are the same but the parameters are different`() = runBlockingTest {

        val oldStack = listOf<Route>(
            Route.StartEdit(EditableTimeEntry.empty(1)),
            Route.Project(EditableProject.empty(1))
        )
        val newStack = listOf<Route>(
            Route.StartEdit(EditableTimeEntry.empty(1)),
            Route.StartEdit(EditableTimeEntry.empty(12))
        )

        router.processNewBackStack(oldStack, navController)
        router.processNewBackStack(newStack, navController)

        verifyOrder {
            navController.navigate(deepLinks.startEditDialog)
            navController.navigate(deepLinks.projectDialog)
        }
    }

    @Test
    fun `navigates between stacks with the same size`() = runBlockingTest {

        val oldStack = listOf(Route.Timer)
        val newStack = listOf<Route>(Route.Login)

        router.processNewBackStack(oldStack, navController)
        router.processNewBackStack(newStack, navController)

        verifyOrder {
            navController.popBackStack()
            navController.navigate(deepLinks.login)
        }
    }

    @Test
    fun `pops everything that is different from both stacks and only navigates what is needed`() = runBlockingTest {

        val oldStack = listOf(
            Route.Timer,
            Route.StartEdit(EditableTimeEntry.empty(1)),
            Route.Project(EditableProject.empty(1))
        )
        val newStack = listOf<Route>(
            Route.Timer
        )

        router.processNewBackStack(oldStack, navController)
        router.processNewBackStack(newStack, navController)

        verifyOrder {
            navController.navigate(deepLinks.startEditDialog)
            navController.navigate(deepLinks.projectDialog)
            navController.popBackStack()
            navController.popBackStack()
        }
    }
}