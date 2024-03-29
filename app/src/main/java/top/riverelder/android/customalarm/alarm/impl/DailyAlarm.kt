package top.riverelder.android.customalarm.alarm.impl

import android.app.TimePickerDialog
import android.content.Context
import android.media.RingtoneManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.os.bundleOf
import top.riverelder.android.customalarm.*
import top.riverelder.android.customalarm.alarm.Alarm
import top.riverelder.android.customalarm.alarm.AlarmType
import top.riverelder.android.customalarm.ui.components.DigitInput
import java.io.DataInput
import java.io.DataOutput
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.*

object DailyAlarmType : AlarmType {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)

    override val id: String
        get() = "daily"

    override fun create(uid: Int, initialTime: Date?): DailyAlarm =
        DailyAlarm(uid).also { initialTime?.let { it1 -> it.setByTime(it1) } }

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

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text(text = "当前定时：", fontSize = 5.em)
                Text(
                    text = timeFormat.format(previousTime.time),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FONT_FAMILY_SMILEY_SANS,
                    fontSize = 7.em,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 0.dp),
                )
            }
            Button(modifier = Modifier.wrapContentSize(), onClick = { timePickerDialog.show() }) {
                Text(text = "改变定时")
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = "允许延迟：", fontSize = 5.em)
            DigitInput(
                value = previousMaxDelayMinutes.toLong(),
                onValueChange = { notifyChange(maxDelayMinutes = it.toInt()) },
                textStyle = TextStyle(fontSize = 7.em, fontFamily = FONT_FAMILY_SMILEY_SANS),
                modifier = Modifier.padding(vertical = 0.dp),
            )
            Text(text = "分钟")
        }

    }


}

class DailyAlarm(
    override val uid: Int
) : Alarm {

    override val type: AlarmType
        get() = DailyAlarmType

    override var name: String = "每日闹铃"

    override var scheduled: Boolean = false

    var dailyTime: Date = Date(0)
    var maxDelayMinutes: Int = 1

    override fun setByTime(time: Date) {
        val calendar = time.calendar
        dailyTime = getTime(hour = calendar.get(HOUR_OF_DAY), minute = calendar.get(MINUTE))
    }

    private fun getCalendarInDate(dateCalendar: Calendar): Calendar {
        val dailyCalendar = dailyTime.calendar
        return getTime(
            year = dateCalendar.get(YEAR),
            month = dateCalendar.get(MONTH),
            date = dateCalendar.get(DATE),
            hour = dailyCalendar.get(HOUR_OF_DAY),
            minute = dailyCalendar.get(MINUTE),
        ).calendar
    }

    override fun followingRingTime(time: Date, order: Int): Date? {
        /***
         *      ┌─┐       ┌─┐
         *   ┌──┘ ┴───────┘ ┴──┐
         *   │                 │
         *   │       ───       │
         *   │  ─┬┘       └┬─  │
         *   │                 │
         *   │       ─┴─       │
         *   │                 │
         *   └───┐         ┌───┘
         *       │         │
         *       │         │
         *       │         │
         *       │         └──────────────┐
         *       │                        │
         *       │                        ├─┐
         *       │                        ┌─┘
         *       │                        │
         *       └─┐  ┐  ┌───────┬──┐  ┌──┘
         *         │ ─┤ ─┤       │ ─┤ ─┤
         *         └──┴──┘       └──┴──┘
         *                神兽保佑
         *               代码无BUG!
         */

        if (order > 0) throw Exception("Cannot supply more than one day")
        val queryCalendar = time.calendar
        // 将检查时刻设为与被检查时刻同一天
        val checkerCalendar = getCalendarInDate(queryCalendar)
//        Log.d("followingRingTime", "compare: queryCalendar ${queryCalendar.time}, checkerCalendar ${checkerCalendar.time}")
        // 如果检查时刻在被检查时刻之后，则说明是在当日还有一次响铃机会
        if (checkerCalendar.time.after(queryCalendar.time)) return checkerCalendar.time
        // 否则只能在次日响铃
        checkerCalendar.add(DAY_OF_MONTH, 1)
        return checkerCalendar.time
    }

    // 与followingRingTime类似，但是获取的是上一个响铃时间，而且返回的时间可以是time
    // 返回的整个时刻不一定又响铃过，只是一个推论时间
    private fun previousRingCalendar(time: Date): Calendar {
        val queryCalendar = time.calendar
        // 将检查时刻设为与被检查时刻同一天
        val checkerCalendar = getCalendarInDate(queryCalendar)
        // 如果检查时刻在被检查时刻之前或者同时，则说明是在当日还有一次响铃机会
        if (!checkerCalendar.time.after(queryCalendar.time)) return checkerCalendar
        // 否则说明上一个应该响铃的时刻是在前一天
        checkerCalendar.add(DATE, -1)
        return checkerCalendar
    }

    override fun shouldRing(time: Date): Boolean {
        val queryCalendar = time.calendar
        val checkerCalendar = previousRingCalendar(time) // 此时checkerCalendar必before queryCalendar
        checkerCalendar.add(MINUTE, maxDelayMinutes)
        return checkerCalendar.time.after(queryCalendar.time)
    }

    override fun ring(service: AlarmService) {
        Toast.makeText(service, "醒醒！", Toast.LENGTH_SHORT).show()
        Log.d("RING", "闹铃响了！")
        val ringtone = RingtoneManager.getRingtone(
            service,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ) ?: return
        if (!ringtone.isPlaying) {
            ringtone.play()
        }
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


    override fun restore(input: DataInput) {
        dailyTime = Date(input.readLong())
        maxDelayMinutes = input.readInt()
    }

    override fun save(output: DataOutput) {
        output.writeLong(dailyTime.time)
        output.writeInt(maxDelayMinutes)
    }
}