package top.riverelder.android.customalarm.alarm.impl

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.os.bundleOf
import top.riverelder.android.customalarm.AlarmService
import top.riverelder.android.customalarm.FONT_FAMILY_SMILEY_SANS
import top.riverelder.android.customalarm.alarm.Alarm
import top.riverelder.android.customalarm.alarm.AlarmConfigurationMetadata
import top.riverelder.android.customalarm.alarm.AlarmConfigurationMetadataItem
import top.riverelder.android.customalarm.alarm.AlarmType
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.*

object DailyAlarmType : AlarmType {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)

    override val id: String
        get() = "daily"

    override fun create(initialTime: Date): DailyAlarm = DailyAlarm()

    @Composable
    override fun AlarmConfigurationView(context: Context, bundle: Bundle, onChange: (Bundle) -> Unit) {

        val previousTime = Calendar.getInstance().also { Date(bundle.getLong("dailyTime", Date().time)) }
        val previousDelayMinutes = bundle.getInt("delayMinutes", 1)

        var dailyTimeHour by remember { mutableStateOf(previousTime.get((HOUR))) }
        var dailyTimeMinute by remember { mutableStateOf(previousTime.get(MINUTE)) }
        var delayMinutes by remember { mutableStateOf(previousDelayMinutes) }
        var delayMinutesString by remember { mutableStateOf(previousDelayMinutes.toString()) }

        val timePickerDialog = TimePickerDialog(context, { _, hourOfDay, minute ->
            dailyTimeHour = hourOfDay
            dailyTimeMinute = minute
        }, dailyTimeHour, dailyTimeMinute, true)

        fun getTime(): Date =
            Calendar.getInstance().also { it.set(0, 0, 0, dailyTimeHour, dailyTimeMinute, 0) }.time

        Column {
            Column(Modifier.weight(1f)) {
                Row {
                    Text(text = "当前定时：", modifier = Modifier.align(CenterVertically))
                    Text(
                        text = timeFormat.format(getTime()),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FONT_FAMILY_SMILEY_SANS,
                        modifier = Modifier
                            .weight(1f)
                            .align(CenterVertically),
                    )
                    Button(
                        modifier = Modifier
                            .wrapContentSize()
                            .align(CenterVertically),
                        onClick = { timePickerDialog.show() },
                    ) {
                        Text(text = "改变定时")
                    }
                }
                Row {
                    Text(text = "允许延迟：", modifier = Modifier.align(CenterVertically))
                    BasicTextField(
                        value = delayMinutesString,
                        onValueChange = { delayMinutesString = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontSize = 5.em, fontFamily = FONT_FAMILY_SMILEY_SANS),
                        modifier = Modifier.align(CenterVertically),
                    )
                }
            }
            Button(
                modifier = Modifier.align(CenterHorizontally),
                onClick = { onChange(bundleOf(
                    "dailyTime" to getTime().time,
                    "delayMinutes" to try { delayMinutesString.toInt() } catch (ignored: Exception) { previousDelayMinutes },
                )) }
            ) {
                Text(text = "确认")
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

    override fun ring(service: AlarmService) {
        Toast.makeText(service, "醒醒！", Toast.LENGTH_SHORT).show()
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