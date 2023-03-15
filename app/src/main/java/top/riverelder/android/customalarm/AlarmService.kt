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
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.File
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

    val alarmsDir: File
        get() = File(dataDir, "alarms")

    private val alarmManagerMutatedListener: (CustomAlarmManager) -> Unit = {
        Log.d(this::class.simpleName + ".alarmManagerMutatedListener", "manager")
        enqueuedTimerTasks.forEach { it.cancel() }
        enqueuedTimerTasks.clear()
        scheduleNextRing()
        CustomAlarmManager.saveTo(alarmsDir)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Toast.makeText(this, "闹铃服务器启动", Toast.LENGTH_SHORT).show()

        return START_STICKY
    }

    override fun onCreate() {
        Toast.makeText(this, "闹铃服务器创建", Toast.LENGTH_SHORT).show()
        try {
            protectRunning()
            scheduleNextRing()

            CustomAlarmManager.onManagerUpdatedListeners.add(alarmManagerMutatedListener)

            CustomAlarmManager.loadFrom(alarmsDir)

            val filter = IntentFilter() //创建意图过滤器对象
            filter.addAction(Intent.ACTION_TIME_TICK) //为接收器指定action，使之用于接收同action的广播
            registerReceiver(MinuteBroadcastReceiver(), filter) //动态注册广播接收器
        } catch (e: Exception) {
            val exceptionMessage = e.javaClass.name + " " + e.message.toString()
            Log.e(this::class.simpleName, exceptionMessage)
            Toast.makeText(this, "闹铃服务器创建异常：$exceptionMessage", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        Toast.makeText(this, "闹铃服务器销毁", Toast.LENGTH_SHORT).show()
        super.onDestroy()
        cleanup()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Toast.makeText(this, "闹铃服务器移除", Toast.LENGTH_SHORT).show()
        super.onTaskRemoved(rootIntent)
        cleanup()
    }

    private fun cleanup() {
        releaseRunning()

        CustomAlarmManager.onManagerUpdatedListeners.remove(alarmManagerMutatedListener)

        CustomAlarmManager.saveTo(alarmsDir)
    }

    private fun protectRunning() {
        val channel = NotificationChannel(packageName, "ForegroundService", NotificationManager.IMPORTANCE_LOW)
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