package com.toggl.common.feature.navigation

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.google.android.material.bottomsheet.BottomSheetBehavior
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Navigator.Name("bottom_sheet")
class BottomSheetNavigator @Inject constructor() : Navigator<BottomSheetNavigator.Destination>(), LifecycleObserver {
    private var bottomSheetBehavior: BottomSheetBehavior<*>? = null
    private var bottomSheetManualDismissCallback: (() -> Unit)? = null

    override fun navigate(
        destination: Destination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ): NavDestination? {
        bottomSheetBehavior?.let {
            it.state = BottomSheetBehavior.STATE_EXPANDED
            it.addBottomSheetCallback(dismissCallback)
        }
        return destination
    }

    override fun popBackStack(): Boolean {
        bottomSheetBehavior?.let {
            it.removeBottomSheetCallback(dismissCallback)
            if (it.state != BottomSheetBehavior.STATE_HIDDEN) {
                it.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
        return true
    }

    override fun createDestination(): Destination =
        Destination()

    fun setupWithBehaviour(
        behavior: BottomSheetBehavior<*>,
        lifecycleOwner: LifecycleOwner,
        onBottomSheetManualDismissCallback: () -> Unit
    ) {
        behavior.addBottomSheetCallback(dismissCallback)
        bottomSheetManualDismissCallback = onBottomSheetManualDismissCallback
        bottomSheetBehavior = behavior
        lifecycleOwner.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        bottomSheetManualDismissCallback = null
        bottomSheetBehavior?.removeBottomSheetCallback(dismissCallback)
        bottomSheetBehavior = null
    }

    private var dismissCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetManualDismissCallback?.invoke()
            }
        }
    }

    class Destination : NavDestination("bottom_sheet")
}