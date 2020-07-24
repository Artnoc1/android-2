package com.toggl.common.feature.navigation

import androidx.navigation.NavController
import com.toggl.common.DeepLinkUrls
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Router @Inject constructor(private val deepLinkUrls: DeepLinkUrls) {
    private var currentBackStack: BackStack = backStackOf(Route.Timer)

    @Suppress("UNCHECKED_CAST")
    fun processNewBackStack(
        newBackStack: BackStack,
        navController: NavController
    ) {
        val oldBackStack = currentBackStack

        val operations = sequence {

            val neededPopOperations = calculatePopOperations(oldBackStack, newBackStack)

            yieldAll(neededPopOperations)

            val numberOfValidOperations = oldBackStack.size - neededPopOperations.size

            for (i in numberOfValidOperations until newBackStack.size) {
                val deepLink = newBackStack[i].deepLink(deepLinkUrls)
                val route = BackStackOperation.Push(deepLink)
                yield(route)
            }
        }

        for (operation in operations) {
            when (operation) {
                is BackStackOperation.Push -> navController.navigate(operation.deepLink)
                BackStackOperation.Pop -> navController.popBackStack()
            }
        }

        currentBackStack = newBackStack
    }

    private fun calculatePopOperations(oldBackStack: BackStack, newBackStack: BackStack): List<BackStackOperation> =
        oldBackStack.foldIndexed(emptyList<BackStackOperation>()) { index, popOperations, oldRoute ->
            if (popOperations.any()) {
                // As soon as any operation is invalid, we need to pop the rest
                return@foldIndexed popOperations + BackStackOperation.Pop
            }

            val newRoute = newBackStack.getOrNull(index)
            if (newRoute != null && oldRoute.isSameTypeAs(newRoute)) popOperations
            else listOf(BackStackOperation.Pop)
        }
}