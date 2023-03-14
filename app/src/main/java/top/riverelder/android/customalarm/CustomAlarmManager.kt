package top.riverelder.android.customalarm

import android.util.Log
import top.riverelder.android.customalarm.alarm.Alarm
import top.riverelder.android.customalarm.alarm.AlarmType
import top.riverelder.android.customalarm.alarm.impl.DailyAlarmType
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object CustomAlarmManager {

    private var uidCounter = 0

    public fun nextUid(): Int = uidCounter++

    private val alarmTypes: MutableMap<String, AlarmType> = HashMap()

    public fun registerAlarmType(alarmType: AlarmType) {
        alarmTypes[alarmType.id] = alarmType
    }

    public fun getAlarmType(alarmTypeId: String): AlarmType? = alarmTypes[alarmTypeId]

    public fun getAlarmTypes(): Collection<AlarmType> = alarmTypes.values

    private val alarms: MutableMap<Int, Alarm> = HashMap()

    public val onAlarmAddedListeners: MutableSet<(Alarm) -> Unit> = HashSet()
    public val onAlarmRemovedListeners: MutableSet<(Alarm) -> Unit> = HashSet()
    public val onAlarmUpdatedListeners: MutableSet<(Alarm) -> Unit> = HashSet()
    public val onManagerUpdatedListeners: MutableSet<(CustomAlarmManager) -> Unit> = HashSet()

    fun addAlarm(alarm: Alarm) {
        alarms[alarm.uid] = alarm
        onAlarmAddedListeners.forEach { it(alarm) }
        onManagerUpdatedListeners.forEach { it(this) }
    }

    fun removeAlarm(alarm: Alarm) {
        alarms.remove(alarm.uid)
        onAlarmRemovedListeners.forEach { it(alarm) }
        onManagerUpdatedListeners.forEach { it(this) }
    }

    fun updateAlarm(alarm: Alarm) {
        if (!alarms.contains(alarm.uid)) return
        onAlarmUpdatedListeners.forEach { it(alarm) }
        onManagerUpdatedListeners.forEach { it(this) }
    }

    fun getAlarms(): List<Alarm> = ArrayList(alarms.values)

    fun getAlarm(uid: Int): Alarm? = alarms.getOrDefault(uid, null)



    init {
        registerAlarmType(DailyAlarmType)
    }


    fun getNextRing(currentTime: Date, maxDelayMinutes: Int): Pair<Alarm, Date>? {
        var nextRingTime: Date? = null
        var nextAlarm: Alarm? = null
        for (alarm in alarms.values) {
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

    fun saveAlarmTo(file: File, alarm: Alarm) {
        DataOutputStream(file.outputStream()).use { output ->
            output.writeUTF(alarm.type.id)
            output.writeInt(alarm.uid)
        }
    }

    fun loadAlarmFrom(file: File): Alarm {
        DataInputStream(file.inputStream()).use { input ->
            val alarmTypeId = input.readUTF()
            val alarmType = getAlarmType(alarmTypeId) ?: throw Exception("Invalid alarm type id: $alarmTypeId")
            val uid = input.readInt()
            val alarm = alarmType.restore(uid)
            return alarm
        }
    }

    fun saveTo(directory: File) {
        for (alarm in alarms.values) {
            saveAlarmTo(File(directory, alarm.uid.toString()), alarm)
        }
    }

    fun loadFrom(directory: File) {
        try {
            var uidCounter = 0
            for (file in (directory.listFiles() ?: return)) {
                if (!file.isFile) continue

                val alarm =loadAlarmFrom(file)
                alarms[alarm.uid] = alarm
                if (alarm.uid > uidCounter) {
                    uidCounter = alarm.uid
                }
            }
            this.uidCounter = uidCounter + 1

            onManagerUpdatedListeners.forEach { it(this) }
        } catch (e: Exception) {
            Log.e("loadFrom", e.message.toString())
        }
    }
}