package top.riverelder.android.customalarm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.riverelder.android.customalarm.alarm.Alarm
import top.riverelder.android.customalarm.alarm.impl.DailyAlarm
import top.riverelder.android.customalarm.ui.theme.CustomAlarmTheme
import top.riverelder.android.customalarm.ui.viewmodel.AlarmModel
import top.riverelder.android.customalarm.ui.viewmodel.getModel
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : ComponentActivity() {

    private var alarmManagerListener: ((CustomAlarmManager) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startAlarmService()


        setContent {

            CustomAlarmTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var alarms: List<AlarmModel> by remember { mutableStateOf(
                        with(currentTime()) {
                            CustomAlarmManager.getAlarms().map { it.getModel(this) }
                        }
                    ) }

                    var listener = alarmManagerListener
                    if (listener == null) {
                        listener = {
                            alarms = with(currentTime()) {
                                CustomAlarmManager.getAlarms().map { it.getModel(this) }
                            }
                        }
                        alarmManagerListener = listener
                    }

                    CustomAlarmManager.onManagerUpdatedListeners.add(listener)

                    AlarmList(
                        alarms,
                        onClickAdd = {
                            val intent = Intent(this, AlarmConfigurationActivity::class.java)
                            intent.putExtra("operation", "add")
                            intent.putExtra("alarmTypeId", "daily")
                            startActivity(intent)
                        },
                        onClickAlarm = {
                            val uid = it
                            val intent = Intent(this, AlarmConfigurationActivity::class.java)
                            intent.putExtra("operation", "update")
                            intent.putExtra("uid", uid)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        val listener = alarmManagerListener ?: return
        alarmManagerListener = null

        CustomAlarmManager.onManagerUpdatedListeners.remove(listener)

    }

    private var serviceName: ComponentName? = null
    private var service: AlarmService? = null
    private val serviceConnection = AlarmServiceConnection()

    private fun startAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    inner class AlarmServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder !is AlarmService.AlarmServiceBinder) return

//            Toast.makeText(this@MainActivity, "连接到服务", Toast.LENGTH_SHORT).show()
            service = binder.service
            serviceName = name
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            if (serviceName == null || service == null || serviceName != name) return

//            Toast.makeText(this@MainActivity, "断开连接服务", Toast.LENGTH_SHORT).show()
            service = null
            serviceName = null
        }

    }
}

val FONT_FAMILY_SMILEY_SANS = FontFamily(
    Font(R.font.smiley_sans_oblique, FontWeight.Normal)
)

@Composable
fun AlarmList(alarms: List<AlarmModel>, onClickAdd: () -> Unit, onClickAlarm: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val time = Date()
        Row {
            Text(text = stringResource(id = R.string.current_time).formatBy(DATE_TIME_FORMAT.format(time)))
        }
        Button(onClick = onClickAdd) {
            Text(text = stringResource(id = R.string.add_alarm))
        }
        Text(
            text = if (alarms.isEmpty()) stringResource(id = R.string.no_alarm)
            else (stringResource(id = R.string.total_alarm_count).formatBy(alarms.size))
        )
        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp, 2.dp)
        ) {
            itemsIndexed(alarms) { index, alarm ->
                Row(Modifier.clickable { onClickAlarm(alarm.uid) }) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "alarm",
                        modifier = Modifier
                            .align(CenterVertically)
                            .padding(5.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(
                                text = alarm.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FONT_FAMILY_SMILEY_SANS,
                            )
                        }
                        Row {
                            Text(
                                text = stringResource(id = R.string.next_ring).formatBy(
                                    if (alarm.followingRingTime != null)
                                        DATE_TIME_FORMAT.format(alarm.followingRingTime)
                                    else "N/A"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                            )
                        }
                    }
                }
            }
        }
    }
}

val DATE_TIME_FORMAT: DateFormat = SimpleDateFormat("yyyy年MM月dd日EHH:mm:ss", Locale.CHINA)
val TIME_FORMAT: DateFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINA)

@Preview(name = "Light Mode")
@Preview(
    name = "Dark Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
)
@Composable
fun DefaultPreview() {
    val alarms by remember { mutableStateOf(with(currentTime()) {
        CustomAlarmManager.getAlarms().map { it.getModel(this) }
    }) }

    CustomAlarmTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AlarmList(
                alarms,
                onClickAdd = {
                    CustomAlarmManager.addAlarm(createTestAlarm())
                },
                onClickAlarm = {  }
            )
        }
    }
}

fun createTestAlarm(): Alarm {
    val currentTime = Date()
    val calendar = Calendar.getInstance()
    calendar.time = currentTime
    calendar.add(Calendar.SECOND, 1)
    val time = calendar.time

    return DailyAlarm(0).also {
        it.name = "起床闹钟"
        it.dailyTime = time
    }
}