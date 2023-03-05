package top.riverelder.android.customalarm

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import top.riverelder.android.customalarm.alarm.Alarm
import top.riverelder.android.customalarm.alarm.impl.DailyAlarm
import top.riverelder.android.customalarm.ui.theme.CustomAlarmTheme
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CustomAlarmTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun AlarmList(alarms: List<Alarm>) {
    LazyColumn {
        itemsIndexed(alarms) { index, alarm ->
            val time = Date()
            Row {
                Text(text = "No.$index")
                Column {
                    Row {
                        Text(text = "名称：")
                        Text(text = alarm.name)
                    }
                    Row {
                        val nextRingTime = alarm.followingRingTime(time)
                        Text(text = "下次响铃：")
                        Text(text = if (nextRingTime != null) DATE_TIME_FORMAT.format(nextRingTime) else "N/A")
                    }
                    Row {
                        Text(text = "当前时间：")
                        Text(text = DATE_TIME_FORMAT.format(time))
                    }
                    Row {
                        Text(text = "是否该响铃：")
                        Text(text = if (alarm.shouldRing(time)) "是" else "否")
                    }
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
    val alarms: MutableList<Alarm> = ArrayList()
    alarms.add(createAlarm())

    CustomAlarmTheme {

        Column {
            AlarmList(alarms)
            Button(onClick = { alarms.add(createAlarm()) }) {
                Text(text = "增加闹钟")
            }
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