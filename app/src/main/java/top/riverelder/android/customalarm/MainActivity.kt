package top.riverelder.android.customalarm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import top.riverelder.android.customalarm.alarm.Alarm
import top.riverelder.android.customalarm.alarm.impl.DailyAlarm
import top.riverelder.android.customalarm.ui.theme.CustomAlarmTheme
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startAlarmService()
        setContent {
            var alarms: List<Alarm> by remember { mutableStateOf(ArrayList<Alarm>().also { it.add(createAlarm()) }) }

            CustomAlarmTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AlarmList(
                        alarms,
                        onClickAdd = {
                            alarms = ArrayList(alarms).also { it.add(createAlarm()) }
                        },
                        onClickAlarm = {
                            val intent = Intent(this, AlarmConfigurationActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    private var serviceName: ComponentName? = null
    private var service: AlarmService? = null
    private val serviceConnection = AlarmServiceConnection()

    private fun startAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    inner class AlarmServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder !is AlarmService.AlarmServiceBinder) return

            Toast.makeText(this@MainActivity, "连接到服务", Toast.LENGTH_SHORT).show()
            service = binder.service
            serviceName = name
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            if (serviceName == null || service == null || serviceName != name) return

            Toast.makeText(this@MainActivity, "断开连接服务", Toast.LENGTH_SHORT).show()
            service = null
            serviceName = null
        }

    }
}

val FONT_FAMILY_SMILEY_SANS = FontFamily(
    Font(R.font.smiley_sans_oblique, FontWeight.Normal)
)

@Composable
fun AlarmList(alarms: List<Alarm>, onClickAdd: () -> Unit, onClickAlarm: (Alarm) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val time = Date()
        Row {
            Text(text = "当前时间：")
            Text(text = DATE_TIME_FORMAT.format(time))
        }
        Button(onClick = onClickAdd) {
            Text(text = "增加闹钟")
        }
        Text(text = "共${alarms.size}个闹钟")
        LazyColumn(modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp, 2.dp)) {
            items(alarms) { alarm ->
                Row(Modifier.clickable { onClickAlarm(alarm) }) {
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
                            val nextRingTime = alarm.followingRingTime(time)
                            Text(
                                text = "下次响铃：",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                            )
                            Text(
                                text = if (nextRingTime != null) DATE_TIME_FORMAT.format(nextRingTime) else "N/A",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                            )
                        }
//                        Row {
//                            Text(text = "是否该响铃：")
//                            Text(text = if (alarm.shouldRing(time)) "是" else "否")
//                        }
                    }
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "alarm",
                        modifier = Modifier
                            .align(CenterVertically)
                            .padding(5.dp)
                            .clickable { onClickAlarm(alarm) },
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello, Dear ${name}!")
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
    var alarms by remember { mutableStateOf(ArrayList<Alarm>().also { it.add(createAlarm()) }) }

    CustomAlarmTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AlarmList(
                alarms,
                onClickAdd = {
                    alarms = ArrayList(alarms).also { it.add(createAlarm()) }
                },
                onClickAlarm = {  }
            )
        }
    }
}

fun createAlarm(): Alarm {
    val currentTime = Date()
    val calendar = Calendar.getInstance()
    calendar.time = currentTime
    calendar.add(Calendar.SECOND, 1)
    val time = calendar.time

    return DailyAlarm().also {
        it.name = "起床闹钟"
        it.dailyTime = time
    }
}