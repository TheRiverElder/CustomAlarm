package top.riverelder.android.customalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import top.riverelder.android.customalarm.alarm.Alarm
import java.util.*
import kotlin.collections.HashSet
import kotlin.concurrent.timerTask


class AlarmService : Service() {

    inner class AlarmServiceBinder : Binder() {
        val service: AlarmService
            get() = this@AlarmService
    }

    override fun onBind(intent: Intent?): IBinder {
        return AlarmServiceBinder()
    }

    private val alarmManagerMutatedListener: (CustomAlarmManager) -> Unit = {
        Log.d(this::class.simpleName + ".alarmManagerMutatedListener", "manager")
        enqueuedTimerTasks.forEach { it.cancel() }
        enqueuedTimerTasks.clear()
        scheduleNextRing()
        CustomAlarmManager.saveTo(dataDir)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        protectRunning()
        scheduleNextRing()
        
        CustomAlarmManager.onManagerUpdatedListeners.add(alarmManagerMutatedListener)

        CustomAlarmManager.loadFrom(dataDir)

        val filter = IntentFilter() //创建意图过滤器对象
        filter.addAction(Intent.ACTION_TIME_TICK) //为接收器指定action，使之用于接收同action的广播
        registerReceiver(MinuteBroadcastReceiver(), filter) //动态注册广播接收器

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        cleanup()
    }

    private fun cleanup() {
        releaseRunning()

        CustomAlarmManager.onManagerUpdatedListeners.remove(alarmManagerMutatedListener)

        CustomAlarmManager.saveTo(dataDir)
    }

    private fun protectRunning() {
        val channel = NotificationChannel(packageName, "ForegroundService", NotificationManager.IMPORTANCE_NONE)
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)

        val notification = Notification.Builder(this, channel.id)
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher))
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText(getString(R.string.notification_hint))
            .setWhen(System.currentTimeMillis())
            .build()
        startForeground(1000, notification)
    }

    private fun releaseRunning() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun onMinute() {
        scheduleNextRing()
    }

    private var timer = Timer()
    private var enqueuedTimerTasks = HashSet<TimerTask>()

    fun scheduleNextRing() {
        val currentTime = currentTime()
        val nextRing = CustomAlarmManager.getNextRing(currentTime, 1) ?: return

        val nextAlarm = nextRing.first
        val nextRingTime = nextRing.second

        synchronized(this) {
            nextAlarm.scheduled = true

            Log.d("CustomAlarmManager.scheduleNextRing", DATE_TIME_FORMAT.format(nextRingTime))

            val timerTask = timerTask {
                enqueuedTimerTasks.remove(this)
                ringAlarms(Date())
                scheduleNextRing()
            }
            enqueuedTimerTasks.add(timerTask)
            try {
                timer.schedule(timerTask, nextRingTime)
            } catch (e: Exception) {
                Log.e("CustomAlarmManager.scheduleNextRing", nextRingTime.time.toString())
            }
        }

    }

    fun ringAlarms(time: Date) {
        CustomAlarmManager.getAlarms()
            .filter { it.shouldRing(time) }
            .forEach { Handler(Looper.getMainLooper()).post {
                Log.i("CustomAlarmManager.ringAlarms", it.name)
                it.ring(this)
                it.scheduled = false
                CustomAlarmManager.updateAlarm(it)
            } }
    }

    inner class MinuteBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == Intent.ACTION_TIME_TICK) {
                onMinute()
//                Log.d("ON_MINUTE", "现在是${DateFormat.getTimeInstance(DateFormat.FULL, Locale.CHINA).format(Date())}，又一分钟了！")
            }
        }
    }
}