package top.riverelder.android.customalarm

import android.os.Bundle
import android.util.Log
import top.riverelder.android.customalarm.alarm.Alarm
import top.riverelder.android.customalarm.alarm.AlarmType
import top.riverelder.android.customalarm.alarm.impl.DailyAlarmType
import java.io.DataInput
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

        return if (nextRingTime != null && nextAlarm != null) Pair(
            nextAlarm,
            nextRingTime
        ) else null
    }

    fun saveAlarmTo(file: File, alarm: Alarm) {
        DataOutputStream(file.outputStream()).use { output ->
            output.writeUTF(alarm.type.id)
            output.writeInt(alarm.uid)
            output.writeUTF(alarm.name)
            alarm.save(output)
        }
    }

    fun loadAlarmFrom(file: File): Alarm {
        DataInputStream(file.inputStream()).use { input ->
            val alarmTypeId = input.readUTF()
            val alarmType =
                getAlarmType(alarmTypeId) ?: throw Exception("Invalid alarm type id: $alarmTypeId")
            val uid = input.readInt()
            val alarm = alarmType.create(uid)
            val name = input.readUTF()
            alarm.name = name
            alarm.restore(input)
            return alarm
        }
    }

    fun saveTo(directory: File) {
        var counter = 0
        for (alarm in alarms.values) {
            saveAlarmTo(File(directory, alarm.uid.toString()), alarm)
            counter++
        }
        Log.d(this::class.simpleName, "saved $counter")
    }

    fun loadFrom(directory: File) {
        var uidCounter = 0
        var counter = 0
        for (file in (directory.listFiles() ?: return)) {
            if (!file.isFile) continue

            try {
                val alarm = loadAlarmFrom(file)
                alarms[alarm.uid] = alarm
                if (alarm.uid > uidCounter) {
                    uidCounter = alarm.uid
                }
                counter++
            } catch (e: Exception) {
                Log.e("loadFrom", e.javaClass.name + " " + e.message.toString())
            }
        }
        this.uidCounter = uidCounter + 1

        onManagerUpdatedListeners.forEach { it(this) }
        Log.d(this::class.simpleName, "loaded $counter")

    }
}