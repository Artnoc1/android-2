package com.toggl.common.extensions

import android.app.Dialog
import android.view.KeyEvent

fun Dialog.setOnBackKeyEventUpCallback(callback: () -> Unit) {
    this.setOnKeyListener { _, _, event ->
        if (event.action == KeyEvent.ACTION_UP && event.keyCode == KeyEvent.KEYCODE_BACK) {
            callback()
            true
        } else {
            false
        }
    }
}