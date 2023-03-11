package top.riverelder.android.customalarm

import java.lang.Exception
import java.lang.Integer.parseInt

val REGEX = Regex("\\$\\{(\\w+)?\\}")

fun String.formatBy(map: Map<String, Any>): String {
    val defaultValue = if (map.isNotEmpty()) map.entries.first().value else null
    var lastIndex = 0
    val builder = StringBuilder()
    for (piece in REGEX.findAll(this, 0)) {
        val group = piece.groups[1] ?: continue
        builder.append(substring(lastIndex, piece.range.first))
        lastIndex = piece.range.last + 1
        builder.append(map.getOrDefault(group.value, defaultValue ?: piece.value))
    }
    builder.append(substring(lastIndex))
    return builder.toString()
}

fun String.formatBy(list: List<Any>): String {
    val defaultValue = if (list.isNotEmpty()) list.first() else null
    var lastIndex = 0
    val builder = StringBuilder()
    for (piece in REGEX.findAll(this, 0)) {
        val group = piece.groups[1] ?: continue
        val index = try {
            parseInt(group.value)
        } catch (ignored: Exception) {
            continue
        }
        builder.append(substring(lastIndex, piece.range.first))
        lastIndex = piece.range.last + 1
        builder.append(list.getOrNull(index) ?: (defaultValue ?: piece.value))
    }
    builder.append(substring(lastIndex))
    return builder.toString()
}

fun String.formatBy(argument: Any): String {
    var lastIndex = 0
    val builder = StringBuilder()
    for (piece in REGEX.findAll(this, 0)) {
        builder.append(substring(lastIndex, piece.range.first))
        lastIndex = piece.range.last + 1
        builder.append(argument)
    }
    builder.append(substring(lastIndex))
    return builder.toString()
}