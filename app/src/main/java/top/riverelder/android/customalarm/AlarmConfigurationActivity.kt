package top.riverelder.android.customalarm

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import top.riverelder.android.customalarm.ui.theme.CustomAlarmTheme

class AlarmConfigurationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val index = intent.getIntExtra("index", 0)
        val alarm = CustomAlarmManager.getAlarm(index)
        if (alarm == null) {
            Toast.makeText(this, "未找到对应闹铃！", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {

            CustomAlarmTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var name by remember { mutableStateOf(alarm.name) }
                    var properties by remember { mutableStateOf(alarm.properties) }

                    fun save() {
                        alarm.name = name
                        alarm.properties = properties
                        CustomAlarmManager.updateAlarm(alarm)
                        finish()
                    }

                    fun reset() {
                        name = alarm.name
                        properties = alarm.properties
                    }

                    fun cancel() {
                        finish()
                    }

                    Column {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(text = "闹铃名称：", fontSize = 5.em)
                                BasicTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    keyboardActions = KeyboardActions { if (name.isEmpty()) name = "Alarm" },
                                    textStyle = TextStyle(fontSize = 7.em, fontFamily = FONT_FAMILY_SMILEY_SANS),
                                    modifier = Modifier.padding(vertical = 0.dp),
                                    singleLine = true,
                                    maxLines = 1,
                                )
                            }
                            
                            alarm.type.AlarmConfigurationView(
                                context = this@AlarmConfigurationActivity,
                                bundle = properties,
                                onChange = { properties = it }
                            )
                        }

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = { save() }) {
                                Text(text = "保存")
                            }
                            Button(onClick = { reset() }) {
                                Text(text = "重置")
                            }
                            Button(onClick = { cancel() }) {
                                Text(text = "取消")
                            }
                        }
                    }
                }
            }
        }
    }
}
