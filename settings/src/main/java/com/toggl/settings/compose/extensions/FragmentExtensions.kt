package com.toggl.settings.compose.extensions

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.fragment.app.Fragment
import androidx.ui.core.setContent
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import com.toggl.common.extensions.doOnInsetsChanged

fun Fragment.createComposeView(
    content: @Composable (statusBarHeight: Dp, navigationBarHeight: Dp) -> Unit
) : ViewGroup {

    val composeContainer = FrameLayout(requireContext())

    with(composeContainer) {
        isClickable = false
        isFocusable = false
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        doOnInsetsChanged { statusBarHeight, navigationBarHeight ->
            setContent(androidx.compose.Recomposer.current()) {
                val dpStatusBarHeight = statusBarHeight.pixelsToDp(context)
                content(dpStatusBarHeight.dp, navigationBarHeight.pixelsToDp(context).dp)
            }
        }
    }

    return composeContainer
}


fun Int.pixelsToDp(context: Context): Int =
    (this / context.resources.displayMetrics.density).toInt()