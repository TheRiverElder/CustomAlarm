package top.riverelder.android.customalarm.ui.viewmodel

import top.riverelder.android.customalarm.alarm.Alarm
import java.util.*

data class AlarmModel(
    val name: String,
    val followingRingTime: Date?,
)

fun Alarm.getModel(currentTime: Date): AlarmModel =
    AlarmModel(name, followingRingTime(currentTime))