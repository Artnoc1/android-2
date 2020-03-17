package com.toggl.environment.services.analytics

import javax.inject.Inject

class CompositeAnalyticsService @Inject constructor(vararg val analyticsServices: AnalyticsService) :
    AnalyticsService {

    override fun track(event: Event) =
        analyticsServices.forEach { it.track(event) }
}
