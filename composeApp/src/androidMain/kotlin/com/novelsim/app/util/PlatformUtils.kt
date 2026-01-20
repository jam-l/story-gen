package com.novelsim.app.util

actual object PlatformUtils {
    actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
}
