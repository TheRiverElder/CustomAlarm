package top.riverelder.android.customalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.text.DateFormat
import java.util.*
import kotlin.concurrent.timerTask


class AlarmService : Service() {

    inner class AlarmServiceBinder : Binder() {
        val service: AlarmService
            get() = this@AlarmService
    }

    override fun onBind(intent: Intent?): IBinder {
        return AlarmServiceBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        protectRunning()
        scheduleNextRing()

        val filter = IntentFilter() //创建意图过滤器对象
        filter.addAction(Intent.ACTION_TIME_TICK) //为接收器指定action，使之用于接收同action的广播
        registerReceiver(MinuteBroadcastReceiver(), filter) //动态注册广播接收器

        return super.onStartCommand(intent, flags, startId)
    }

    private fun protectRunning() {
        val channel = NotificationChannel(packageName, "ForegroundService", NotificationManager.IMPORTANCE_NONE)
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)

        val notification = Notification.Builder(this, channel.id)
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher))
            .setContentTitle("Custom Alarm")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText("该提示保证闹钟能正确运行")
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

    private val timer = Timer()

    fun scheduleNextRing() {
        val currentTime = Date()
        val nextRing = CustomAlarmManager.getNextRing(currentTime, 1) ?: return

        val nextAlarm = nextRing.first
        val nextRingTime = nextRing.second

        synchronized(this) {
            nextAlarm.scheduled = true

            timer.schedule(timerTask {
                nextAlarm.scheduled = false
                ringAlarms(Date())
                scheduleNextRing()
            }, nextRingTime)
        }

    }

    fun ringAlarms(time: Date) {
        CustomAlarmManager.getAlarms()
            .filter { it.shouldRing(time) }
            .forEach { it.ring(this) }
    }

    inner class MinuteBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            if (action == Intent.ACTION_TIME_TICK) {
                onMinute()
                Log.d("ON_MINUTE", "现在是${DateFormat.getTimeInstance(DateFormat.FULL, Locale.CHINA).format(Date())}，又一分钟了！")
            }
        }
    }
}