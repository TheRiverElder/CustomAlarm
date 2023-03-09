package top.riverelder.android.customalarm

import android.util.Log
import java.util.*

fun getTime(
    year: Int = 0,
    month: Int = 0,
    date: Int = 0,
    hour: Int = 0,
    minute: Int = 0,
    second: Int = 0,
    millisecond: Int = 0,
): Date {
    return Calendar.getInstance().also {
        it.timeZone = TimeZone.getDefault()
        it.set(year, month, date, hour, minute, second)
        it.set(Calendar.MILLISECOND, millisecond)
    }.time
}

fun currentTime(): Date = Calendar.getInstance().also {
    it.timeZone = TimeZone.getDefault()
    it.time = Date(System.currentTimeMillis())
}.time

fun ignoreThrowable(block: () -> Unit) {
    try {
        block()
    } catch (ignored: Throwable) { }
}

val Date.calendar: Calendar get() = Calendar.getInstance().also {
    it.timeZone = TimeZone.getDefault()
    it.time = this
}