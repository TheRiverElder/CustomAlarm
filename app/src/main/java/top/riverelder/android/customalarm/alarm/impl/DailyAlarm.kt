package top.riverelder.android.customalarm.alarm.impl

import android.os.Bundle
import androidx.core.os.bundleOf
import top.riverelder.android.customalarm.alarm.Alarm
import top.riverelder.android.customalarm.alarm.AlarmConfigurationMetadata
import top.riverelder.android.customalarm.alarm.AlarmConfigurationMetadataItem
import java.util.*
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MINUTE
import java.util.Calendar.YEAR
import java.util.Calendar.MONTH
import java.util.Calendar.DATE

class DailyAlarm : Alarm {

    override var name: String = "每日闹铃"

    var dailyTime: Date = Date(0)
    var delayMinutes: Int = 1

    override fun setByTime(time: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = time
        val hour = calendar.get(HOUR_OF_DAY)
        val minute = calendar.get(MINUTE)
        dailyTime = Calendar.getInstance().also { it.set(0, 0, 0, hour, minute) }.time
    }

    private fun getCalendarInDate(date: Date): Calendar =
        getCalendarInDate(Calendar.getInstance().also { it.time = date })

    private fun getCalendarInDate(dateCalendar: Calendar): Calendar {
        val calendar = Calendar.getInstance().also { it.time = dailyTime }
        calendar.set(dateCalendar.get(YEAR), dateCalendar.get(MONTH), dateCalendar.get(DATE))
        return calendar
    }

    override fun followingRingTime(time: Date, order: Int): Date? {
        val queryCalendar = Calendar.getInstance().also { it.time = time }
        // 将检查时刻设为与被检查时刻同一天
        val checkerCalendar = getCalendarInDate(queryCalendar)
        // 如果检查时刻在被检查时刻之后，则说明是在当日还有一次响铃机会
        if (checkerCalendar.after(queryCalendar)) return checkerCalendar.also { it.add(DATE, order) }.time
        // 否则只能在次日响铃
        checkerCalendar.add(DATE, 1 + order)
        return checkerCalendar.time
    }

    // 与followingRingTime类似，但是获取的是上一个响铃时间，而且返回的时间可以是time
    // 返回的整个时刻不一定又响铃过，只是一个推论时间
    private fun previousRingCalendar(time: Date): Calendar {
        val queryCalendar = Calendar.getInstance().also { it.time = time }
        // 将检查时刻设为与被检查时刻同一天
        val checkerCalendar = getCalendarInDate(queryCalendar)
        // 如果检查时刻在被检查时刻之前或者同时，则说明是在当日还有一次响铃机会
        if (!checkerCalendar.after(queryCalendar)) return checkerCalendar
        // 否则说明上一个应该响铃的时刻是在前一天
        checkerCalendar.add(DATE, -1)
        return checkerCalendar
    }

    override fun shouldRing(time: Date): Boolean {
        val queryCalendar = Calendar.getInstance().also { it.time = time }
        val checkerCalendar = previousRingCalendar(time) // 此时checkerCalendar必before queryCalendar
        checkerCalendar.add(MINUTE, delayMinutes)
        return checkerCalendar.after(queryCalendar)
    }

    override var properties: Bundle
        get() = bundleOf(
            "dailyTime" to dailyTime.time,
            "delayMinutes" to delayMinutes,
        )
        set(bundle) {
            dailyTime = Date(bundle.getLong("dailyTime", dailyTime.time))
            delayMinutes = bundle.getInt("delayMinutes", delayMinutes)
        }

    override val configurationMetadata: AlarmConfigurationMetadata
        get() = AlarmConfigurationMetadata(listOf(
            AlarmConfigurationMetadataItem("dailyTime", Long::class.java, true, 0),
            AlarmConfigurationMetadataItem("delayMinutes", Int::class.java, true, 1),
        ))
}