package top.riverelder.android.customalarm

import top.riverelder.android.customalarm.alarm.Alarm
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

object CustomAlarmManager {
    private val alarms: MutableList<Alarm> = ArrayList()

    public val onAlarmAddedListeners: Set<(Alarm) -> Unit> = HashSet()
    public val onAlarmRemovedListeners: Set<(Alarm) -> Unit> = HashSet()
    public val onAlarmUpdatedListeners: Set<(Alarm) -> Unit> = HashSet()

    fun addAlarm(alarm: Alarm) {
        if (!alarms.add(alarm)) return
        onAlarmAddedListeners.forEach { it(alarm) }
    }

    fun removeAlarm(alarm: Alarm) {
        if (!alarms.remove(alarm)) return
        onAlarmRemovedListeners.forEach { it(alarm) }
    }

    fun updateAlarm(alarm: Alarm) {
        if (!alarms.contains(alarm)) return
        onAlarmUpdatedListeners.forEach { it(alarm) }
    }

    fun getAlarms(): List<Alarm> = alarms.slice(0 until alarms.size)

    fun getAlarm(index: Int): Alarm? = alarms.getOrNull(index)

    fun getNextRing(currentTime: Date, maxDelayMinutes: Int): Pair<Alarm, Date>? {
        var nextRingTime: Date? = null
        var nextAlarm: Alarm? = null
        for (alarm in alarms) {
            if (alarm.scheduled) continue

            val ringTime = alarm.followingRingTime(currentTime)
            if (ringTime == null || (ringTime.time - currentTime.time) > (maxDelayMinutes * 60 * 1000)) continue

            if (nextRingTime == null || ringTime.before(nextRingTime)) {
                nextRingTime = ringTime
                nextAlarm = alarm
            }
        }

        return if (nextRingTime != null && nextAlarm != null) Pair(nextAlarm, nextRingTime) else null
    }
}