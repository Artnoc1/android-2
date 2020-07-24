package com.toggl.common.feature.navigation

typealias BackStack = List<Route>

inline fun <reified R : Route> BackStack.popUntil() =
    generateSequence(
        seedFunction = { this },
        nextFunction = {
            if (it is R) null
            else pop()
        }
    ).lastOrNull() ?: this

fun BackStack.push(route: Route) =
    this + route

fun BackStack.pop() =
    this.dropLast(1)

inline fun <reified P> BackStack.getRouteParam(): P? =
    mapNotNull {
        when {
            it !is ParameterRoute<*> -> null
            it.parameter !is P -> null
            else -> it.parameter
        }
    }.firstOrNull() as? P

inline fun <reified R : Route> BackStack.setRouteParam(paramSetter: () -> R): BackStack =
    map {
        if (it !is R) it
        else paramSetter()
    }