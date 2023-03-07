package top.riverelder.android.customalarm.alarm

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import java.util.*

interface AlarmType {
    /**
     * 唯一标识符，唯一确定以一个实例
     */
    val id: String

    /**
     * 创建并初始化一个闹铃
     * @param initialTime 用于初始化的时间参数，一般是当前时间
     * @return 返回创建好的闹铃
     */
    fun create(initialTime: Date): Alarm


    /**
     * 渲染其配置视图
     * @param alarm 要渲染配置视图的闹铃
     */
    @Composable
    fun AlarmConfigurationView(context: Context, bundle: Bundle, onChange: (Bundle) -> Unit)
}