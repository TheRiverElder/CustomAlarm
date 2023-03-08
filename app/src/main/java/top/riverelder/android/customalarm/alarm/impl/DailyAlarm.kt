package top.riverelder.android.customalarm.alarm.impl

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.em
import androidx.core.os.bundleOf
import top.riverelder.android.customalarm.*
import top.riverelder.android.customalarm.alarm.Alarm
import top.riverelder.android.customalarm.alarm.AlarmType
import top.riverelder.android.customalarm.ui.components.DigitInput
import java.lang.Integer.parseInt
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.*

object DailyAlarmType : AlarmType {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)

    override val id: String
        get() = "daily"

    override fun create(initialTime: Date): DailyAlarm = DailyAlarm()

    @Composable
    override fun AlarmConfigurationView(
        context: Context,
        bundle: Bundle,
        onChange: (Bundle) -> Unit
    ) {
        val previousTime = Date(bundle.getLong("dailyTime", Date().time)).calendar
        val previousTimeHour = previousTime.get(HOUR_OF_DAY)
        val previousTimeMinute = previousTime.get(MINUTE)
        val previousMaxDelayMinutes = bundle.getInt("maxDelayMinutes", 1)

        fun notifyChange(
            dailyTimeHour: Int = previousTimeHour,
            dailyTimeMinute: Int = previousTimeMinute,
            maxDelayMinutes: Int = previousMaxDelayMinutes,
        ) {
            Log.d("maxDelayMinutes", maxDelayMinutes.toString())
            onChange(
                bundleOf(
                    "dailyTime" to getTime(hour = dailyTimeHour, minute = dailyTimeMinute).time,
                    "maxDelayMinutes" to maxDelayMinutes,
                )
            )
        }

        val timePickerDialog = TimePickerDialog(context, { _, hourOfDay, minute ->
            notifyChange(dailyTimeHour = hourOfDay, dailyTimeMinute = minute)
        }, previousTimeHour, previousTimeMinute, true)

        Column {
            Row(verticalAlignment = CenterVertically) {
                Text(text = "当前定时：", fontSize = 5.em)
                Text(
                    text = timeFormat.format(previousTime.time),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FONT_FAMILY_SMILEY_SANS,
                    fontSize = 7.em,
                    modifier = Modifier.weight(1f),
                )
                Button(modifier = Modifier.wrapContentSize(), onClick = { timePickerDialog.show() }) {
                    Text(text = "改变定时")
                }
            }
            Row(verticalAlignment = CenterVertically) {
                Text(text = "允许延迟：", fontSize = 5.em)
                DigitInput(
                    value = previousMaxDelayMinutes.toLong(),
                    onValueChange = { notifyChange(maxDelayMinutes = it.toInt()) },
                    textStyle = TextStyle(fontSize = 7.em, fontFamily = FONT_FAMILY_SMILEY_SANS)
                )
                Text(text = "分钟")
            }
        }
    }


}

class DailyAlarm : Alarm {

    override val type: AlarmType
        get() = DailyAlarmType

    override var name: String = "每日闹铃"

    override var scheduled: Boolean = false

    var dailyTime: Date = Date(0)
    var maxDelayMinutes: Int = 1

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
        if (checkerCalendar.after(queryCalendar)) return checkerCalendar.also {
            it.add(
                DATE,
                order
            )
        }.time
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
        checkerCalendar.add(MINUTE, maxDelayMinutes)
        return checkerCalendar.after(queryCalendar)
    }

    override fun ring(service: AlarmService) {
        Toast.makeText(service, "醒醒！", Toast.LENGTH_SHORT).show()
    }

    override var properties: Bundle
        get() = bundleOf(
            "dailyTime" to dailyTime.time,
            "maxDelayMinutes" to maxDelayMinutes,
        )
        set(bundle) {
            dailyTime = Date(bundle.getLong("dailyTime", dailyTime.time))
            maxDelayMinutes = bundle.getInt("maxDelayMinutes", maxDelayMinutes)
        }

}