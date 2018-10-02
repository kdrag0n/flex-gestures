package com.kdrag0n.flexgestures.utils

import android.annotation.SuppressLint
import java.lang.reflect.Method

@SuppressLint("PrivateApi")
object PrivateWindowManager {
    private val int = Int::class.java
    private val service = Class.forName("android.view.WindowManagerGlobal")
            .getMethod("getWindowManagerService")
            .invoke(null)
    private val wmClass: Class<*> = Class.forName("android.view.IWindowManager")

    private fun <T: Any> getMethod(name: String, vararg types: Class<T>): Method {
        return wmClass.getMethod(name, *types)
    }

    fun setOverscan(display: Int, left: Int, top: Int, right: Int, bottom: Int) {
        getMethod("setOverscan", int, int, int, int, int)
                .invoke(service, display, left, top, right, bottom)
    }
}