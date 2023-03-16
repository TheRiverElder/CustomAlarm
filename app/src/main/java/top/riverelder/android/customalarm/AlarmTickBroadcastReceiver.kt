package top.riverelder.android.customalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startForegroundService

class AlarmTickBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        val serviceIntent = Intent(context, AlarmService::class.java)
        startForegroundService(context, serviceIntent)
    }

}