package top.riverelder.android.customalarm

import top.riverelder.android.customalarm.alarm.Alarm
import top.riverelder.android.customalarm.alarm.AlarmType
import top.riverelder.android.customalarm.alarm.impl.DailyAlarmType
import java.util.*

object CustomAlarmManager {

    private val alarmTypes: MutableMap<String, AlarmType> = HashMap()

    public fun registerAlarmType(alarmType: AlarmType) {
        alarmTypes[alarmType.id] = alarmType
    }

    public fun getAlarmType(alarmTypeId: String): AlarmType? = alarmTypes[alarmTypeId]

    public fun getAlarmTypes(): Collection<AlarmType> = alarmTypes.values

    private val alarms: MutableList<Alarm> = ArrayList()

    public val onAlarmAddedListeners: MutableSet<(Alarm) -> Unit> = HashSet()
    public val onAlarmRemovedListeners: MutableSet<(Alarm) -> Unit> = HashSet()
    public val onAlarmUpdatedListeners: MutableSet<(Alarm) -> Unit> = HashSet()

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



    init {
        registerAlarmType(DailyAlarmType)
    }


    fun getNextRing(currentTime: Date, maxDelayMinutes: Int): Pair<Alarm, Date>? {
        var nextRingTime: Date? = null
        var nextAlarm: Alarm? = null
        for (alarm in alarms) {
            if (alarm.scheduled) continue

            val ringTime = alarm.followingRingTime(currentTime) ?: continue
            val deltaMilliseconds = (ringTime.time - currentTime.time)
            if (deltaMilliseconds > (1000L * 60 * maxDelayMinutes)) continue

            if (nextRingTime == null || ringTime.before(nextRingTime)) {
                nextRingTime = ringTime
                nextAlarm = alarm
            }
        }

        return if (nextRingTime != null && nextAlarm != null) Pair(nextAlarm, nextRingTime) else null
    }
}