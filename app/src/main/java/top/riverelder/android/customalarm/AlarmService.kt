package top.riverelder.android.customalarm

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder

class AlarmService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        protectRunning()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun protectRunning() {
        val notification = Notification.Builder(this, "custom_alarm")
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
}