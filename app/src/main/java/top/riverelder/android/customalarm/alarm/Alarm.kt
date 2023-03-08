package top.riverelder.android.customalarm.alarm

import android.os.Bundle
import androidx.annotation.IntRange
import top.riverelder.android.customalarm.AlarmService
import java.util.Date

interface Alarm {

    /**
     * 该闹铃的类型，其包括了如何渲染等
     */
    val type: AlarmType

    /**
     * 该闹铃的名称
     */
    var name: String

    /**
     * 标记该闹铃已经被计划响铃，响铃后设为false等待下一次计划
     */
    var scheduled: Boolean

    /**
     * 通过给定时间（一般是当前时间）进行设置
     */
    fun setByTime(time: Date)

    /**
     * 在给定时间time后第1个应该响铃的时间
     * @param time 给定的时间，返回值不包括time
     * @return 下个响铃的时间，若返回null则说明在可计算范围内没有可以响铃的时间
     */
    fun followingRingTime(time: Date) = this.followingRingTime(time, 0)

    /**
     * 在给定时间time后第order个应该响铃的时间
     * @param time 给定的时间，返回值不包括time
     * @param order 要查询的之后第几个响铃时间，序数从0开始
     * @return 下个响铃的时间，若返回null则说明在可计算范围内没有可以响铃的时间
     */
    fun followingRingTime(time: Date, @IntRange(from = 0) order: Int): Date?

    /**
     * 测试一个时间是否是该响铃
     * 例如：定时在1：00，当前时间是1：01，虽然已经超时1分钟，但是根据计划，在超时5分钟内仍可以响铃，故而返回true
     * @param time 给定的时间
     * @return 是否应该响铃
     */
    fun shouldRing(time: Date): Boolean

    /**
     * 实行响铃行为，后期会加入复杂的响铃行为，为了方便，暂不使用组合，而是继承
     */
    fun ring(service: AlarmService)

    /**
     * 用于设置获取读取设置，不一定真的有这一个field，只是定义了getter和setter
     * 该getter月setter也用以持久化
     */
    var properties: Bundle

}