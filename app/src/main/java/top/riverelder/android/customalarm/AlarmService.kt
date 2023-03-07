package top.riverelder.android.customalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.IBinder
import top.riverelder.android.customalarm.alarm.Alarm


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

    private val alarms: MutableList<Alarm> = ArrayList()
}