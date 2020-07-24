package com.toggl.common.feature.navigation

import android.app.Dialog
import android.view.KeyEvent
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

fun AppCompatActivity.handleBackPressesEmitting(callback: () -> Unit) {
    val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            callback()
        }
    }

    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            onBackPressedDispatcher.addCallback(backPressedCallback)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            backPressedCallback.remove()
        }
    })
}

fun <Action> Fragment.handleBackPressesEmitting(callback: () -> Unit) {
    val activity = requireActivity()

    val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            callback()
        }
    }

    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            activity.onBackPressedDispatcher.addCallback(backPressedCallback)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            backPressedCallback.remove()
        }
    })
}

fun Dialog.handleBackPressesEmitting(lifecycle: Lifecycle, callback: () -> Unit) {
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            setOnKeyListener { _, _, event ->
                if (event.action == KeyEvent.ACTION_UP && event.keyCode == KeyEvent.KEYCODE_BACK) {
                    callback()
                    true
                } else {
                    false
                }
            }
        }

        override fun onDestroy(owner: LifecycleOwner) {
            setOnKeyListener(null)
        }
    })
}