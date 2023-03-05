package top.riverelder.android.customalarm.alarm

data class AlarmConfigurationMetadataItem<T> (
    val name: String,
    val type: Class<T>,
    val hasDefaultValue: Boolean,
    val defaultValue: T?,
)