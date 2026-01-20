package com.novelsim.app.util

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object PlatformUtils {
    actual fun getCurrentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
}
