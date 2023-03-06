package top.riverelder.android.customalarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import top.riverelder.android.customalarm.ui.theme.CustomAlarmTheme

class AlarmConfigurationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val alarm = createAlarm()

            CustomAlarmTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    alarm.type.AlarmConfigurationView(
                        context = this,
                        bundle = alarm.properties,
                        onChange = { alarm.properties = it }
                    )
                }
            }
        }
    }
}